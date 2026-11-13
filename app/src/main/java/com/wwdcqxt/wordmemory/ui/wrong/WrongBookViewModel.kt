package com.wwdcqxt.wordmemory.ui.wrong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.core.data.model.WrongWord
import com.wwdcqxt.wordmemory.core.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 错题本 ViewModel（自选功能“错题回顾”）。
 * 数据来自 wrong_records 表：测试答错、复习打“忘记/模糊”都会写入。
 */
class WrongBookViewModel(
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val items: List<WrongWord> = emptyList(),
        val totalWrongCount: Int = 0
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching {
                reviewRepository.observeWrongWords().collect { items ->
                    _state.update {
                        it.copy(
                            loading = false,
                            items = items,
                            totalWrongCount = items.sumOf { word -> word.wrongCount }
                        )
                    }
                }
            }
        }
    }

    fun markMastered(wordId: Long) {
        viewModelScope.launch {
            runCatching { reviewRepository.markWrongMastered(wordId) }
                .onSuccess { _events.emit("已标记为掌握，不再出现在错题本") }
        }
    }

    fun remove(wordId: Long) {
        viewModelScope.launch {
            runCatching { reviewRepository.removeWrong(wordId) }
                .onSuccess { _events.emit("已移出错题本") }
        }
    }
}
