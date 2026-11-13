package com.wwdcqxt.wordmemory.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 复习日志：每答一次记一条，用于统计看板的正确率、每日复习量与连续打卡天数。
 *
 * quality 取值沿用 SM-2 的 0~5 分制，本 App 只用到 1/3/4/5 四档：
 * 1 忘记、3 模糊、4 记得、5 简单。
 *
 * 外键指向 words：单词被删除时日志一并删除，避免统计里出现孤儿记录。
 */
@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
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
