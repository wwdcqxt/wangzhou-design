package com.wwdcqxt.wordmemory.core.data.repository

import androidx.room.withTransaction
import com.wwdcqxt.wordmemory.core.data.db.AppDatabase
import com.wwdcqxt.wordmemory.core.data.entity.ReviewCardEntity
import com.wwdcqxt.wordmemory.core.data.entity.ReviewLogEntity
import com.wwdcqxt.wordmemory.core.data.entity.WrongRecordEntity
import com.wwdcqxt.wordmemory.core.data.model.DailyStat
import com.wwdcqxt.wordmemory.core.data.model.DueWord
import com.wwdcqxt.wordmemory.core.domain.algorithm.EbbinghausScheduler
import com.wwdcqxt.wordmemory.core.domain.algorithm.ReviewState
import com.wwdcqxt.wordmemory.core.domain.algorithm.ScheduleResult
import kotlinx.coroutines.flow.Flow

/**
 * 复习仓库：今日队列、提交评分、错题记录。
 *
 * 关键点：一次评分会同时更新“卡片状态”“复习日志”“错题记录”三张表，
 * 必须放在同一个事务里，否则中途失败会导致统计数据与卡片状态不一致。
 */
class ReviewRepository(private val db: AppDatabase) {

    fun observeDueCount(now: Long): Flow<Int> = db.reviewDao().observeDueCount(now)

    fun observeDueWords(now: Long, limit: Int = 200): Flow<List<DueWord>> =
        db.reviewDao().observeDueWords(now, limit)

    fun observeLearnedCount(): Flow<Int> = db.reviewDao().observeLearnedCount()

    fun observeLogs(wordId: Long, limit: Int = 20): Flow<List<ReviewLogEntity>> =
        db.reviewDao().observeLogsByWord(wordId, limit)

    fun observeCard(wordId: Long): Flow<ReviewCardEntity?> = db.reviewDao().observeCardByWord(wordId)

    fun observeWrongRecord(wordId: Long): Flow<WrongRecordEntity?> = db.wrongDao().observeWrong(wordId)

    fun observeDaily(from: Long): Flow<List<DailyStat>> = db.reviewDao().observeDaily(from)

    suspend fun countDue(now: Long): Int = db.reviewDao().countDue(now)

    /**
     * 提交一次复习评分。
     *
     * @return 新的调度结果；卡片不存在返回 null
     */
    suspend fun submitReview(
        wordId: Long,
        quality: Int,
        now: Long = System.currentTimeMillis()
    ): ScheduleResult? = db.withTransaction {
        val card = db.reviewDao().findCardByWord(wordId) ?: return@withTransaction null

        val result = EbbinghausScheduler.next(
            ReviewState(
                easeFactor = card.easeFactor,
                intervalDays = card.intervalDays,
                repetitions = card.repetitions,
                lapses = card.lapses,
                state = card.state
            ),
            quality = quality,
            now = now
        )

        db.reviewDao().updateCard(
            card.copy(
                easeFactor = result.easeFactor,
                intervalDays = result.intervalDays,
                repetitions = result.repetitions,
                lapses = result.lapses,
                dueAt = result.dueAt,
                lastReviewedAt = now,
                state = result.state
            )
        )
        db.reviewDao().insertLog(
            ReviewLogEntity(
                wordId = wordId,
                quality = quality,
                prevIntervalDays = card.intervalDays,
                nextIntervalDays = result.intervalDays,
                reviewedAt = now
            )
        )
        if (quality < EbbinghausScheduler.QUALITY_HARD) {
            recordWrongInternal(wordId, now)
        }
        result
    }

    /** 测试模式（选择题）答错时调用，写入错题本 */
    suspend fun recordWrong(wordId: Long, now: Long = System.currentTimeMillis()) = db.withTransaction {
        recordWrongInternal(wordId, now)
    }

    private suspend fun recordWrongInternal(wordId: Long, now: Long) {
        val existing = db.wrongDao().find(wordId)
        db.wrongDao().upsert(
            WrongRecordEntity(
                wordId = wordId,
                wrongCount = (existing?.wrongCount ?: 0) + 1,
                lastWrongAt = now,
                mastered = false
            )
        )
    }

    fun observeWrongWords() = db.wrongDao().observeWrongWords()

    fun observeWrongCount(): Flow<Int> = db.wrongDao().observeActiveCount()

    suspend fun markWrongMastered(wordId: Long) = db.wrongDao().markMastered(wordId)

    suspend fun removeWrong(wordId: Long) = db.wrongDao().delete(wordId)
}
