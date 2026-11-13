package com.wwdcqxt.wordmemory.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时间格式化工具：统一中文展示，避免各页面各写一套。
 */
object TimeFormats {

    private val dateTimeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val shortDateFormat = SimpleDateFormat("MM-dd", Locale.CHINA)

    /** 距离到期的可读描述，用于今日队列 */
    fun relativeDue(dueAt: Long, now: Long = System.currentTimeMillis()): String {
        val diff = dueAt - now
        return when {
            diff <= 0 -> "待复习"
            diff < 60_000 -> "不到 1 分钟"
            diff < 3_600_000 -> "${diff / 60_000} 分钟后"
            diff < 86_400_000 -> "${diff / 3_600_000} 小时后"
            else -> "${diff / 86_400_000} 天后"
        }
    }

    fun dateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun date(millis: Long): String = dateFormat.format(Date(millis))

    fun shortDate(millis: Long): String = shortDateFormat.format(Date(millis))

    fun timeOfDay(hour: Int, minute: Int): String = String.format(Locale.CHINA, "%02d:%02d", hour, minute)
}
