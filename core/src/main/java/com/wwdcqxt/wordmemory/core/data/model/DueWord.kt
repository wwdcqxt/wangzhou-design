package com.wwdcqxt.wordmemory.core.data.model

import androidx.room.Embedded
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity

/**
 * “今日复习”列表的行数据：单词 + 对应复习卡片状态（联表查询结果）。
 */
data class DueWord(
    @Embedded val word: WordEntity,
    val cardId: Long,
    val dueAt: Long,
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val state: Int,
    val lapses: Int
) {
    /** 记忆强度百分比，用于列表上的进度条 */
    val strengthPercent: Int
        get() = com.wwdcqxt.wordmemory.core.domain.algorithm.EbbinghausScheduler
            .memoryStrength(intervalDays, easeFactor, repetitions)
            .times(100).toInt()
}
