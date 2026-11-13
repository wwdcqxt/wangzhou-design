package com.wwdcqxt.wordmemory.ui.imports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.core.data.repository.WordRepository
import com.wwdcqxt.wordmemory.core.domain.importer.ImportResult
import com.wwdcqxt.wordmemory.core.domain.importer.WordBookFormat
import com.wwdcqxt.wordmemory.core.domain.importer.WordBookParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 词库导入 ViewModel：
 * 读取文件 → 解析（CSV/TXT/JSON）→ 预览结果与错误行 → 事务写入数据库。
 * 解析与写库都在 IO 线程，UI 只展示状态。
 */
class ImportViewModel(
    private val wordRepository: WordRepository
) : ViewModel() {

    data class UiState(
        val name: String = "",
        val fileName: String? = null,
        val format: WordBookFormat? = null,
        val parsing: Boolean = false,
        val importing: Boolean = false,
        val parsed: ImportResult? = null,
        val error: String? = null,
        val canImport: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun onFilePicked(fileName: String, content: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(parsing = true, error = null, fileName = fileName, parsed = null, canImport = false)
            }
            val format = WordBookParser.detectFormat(fileName)
            val result = withContext(Dispatchers.IO) { WordBookParser.parse(content, format) }
            _state.update {
                it.copy(
                    parsing = false,
                    format = format,
                    parsed = result,
                    canImport = result.words.isNotEmpty(),
                    name = it.name.ifBlank { fileName.substringBeforeLast('.') }
                )
            }
        }
    }

    fun confirmImport() {
        val state = _state.value
        val parsed = state.parsed ?: return
        if (parsed.words.isEmpty() || state.importing) return

        viewModelScope.launch {
            _state.update { it.copy(importing = true) }
            val bookName = state.name.ifBlank { "导入词库" }
            val source = when (state.format) {
                WordBookFormat.CSV -> "csv"
                WordBookFormat.TXT -> "txt"
                WordBookFormat.JSON -> "json"
                null -> "txt"
            }
            runCatching {
                wordRepository.importBook(
                    name = bookName,
                    description = "从 ${state.fileName ?: "文件"} 导入",
                    source = source,
                    words = parsed.words
                )
            }.onSuccess { outcome ->
                _events.emit("已导入 ${outcome.imported} 个单词${if (outcome.skipped > 0) "，跳过重复 ${outcome.skipped} 个" else ""}")
                _state.update {
                    it.copy(importing = false, parsed = null, canImport = false)
                }
            }.onFailure { error ->
                _state.update { it.copy(importing = false, error = "导入失败：${error.message}") }
            }
        }
    }
}
