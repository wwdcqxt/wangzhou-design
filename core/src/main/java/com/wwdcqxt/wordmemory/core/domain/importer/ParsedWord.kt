package com.wwdcqxt.wordmemory.core.domain.importer

/**
 * 解析出来的单词（尚未落库，bookId / id 由 Repository 分配）。
 */
data class ParsedWord(
    val headword: String,
    val phonetic: String = "",
    val meaning: String,
    val example: String = "",
    val exampleCn: String = ""
)

/**
 * 词库解析结果。errors 里保留“第几行、什么原因”，导入页直接展示给用户。
 */
data class ImportResult(
    val words: List<ParsedWord>,
    val errors: List<String> = emptyList(),
    val skipped: Int = 0
) {
    val isEmpty: Boolean get() = words.isEmpty()
}

enum class WordBookFormat { CSV, TXT, JSON }
