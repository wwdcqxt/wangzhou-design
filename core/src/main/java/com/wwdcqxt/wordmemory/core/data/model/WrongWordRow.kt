package com.wwdcqxt.wordmemory.core.data.model

/**
 * 看板“错词榜”行数据。
 */
data class WrongWordRow(
    val wordId: Long,
    val headword: String,
    val meaning: String,
    val wrongCount: Int
)
