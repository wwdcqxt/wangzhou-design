package com.wwdcqxt.wordmemory.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词实体。
 *
 * 同一条词条在同一个词库内不允许重复（headword + bookId 唯一索引），
 * 导入时重复词条会被 [androidx.room.OnConflictStrategy.IGNORE] 跳过并计入 skipped。
 */
@Entity(
    tableName = "words",
    indices = [
        Index(value = ["headword", "bookId"], unique = true),
        Index(value = ["bookId"])
    ]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 单词 / 词条，例如 abandon */
    val headword: String,
    /** 音标，例如 əˈbændən */
    val phonetic: String = "",
    /** 中文释义，例如 vt. 放弃，抛弃 */
    val meaning: String,
    /** 英文例句 */
    val example: String = "",
    /** 例句中文翻译 */
    val exampleCn: String = "",
    /** 所属词库 id */
    val bookId: Long = 0L,
    /** 是否标记为“已熟悉”，熟悉后仍可复习但默认不进入测试范围 */
    val familiar: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
