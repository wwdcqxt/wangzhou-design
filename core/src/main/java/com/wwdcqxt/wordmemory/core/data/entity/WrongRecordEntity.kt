package com.wwdcqxt.wordmemory.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 错题记录（自选功能“错题回顾”的数据来源）。
 *
 * 以 wordId 为主键，答错一次 wrongCount + 1；在错题本里标记“已掌握”后不再展示。
 */
@Entity(
    tableName = "wrong_records",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mastered"])]
)
data class WrongRecordEntity(
    @PrimaryKey val wordId: Long,
    val wrongCount: Int = 1,
    val lastWrongAt: Long = System.currentTimeMillis(),
    val mastered: Boolean = false
)
