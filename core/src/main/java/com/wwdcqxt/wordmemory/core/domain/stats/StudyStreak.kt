package com.wwdcqxt.wordmemory.core.domain.stats

import com.wwdcqxt.wordmemory.core.data.model.DailyStat
import java.util.Calendar

/**
 * 学习连续打卡天数计算（纯 Kotlin，可单元测试）。
 *
 * 规则：从“今天”往前数，连续每天都有复习记录的天数；
 * 今天还没复习时从昨天开始数，避免早上打开 App 就显示 0 天。
 */
object StudyStreak {

    const val DAY_MS = 24 * 60 * 60 * 1000L

    fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun dayKey(time: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = time }
        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun of(daily: List<DailyStat>, now: Long): Int {
        val reviewedDays = daily.filter { it.reviews > 0 }.map { it.day }.toHashSet()
        if (reviewedDays.isEmpty()) return 0

        var cursor = startOfDay(now)
        if (dayKey(cursor) !in reviewedDays) {
            cursor -= DAY_MS
        }
        var streak = 0
        while (streak < 3650 && dayKey(cursor) in reviewedDays) {
            streak++
            cursor -= DAY_MS
        }
        return streak
    }
}
