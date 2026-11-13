package com.wwdcqxt.wordmemory.core.data.model

/**
 * 按天聚合的复习统计（SQLite strftime 分组结果）。
 *
 * @param day 形如 2026-11-13
 * @param reviews 当天复习总次数
 * @param correct 当天质量 >= 3 的次数
 */
data class DailyStat(
    val day: String,
    val reviews: Int,
    val correct: Int
)
