package com.wwdcqxt.wordmemory.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 复习日志：每答一次记一条，用于统计看板的正确率、每日复习量与连续打卡天数。
 *
 * quality 取值沿用 SM-2 的 0~5 分制，本 App 只用到 1/3/4/5 四档：
 * 1 忘记、3 模糊、4 记得、5 简单。
 */
@Entity(
    tableName = "review_logs",
    indices = [
        Index(value = ["wordId"]),
        Index(value = ["reviewedAt"])
    ]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val wordId: Long,
    val quality: Int,
    val prevIntervalDays: Int,
    val nextIntervalDays: Int,
    val reviewedAt: Long,
    val durationMs: Long = 0L
)
