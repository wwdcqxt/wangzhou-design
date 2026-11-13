package com.wwdcqxt.wordmemory.core.domain.importer

import org.json.JSONArray
import org.json.JSONObject

/**
 * 词库文件解析器，支持三种格式：
 *
 * - **CSV**：`word,phonetic,meaning,example`，可带表头，字段支持双引号包裹
 * - **TXT**：每行一条，用制表符 / 两个以上空格 / 首个空格分隔 `单词  释义`
 * - **JSON**：对象数组 `[{"word":"...","phonetic":"...","meaning":"..."}]`，
 *   或对象 `{"words":[...]}`，字段名兼容 headword/translation/exampleCn
 *
 * 解析器只做纯文本处理，不依赖 Android，便于单元测试；行号从 1 开始，错误提示可直接展示。
 */
object WordBookParser {

    const val MAX_WORDS = 5000
    private const val MAX_ERRORS = 20
    private val HEADER_WORDS = setOf("word", "headword", "单词", "词条")
    private val SPLIT_REGEX = Regex("\\t|\\s{2,}")

    fun detectFormat(fileName: String): WordBookFormat = when (fileName.substringAfterLast('.', "").lowercase()) {
        "csv" -> WordBookFormat.CSV
        "json" -> WordBookFormat.JSON
        else -> WordBookFormat.TXT
    }

    fun parse(content: String, fileName: String): ImportResult = parse(content, detectFormat(fileName))

    fun parse(content: String, format: WordBookFormat): ImportResult {
        val text = content.removePrefix("\uFEFF")
        return when (format) {
            WordBookFormat.CSV -> parseCsv(text)
            WordBookFormat.TXT -> parseTxt(text)
            WordBookFormat.JSON -> parseJson(text)
        }
    }

    // ---------------------------------------------------------------- CSV

    private fun parseCsv(text: String): ImportResult {
        val errors = mutableListOf<String>()
        val rows = text.lineSequence().mapIndexed { index, line -> index + 1 to line }.toList()
        if (rows.isEmpty()) return ImportResult(emptyList(), listOf("文件为空"))

        var startIndex = 0
        var columnIndex = mapOf<String, Int>()
        rows.firstOrNull()?.let { (_, firstLine) ->
            val cells = splitCsvLine(firstLine)
            if (cells.firstOrNull()?.trim()?.lowercase() in HEADER_WORDS) {
                columnIndex = cells.mapIndexed { i, name -> name.trim().lowercase() to i }.toMap()
                startIndex = 1
            }
        }

        val words = mutableListOf<ParsedWord>()
        for (i in startIndex until rows.size) {
            val (lineNo, rawLine) = rows[i]
            if (rawLine.isBlank() || rawLine.trimStart().startsWith("#")) continue
            val cells = splitCsvLine(rawLine)
            if (cells.all { it.isBlank() }) continue

            // 无表头时按列数推断：3 列及以上视为 word,phonetic,meaning[,example]；2 列视为 word,meaning
            val headword: String
            val phonetic: String
            val meaning: String
            val example: String
            val exampleCn: String
            if (columnIndex.isEmpty()) {
                headword = cells.getOrElse(0) { "" }
                val hasPhoneticColumn = cells.size >= 3
                phonetic = if (hasPhoneticColumn) cells[1] else ""
                meaning = if (hasPhoneticColumn) cells[2] else cells.getOrElse(1) { "" }
                example = if (hasPhoneticColumn) cells.getOrElse(3) { "" } else cells.getOrElse(2) { "" }
                exampleCn = if (hasPhoneticColumn) cells.getOrElse(4) { "" } else cells.getOrElse(3) { "" }
            } else {
                headword = cells.pick(columnIndex, "word", "headword", default = 0)
                phonetic = cells.pick(columnIndex, "phonetic", "pron", "音标", default = 1)
                meaning = cells.pick(columnIndex, "meaning", "translation", "释义", default = 2)
                example = cells.pick(columnIndex, "example", "sentence", "例句", default = 3)
                exampleCn = cells.pick(columnIndex, "examplecn", "example_cn", "例句翻译", default = 4)
            }

            val parsed = validate(lineNo, headword, meaning, phonetic, example, exampleCn)
            when (parsed) {
                is ParseOutcome.Ok -> words += parsed.word
                is ParseOutcome.Skip -> errors.addSafe(parsed.reason)
            }
            if (words.size >= MAX_WORDS) {
                errors.addSafe("最多导入 $MAX_WORDS 个单词，其余已忽略")
                break
            }
        }
        return finish(words, errors)
    }

    private fun List<String>.pick(
        columns: Map<String, Int>,
        vararg names: String,
        default: Int
    ): String {
        names.forEach { name ->
            val index = columns[name]
            if (index != null && index < this.size) return this[index]
        }
        return this.getOrNull(default) ?: ""
    }

    /** 支持 RFC4180 的双引号转义："" 表示一个引号字符 */
    internal fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val buffer = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    buffer.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    cells += buffer.toString().trim()
                    buffer.clear()
                }
                else -> buffer.append(c)
            }
            i++
        }
        cells += buffer.toString().trim()
        return cells
    }

    // ---------------------------------------------------------------- TXT

    private fun parseTxt(text: String): ImportResult {
        val errors = mutableListOf<String>()
        val words = mutableListOf<ParsedWord>()

        text.lineSequence().forEachIndexed { index, rawLine ->
            val lineNo = index + 1
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return@forEachIndexed

            // 制表符分隔：单词 \t 音标 \t 释义
            if (line.contains('\t')) {
                val cells = line.split('\t').map { it.trim() }.filter { it.isNotEmpty() }
                when {
                    cells.size >= 3 -> words.addValidated(errors, lineNo, cells[0], cells.drop(2).joinToString(" "), cells[1])
                    cells.size == 2 -> words.addValidated(errors, lineNo, cells[0], cells[1])
                    else -> errors.addSafe("第 $lineNo 行：缺少释义（$line）")
                }
                return@forEachIndexed
            }

            // 空格分隔：连续两个及以上空格视为字段分隔符，否则按第一个空格切开
            val parts = line.split(SPLIT_REGEX).filter { it.isNotBlank() }
            when {
                parts.size >= 2 -> words.addValidated(errors, lineNo, parts[0], parts.drop(1).joinToString(" "))
                else -> {
                    val space = line.indexOf(' ')
                    if (space <= 0) {
                        errors.addSafe("第 $lineNo 行：缺少释义（$line）")
                    } else {
                        words.addValidated(
                            errors,
                            lineNo,
                            line.substring(0, space).trim(),
                            line.substring(space + 1).trim()
                        )
                    }
                }
            }
        }
        return finish(words, errors)
    }

    private fun MutableList<ParsedWord>.addValidated(
        errors: MutableList<String>,
        lineNo: Int,
        headword: String,
        meaning: String,
        phonetic: String = ""
    ) {
        when (val parsed = validate(lineNo, headword, meaning, phonetic)) {
            is ParseOutcome.Ok -> add(parsed.word)
            is ParseOutcome.Skip -> errors.addSafe(parsed.reason)
        }
    }

    // ---------------------------------------------------------------- JSON

    private fun parseJson(text: String): ImportResult {
        val errors = mutableListOf<String>()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ImportResult(emptyList(), listOf("文件为空"))

        val array: JSONArray = try {
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONObject(trimmed).optJSONArray("words") ?: return ImportResult(
                    emptyList(),
                    listOf("JSON 结构不正确：应为数组，或包含 words 数组的对象")
                )
            }
        } catch (e: Exception) {
            return ImportResult(emptyList(), listOf("JSON 解析失败：${e.message}"))
        }

        val words = mutableListOf<ParsedWord>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)
            if (item == null) {
                errors.addSafe("第 ${i + 1} 条：不是对象")
                continue
            }
            val headword = item.firstString("word", "headword", "name")
            val meaning = item.firstString("meaning", "translation", "chinese")
            val phonetic = item.firstString("phonetic", "pron")
            val example = item.firstString("example", "sentence")
            val exampleCn = item.firstString("exampleCn", "example_cn", "exampleTranslation")

            when (val parsed = validate(i + 1, headword, meaning, phonetic, example, exampleCn)) {
                is ParseOutcome.Ok -> words += parsed.word
                is ParseOutcome.Skip -> errors.addSafe(parsed.reason)
            }
        }
        return finish(words, errors)
    }

    private fun JSONObject.firstString(vararg keys: String): String {
        keys.forEach { key ->
            val value = optString(key, "")
            if (value.isNotBlank()) return value.trim()
        }
        return ""
    }

    // ---------------------------------------------------------------- 公共

    private sealed interface ParseOutcome {
        data class Ok(val word: ParsedWord) : ParseOutcome
        data class Skip(val reason: String) : ParseOutcome
    }

    private fun validate(
        lineNo: Int,
        headword: String,
        meaning: String,
        phonetic: String = "",
        example: String = "",
        exampleCn: String = ""
    ): ParseOutcome {
        val word = headword.trim()
        val cn = meaning.trim()
        return when {
            word.isEmpty() -> ParseOutcome.Skip("第 $lineNo 行：单词为空")
            cn.isEmpty() -> ParseOutcome.Skip("第 $lineNo 行：缺少释义（$word）")
            word.length > 64 -> ParseOutcome.Skip("第 $lineNo 行：单词过长（$word）")
            cn.length > 512 -> ParseOutcome.Skip("第 $lineNo 行：释义过长（$word）")
            else -> ParseOutcome.Ok(
                ParsedWord(
                    headword = word,
                    phonetic = phonetic.trim(),
                    meaning = cn,
                    example = example.trim(),
                    exampleCn = exampleCn.trim()
                )
            )
        }
    }

    /** 去重（忽略大小写）并生成最终结果 */
    private fun finish(words: List<ParsedWord>, errors: MutableList<String>): ImportResult {
        val unique = LinkedHashMap<String, ParsedWord>()
        var duplicated = 0
        words.forEach { word ->
            val key = word.headword.lowercase()
            if (unique.containsKey(key)) duplicated++ else unique[key] = word
        }
        return ImportResult(
            words = unique.values.toList(),
            errors = errors.toList(),
            skipped = duplicated
        )
    }

    private fun MutableList<String>.addSafe(message: String) {
        if (size < MAX_ERRORS && message !in this) add(message)
    }
}
