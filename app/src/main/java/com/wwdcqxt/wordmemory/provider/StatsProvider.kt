package com.wwdcqxt.wordmemory.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.wwdcqxt.wordmemory.core.data.db.AppDatabase
import com.wwdcqxt.wordmemory.core.data.repository.StatsRepository
import com.wwdcqxt.wordmemory.core.provider.StatsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 主 App 对外暴露的只读统计 Provider（P2 数据看板的数据来源）。
 *
 * 只有持有 READ_STATS（signature 级）权限的 APK 才能读；
 * 查询在 IO 线程执行，异常统一转成空游标，避免把看板 App 拖崩。
 */
class StatsProvider : ContentProvider() {

    private val statsRepository: StatsRepository by lazy {
        StatsRepository(AppDatabase.get(requireNotNull(context)))
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor = runCatching {
        runBlocking(Dispatchers.IO) {
            when (uri.pathSegments.firstOrNull()) {
                PATH_OVERVIEW -> overviewCursor()
                PATH_DAILY -> dailyCursor()
                PATH_TOP_WRONG -> topWrongCursor()
                else -> throw IllegalArgumentException("未知的查询路径：$uri")
            }
        }
    }.getOrElse { error ->
        Log.w(TAG, "统计查询失败：$uri", error)
        MatrixCursor(arrayOf("error")).apply { addRow(arrayOf(error.message ?: "unknown")) }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.${StatsContract.AUTHORITY}.${uri.pathSegments.firstOrNull()}"

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("统计视图只读")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("统计视图只读")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("统计视图只读")

    // ---------------------------------------------------------------- 游标构建

    private suspend fun overviewCursor(): Cursor {
        val snapshot = statsRepository.snapshot()
        val columns = arrayOf(
            StatsContract.OverviewColumns.TOTAL_WORDS,
            StatsContract.OverviewColumns.TOTAL_CARDS,
            StatsContract.OverviewColumns.LEARNED_CARDS,
            StatsContract.OverviewColumns.MASTERED_CARDS,
            StatsContract.OverviewColumns.DUE_CARDS,
            StatsContract.OverviewColumns.TOTAL_REVIEWS,
            StatsContract.OverviewColumns.CORRECT_REVIEWS,
            StatsContract.OverviewColumns.ACCURACY,
            StatsContract.OverviewColumns.TODAY_REVIEWS,
            StatsContract.OverviewColumns.TODAY_ACCURACY,
            StatsContract.OverviewColumns.STREAK_DAYS,
            StatsContract.OverviewColumns.WRONG_COUNT,
            StatsContract.OverviewColumns.UPDATED_AT
        )
        return MatrixCursor(columns).apply {
            addRow(
                arrayOf<Any>(
                    snapshot.totalWords,
                    snapshot.totalCards,
                    snapshot.learnedCards,
                    snapshot.masteredCards,
                    snapshot.dueCards,
                    snapshot.totalReviews,
                    snapshot.correctReviews,
                    snapshot.accuracy,
                    snapshot.todayReviews,
                    snapshot.todayAccuracy,
                    snapshot.streakDays,
                    snapshot.wrongCount,
                    System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun dailyCursor(): Cursor {
        val snapshot = statsRepository.snapshot()
        val columns = arrayOf(
            StatsContract.DailyColumns.DAY,
            StatsContract.DailyColumns.REVIEWS,
            StatsContract.DailyColumns.CORRECT
        )
        return MatrixCursor(columns).apply {
            snapshot.daily.forEach { stat ->
                addRow(arrayOf<Any>(stat.day, stat.reviews, stat.correct))
            }
        }
    }

    private suspend fun topWrongCursor(): Cursor {
        val columns = arrayOf(
            StatsContract.WrongColumns.WORD_ID,
            StatsContract.WrongColumns.HEADWORD,
            StatsContract.WrongColumns.MEANING,
            StatsContract.WrongColumns.WRONG_COUNT
        )
        return MatrixCursor(columns).apply {
            statsRepository.topWrong(5).forEach { row ->
                addRow(arrayOf<Any>(row.wordId, row.headword, row.meaning, row.wrongCount))
            }
        }
    }

    companion object {
        private const val TAG = "StatsProvider"
        private const val PATH_OVERVIEW = "overview"
        private const val PATH_DAILY = "daily"
        private const val PATH_TOP_WRONG = "top_wrong"
    }
}
