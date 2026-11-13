package com.wwdcqxt.wordmemory.core.provider

import android.net.Uri

/**
 * 主 App 与 P2 看板模块之间的数据契约。
 *
 * 两个 APK 的应用沙箱互相隔离，不能直接读同一个数据库文件，
 * 因此主 App 用 ContentProvider 暴露只读统计视图，看板用 ContentResolver 查询。
 * 读权限声明为 signature 级：只有同签名（同一台机器上的 debug 签名）的 APK 才能读。
 */
object StatsContract {

    const val AUTHORITY = "com.wwdcqxt.wordmemory.provider"
    const val PERMISSION_READ = "com.wwdcqxt.wordmemory.permission.READ_STATS"

    private const val BASE = "content://$AUTHORITY"

    /** 总览：单行，列见 [OverviewColumns] */
    val OVERVIEW: Uri = Uri.parse("$BASE/overview")

    /** 每日统计：多行，列见 [DailyColumns] */
    val DAILY: Uri = Uri.parse("$BASE/daily")

    /** 错词 Top N，按错误次数降序 */
    val TOP_WRONG: Uri = Uri.parse("$BASE/top_wrong")

    object OverviewColumns {
        const val TOTAL_WORDS = "total_words"
        const val TOTAL_CARDS = "total_cards"
        const val LEARNED_CARDS = "learned_cards"
        const val MASTERED_CARDS = "mastered_cards"
        const val DUE_CARDS = "due_cards"
        const val TOTAL_REVIEWS = "total_reviews"
        const val CORRECT_REVIEWS = "correct_reviews"
        const val ACCURACY = "accuracy"
        const val TODAY_REVIEWS = "today_reviews"
        const val TODAY_ACCURACY = "today_accuracy"
        const val STREAK_DAYS = "streak_days"
        const val WRONG_COUNT = "wrong_count"
        const val UPDATED_AT = "updated_at"
    }

    object DailyColumns {
        const val DAY = "day"
        const val REVIEWS = "reviews"
        const val CORRECT = "correct"
    }

    object WrongColumns {
        const val WORD_ID = "word_id"
        const val HEADWORD = "headword"
        const val MEANING = "meaning"
        const val WRONG_COUNT = "wrong_count"
    }
}
