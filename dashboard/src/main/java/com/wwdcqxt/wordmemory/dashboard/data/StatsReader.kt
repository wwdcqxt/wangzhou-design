package com.wwdcqxt.wordmemory.dashboard.data

import android.content.Context
import android.database.Cursor
import com.wwdcqxt.wordmemory.core.provider.StatsContract

/**
 * 通过 ContentResolver 读取主 App 暴露的统计视图。
 *
 * 这是伴生模块与主 App 的唯一数据通道：
 * 主 App 写数据库 → Provider 查询 → 看板展示，全程不共享数据库文件。
 */
class StatsReader(private val context: Context) {

    fun load(): Result<DashboardData> = runCatching {
        val resolver = context.contentResolver
        val overviewCursor = resolver.query(StatsContract.OVERVIEW, null, null, null, null)
            ?: error("无法访问主 App 数据（Provider 未响应）")
        val overview = overviewCursor.use { readOverview(it) }

        val dailyCursor = resolver.query(StatsContract.DAILY, null, null, null, null)
        val daily = dailyCursor?.use { readDaily(it) } ?: emptyList()

        val wrongCursor = resolver.query(StatsContract.TOP_WRONG, null, null, null, null)
        val wrong = wrongCursor?.use { readWrong(it) } ?: emptyList()

        DashboardData(overview = overview, daily = daily, wrong = wrong)
    }

    private fun readOverview(cursor: Cursor): DashboardOverview {
        if (!cursor.moveToFirst()) return DashboardOverview()
        fun int(column: String) = cursor.getInt(cursor.getColumnIndexOrThrow(column))
        fun float(column: String) = cursor.getFloat(cursor.getColumnIndexOrThrow(column))
        return DashboardOverview(
            totalWords = int(StatsContract.OverviewColumns.TOTAL_WORDS),
            totalCards = int(StatsContract.OverviewColumns.TOTAL_CARDS),
            learnedCards = int(StatsContract.OverviewColumns.LEARNED_CARDS),
            masteredCards = int(StatsContract.OverviewColumns.MASTERED_CARDS),
            dueCards = int(StatsContract.OverviewColumns.DUE_CARDS),
            totalReviews = int(StatsContract.OverviewColumns.TOTAL_REVIEWS),
            correctReviews = int(StatsContract.OverviewColumns.CORRECT_REVIEWS),
            accuracy = float(StatsContract.OverviewColumns.ACCURACY),
            todayReviews = int(StatsContract.OverviewColumns.TODAY_REVIEWS),
            todayAccuracy = float(StatsContract.OverviewColumns.TODAY_ACCURACY),
            streakDays = int(StatsContract.OverviewColumns.STREAK_DAYS),
            wrongCount = int(StatsContract.OverviewColumns.WRONG_COUNT)
        )
    }

    private fun readDaily(cursor: Cursor): List<DailyPoint> {
        val dayIndex = cursor.getColumnIndexOrThrow(StatsContract.DailyColumns.DAY)
        val reviewsIndex = cursor.getColumnIndexOrThrow(StatsContract.DailyColumns.REVIEWS)
        val correctIndex = cursor.getColumnIndexOrThrow(StatsContract.DailyColumns.CORRECT)
        val points = mutableListOf<DailyPoint>()
        while (cursor.moveToNext()) {
            points += DailyPoint(
                day = cursor.getString(dayIndex),
                reviews = cursor.getInt(reviewsIndex),
                correct = cursor.getInt(correctIndex)
            )
        }
        return points
    }

    private fun readWrong(cursor: Cursor): List<WrongItem> {
        val idIndex = cursor.getColumnIndexOrThrow(StatsContract.WrongColumns.WORD_ID)
        val headwordIndex = cursor.getColumnIndexOrThrow(StatsContract.WrongColumns.HEADWORD)
        val meaningIndex = cursor.getColumnIndexOrThrow(StatsContract.WrongColumns.MEANING)
        val countIndex = cursor.getColumnIndexOrThrow(StatsContract.WrongColumns.WRONG_COUNT)
        val items = mutableListOf<WrongItem>()
        while (cursor.moveToNext()) {
            items += WrongItem(
                wordId = cursor.getLong(idIndex),
                headword = cursor.getString(headwordIndex),
                meaning = cursor.getString(meaningIndex),
                wrongCount = cursor.getInt(countIndex)
            )
        }
        return items
    }
}
