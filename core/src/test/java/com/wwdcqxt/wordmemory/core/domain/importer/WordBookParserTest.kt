package com.wwdcqxt.wordmemory.core.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 词库解析器单元测试：CSV / TXT / JSON 三种格式 + 去重 + 错误行提示。
 */
class WordBookParserTest {

    @Test
    fun `CSV 带表头时按列名映射`() {
        val csv = """
            word,phonetic,meaning,example
            abandon,əˈbændən,vt. 放弃,He abandoned the plan.
            ability,əˈbɪləti,n. 能力,
        """.trimIndent()

        val result = WordBookParser.parse(csv, WordBookFormat.CSV)

        assertEquals(2, result.words.size)
        assertEquals("abandon", result.words[0].headword)
        assertEquals("vt. 放弃", result.words[0].meaning)
        assertEquals("He abandoned the plan.", result.words[0].example)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `CSV 字段里的逗号被引号包裹时不算分隔符`() {
        val csv = "abandon,\"əˈbændən\",\"give up, leave\"\n"

        val result = WordBookParser.parse(csv, WordBookFormat.CSV)

        assertEquals(1, result.words.size)
        assertEquals("give up, leave", result.words[0].meaning)
    }

    @Test
    fun `CSV 无表头且只有两列时按 单词_释义 解析`() {
        val csv = "abandon,放弃\nability,能力\n"

        val result = WordBookParser.parse(csv, WordBookFormat.CSV)

        assertEquals(2, result.words.size)
        assertEquals("放弃", result.words[0].meaning)
    }

    @Test
    fun `TXT 支持制表符与连续空格分隔`() {
        val txt = """
            abandon	əˈbændən	vt. 放弃
            ability  n. 能力
        """.trimIndent()

        val result = WordBookParser.parse(txt, WordBookFormat.TXT)

        assertEquals(2, result.words.size)
        assertEquals("abandon", result.words[0].headword)
        assertEquals("vt. 放弃", result.words[0].meaning)
        assertEquals("əˈbændən", result.words[0].phonetic)
        assertEquals("ability", result.words[1].headword)
        assertEquals("n. 能力", result.words[1].meaning)
    }

    @Test
    fun `JSON 对象数组解析`() {
        val json = """
            [
              {"word":"abandon","phonetic":"əˈbændən","meaning":"vt. 放弃","example":"He abandoned it."},
              {"word":"ability","meaning":"n. 能力"}
            ]
        """.trimIndent()

        val result = WordBookParser.parse(json, WordBookFormat.JSON)

        assertEquals(2, result.words.size)
        assertEquals("ability", result.words[1].headword)
        assertEquals("n. 能力", result.words[1].meaning)
    }

    @Test
    fun `重复单词会被去重并计入 skipped`() {
        val txt = "abandon 放弃\nAbandon 放弃\nability 能力"

        val result = WordBookParser.parse(txt, WordBookFormat.TXT)

        assertEquals(2, result.words.size)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `缺少释义的行会记录错误但不影响其他行`() {
        val txt = "abandon\nability 能力\n\n# 注释行"

        val result = WordBookParser.parse(txt, WordBookFormat.TXT)

        assertEquals(1, result.words.size)
        assertTrue(result.errors.any { it.contains("abandon") })
    }

    @Test
    fun `根据扩展名识别格式`() {
        assertEquals(WordBookFormat.CSV, WordBookParser.detectFormat("words.csv"))
        assertEquals(WordBookFormat.JSON, WordBookParser.detectFormat("words.JSON"))
        assertEquals(WordBookFormat.TXT, WordBookParser.detectFormat("words.txt"))
        assertEquals(WordBookFormat.TXT, WordBookParser.detectFormat("noext"))
    }
}
