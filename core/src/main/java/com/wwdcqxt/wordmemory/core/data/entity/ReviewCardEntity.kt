package com.wwdcqxt.wordmemory.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 复习卡片：艾宾浩斯调度算法维护的状态。
 *
 * - easeFactor：难度因子 EF，初始 2.5，范围 [1.3, 3.0]
 * - intervalDays：当前复习间隔（天），0 表示仍在学习阶段
 * - repetitions：连续答对次数，答错清零
 * - lapses：累计遗忘次数
 * - dueAt：下次到期时间（epoch millis），今日复习列表 = dueAt <= now
 * - state：0 新词 / 1 学习中 / 2 复习中 / 3 重学中
 */
@Entity(
    tableName = "review_cards",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["wordId"], unique = true),
        Index(value = ["dueAt"])
    ]
)
data class ReviewCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val wordId: Long,
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val dueAt: Long,
    val lastReviewedAt: Long? = null,
    val state: Int = 0
) {
    companion object {
        const val STATE_NEW = 0
        const val STATE_LEARNING = 1
        const val STATE_REVIEW = 2
        const val STATE_RELEARNING = 3
    }
}
