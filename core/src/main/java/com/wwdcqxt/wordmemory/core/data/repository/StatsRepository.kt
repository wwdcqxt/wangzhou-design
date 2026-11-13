package com.wwdcqxt.wordmemory.core.data.repository

import com.wwdcqxt.wordmemory.core.data.db.AppDatabase
import com.wwdcqxt.wordmemory.core.data.model.DailyStat
import com.wwdcqxt.wordmemory.core.data.model.WrongWordRow
import com.wwdcqxt.wordmemory.core.domain.stats.StudyStreak

/**
 * 统计快照：P2 数据看板通过 ContentProvider 读到的就是这份数据。
 */
data class StatsSnapshot(
    val totalWords: Int,
    val totalCards: Int,
    val learnedCards: Int,
    val masteredCards: Int,
    val dueCards: Int,
    val totalReviews: Int,
    val correctReviews: Int,
    val accuracy: Float,
    val todayReviews: Int,
    val todayAccuracy: Float,
    val streakDays: Int,
    val wrongCount: Int,
    val daily: List<DailyStat>
)

/**
 * 统计仓库：把多张表的聚合查询拼成一个快照，供主 App 统计与看板模块共用。
 */
class StatsRepository(private val db: AppDatabase) {

    suspend fun snapshot(now: Long = System.currentTimeMillis(), days: Int = 14): StatsSnapshot {
        val statsDao = db.statsDao()
        val from = startOfDay(now) - (days - 1) * DAY_MS

        val totalReviews = statsDao.totalReviews()
        val correctReviews = statsDao.correctReviews()
        val daily = statsDao.dailyStats(from)
        val todayStart = startOfDay(now)
        val todayEnd = todayStart + DAY_MS - 1

        val todayReviews = statsDao.reviewsBetween(todayStart, todayEnd)
        val dailyCorrectToday = daily.firstOrNull { it.day == dayKey(now) }?.correct ?: 0

        return StatsSnapshot(
            totalWords = statsDao.totalWords(),
            totalCards = statsDao.totalCards(),
            learnedCards = statsDao.learnedCards(),
            masteredCards = statsDao.masteredCards(),
            dueCards = statsDao.dueCards(now),
            totalReviews = totalReviews,
            correctReviews = correctReviews,
            accuracy = if (totalReviews == 0) 0f else correctReviews.toFloat() / totalReviews,
            todayReviews = todayReviews,
            todayAccuracy = if (todayReviews == 0) 0f else dailyCorrectToday.toFloat() / todayReviews,
            streakDays = streakOf(daily, now),
            wrongCount = statsDao.activeWrongCount(),
            daily = daily
        )
    }

    /** 错词榜（看板用） */
    suspend fun topWrong(limit: Int = 5): List<WrongWordRow> = db.statsDao().topWrong(limit)

    /** 连续打卡天数，实现见 [StudyStreak] */
    fun streakOf(daily: List<DailyStat>, now: Long): Int = StudyStreak.of(daily, now)

    companion object {
        const val DAY_MS = StudyStreak.DAY_MS

        fun startOfDay(time: Long): Long = StudyStreak.startOfDay(time)

        fun dayKey(time: Long): String = StudyStreak.dayKey(time)
    }
}
