package com.wwdcqxt.wordmemory.ui.wordbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.core.data.entity.WordBookEntity
import com.wwdcqxt.wordmemory.core.data.repository.WordRepository
import com.wwdcqxt.wordmemory.data.SeedWordBookImporter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 词库列表 ViewModel：观察词库表 + 总词数，处理删除与“加载内置词库”。
 */
class WordBookViewModel(
    private val wordRepository: WordRepository,
    private val seedImporter: SeedWordBookImporter
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val books: List<WordBookEntity> = emptyList(),
        val totalWords: Int = 0,
        val error: String? = null,
        val working: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching {
                wordRepository.observeBooks().collect { books ->
                    _state.update { it.copy(loading = false, error = null, books = books) }
                }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "加载词库失败") }
            }
        }
        viewModelScope.launch {
            runCatching {
                wordRepository.observeTotalCount().collect { total ->
                    _state.update { it.copy(totalWords = total) }
                }
            }
        }
    }

    fun deleteBook(book: WordBookEntity) {
        viewModelScope.launch {
            runCatching { wordRepository.deleteBook(book.id) }
                .onSuccess { _events.emit("已删除词库「${book.name}」") }
                .onFailure { _events.emit("删除失败：${it.message}") }
        }
    }

    fun loadSeedWordBook() {
        if (_state.value.working) return
        viewModelScope.launch {
            _state.update { it.copy(working = true) }
            runCatching { seedImporter.importBuiltin() }
                .onSuccess { _events.emit("已导入 ${it.words.size} 个内置单词") }
                .onFailure { _events.emit("导入失败：${it.message ?: "未知错误"}") }
            _state.update { it.copy(working = false) }
        }
    }
}
