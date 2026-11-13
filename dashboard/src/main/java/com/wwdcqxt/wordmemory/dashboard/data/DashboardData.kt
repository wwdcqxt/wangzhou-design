package com.wwdcqxt.wordmemory.dashboard.data

/** 看板展示用的数据模型（与主 App 的 Provider 契约一一对应） */
data class DashboardOverview(
    val totalWords: Int = 0,
    val totalCards: Int = 0,
    val learnedCards: Int = 0,
    val masteredCards: Int = 0,
    val dueCards: Int = 0,
    val totalReviews: Int = 0,
    val correctReviews: Int = 0,
    val accuracy: Float = 0f,
    val todayReviews: Int = 0,
    val todayAccuracy: Float = 0f,
    val streakDays: Int = 0,
    val wrongCount: Int = 0
)

data class DailyPoint(
    val day: String,
    val reviews: Int,
    val correct: Int
) {
    val accuracy: Float get() = if (reviews == 0) 0f else correct.toFloat() / reviews
}

data class WrongItem(
    val wordId: Long,
    val headword: String,
    val meaning: String,
    val wrongCount: Int
)

data class DashboardData(
    val overview: DashboardOverview,
    val daily: List<DailyPoint>,
    val wrong: List<WrongItem>
) {
    val hasData: Boolean get() = overview.totalWords > 0 || overview.totalReviews > 0
}
