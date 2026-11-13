package com.wwdcqxt.wordmemory.data

import android.content.Context
import com.wwdcqxt.wordmemory.core.data.repository.WordRepository
import com.wwdcqxt.wordmemory.core.domain.importer.ImportResult
import com.wwdcqxt.wordmemory.core.domain.importer.WordBookFormat
import com.wwdcqxt.wordmemory.core.domain.importer.WordBookParser
import kotlinx.coroutines.flow.first

/**
 * 内置词库导入器：assets/wordbooks/cet4_core.json 在首次启动时导入，
 * 保证新装应用马上就有词可学，不用先准备文件。
 */
class SeedWordBookImporter(
    private val context: Context,
    private val wordRepository: WordRepository
) {

    /** 数据库为空时才导入，避免每次启动重复建词库 */
    suspend fun importIfEmpty(): Boolean {
        if (wordRepository.observeTotalCount().first() > 0) return false
        importBuiltin()
        return true
    }

    suspend fun importBuiltin(): ImportResult {
        val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val parsed = WordBookParser.parse(json, WordBookFormat.JSON)
        wordRepository.importBook(
            name = "CET-4 核心词",
            description = "内置词库，共 ${parsed.words.size} 词，带音标与例句",
            source = "builtin",
            words = parsed.words
        )
        return parsed
    }

    companion object {
        const val ASSET_PATH = "wordbooks/cet4_core.json"
    }
}
