package com.wwdcqxt.wordmemory.core.data.model

import androidx.room.Embedded
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity

/**
 * 错题本列表行数据：单词 + 错误次数 / 最近答错时间。
 */
data class WrongWord(
    @Embedded val word: WordEntity,
    val wrongCount: Int,
    val lastWrongAt: Long
)
