package com.wwdcqxt.wordmemory.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 词库实体。
 *
 * source 取值：builtin（内置词库）、csv、txt、json（用户导入）。
 */
@Entity(tableName = "word_books")
data class WordBookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val source: String = SOURCE_BUILTIN,
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_BUILTIN = "builtin"
        const val SOURCE_CSV = "csv"
        const val SOURCE_TXT = "txt"
        const val SOURCE_JSON = "json"
    }
}
