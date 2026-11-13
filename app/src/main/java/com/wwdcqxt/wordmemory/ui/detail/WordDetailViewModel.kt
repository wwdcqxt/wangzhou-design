package com.wwdcqxt.wordmemory.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.core.data.entity.ReviewCardEntity
import com.wwdcqxt.wordmemory.core.data.entity.ReviewLogEntity
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity
import com.wwdcqxt.wordmemory.core.data.repository.ReviewRepository
import com.wwdcqxt.wordmemory.core.data.repository.WordRepository
import com.wwdcqxt.wordmemory.data.DictionaryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 单词详情 ViewModel：
 * - 观察单词、复习卡片、复习记录、错题状态；
 * - 提供“在线补充释义”（免费词典 API）与熟悉标记、错题标记等操作。
 */
class WordDetailViewModel(
    private val wordId: Long,
    private val wordRepository: WordRepository,
    private val reviewRepository: ReviewRepository,
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val word: WordEntity? = null,
        val card: ReviewCardEntity? = null,
        val logs: List<ReviewLogEntity> = emptyList(),
        val isWrong: Boolean = false,
        val onlineLoading: Boolean = false,
        val onlineText: String? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    private var onlineJob: Job? = null

    init {
        collection(wordRepository.observeWord(wordId)) { word ->
            _state.update { it.copy(loading = false, word = word) }
        }
        collection(reviewRepository.observeCard(wordId)) { card ->
            _state.update { it.copy(card = card) }
        }
        collection(reviewRepository.observeLogs(wordId)) { logs ->
            _state.update { it.copy(logs = logs) }
        }
        collection(reviewRepository.observeWrongRecord(wordId)) { record ->
            _state.update { it.copy(isWrong = record != null && !record.mastered) }
        }
    }

    private fun <T> collection(flow: kotlinx.coroutines.flow.Flow<T>, onValue: (T) -> Unit) {
        viewModelScope.launch {
            runCatching { flow.collect(onValue) }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "加载失败") } }
        }
    }

    /** 请求免费词典 API，把英文释义拼成一段可读文本 */
    fun lookupOnline() {
        val word = _state.value.word?.headword ?: return
        if (_state.value.onlineLoading) return
        onlineJob?.cancel()
        onlineJob = viewModelScope.launch {
            _state.update { it.copy(onlineLoading = true) }
            when (val result = dictionaryRepository.lookup(word)) {
                is DictionaryRepository.DictionaryResult.Success -> {
                    val text = buildString {
                        append("音标：").append(result.phonetic.ifBlank { "—" }).append('\n')
                        result.meanings.forEachIndexed { index, meaning ->
                            append("\n${index + 1}. [${meaning.partOfSpeech}] ${meaning.definition}")
                            if (meaning.example.isNotBlank()) append("\n   例：${meaning.example}")
                        }
                    }
                    _state.update { it.copy(onlineLoading = false, onlineText = text) }
                }

                DictionaryRepository.DictionaryResult.NotFound -> {
                    _state.update { it.copy(onlineLoading = false) }
                    _events.emit("词典中没有找到「$word」的释义")
                }

                is DictionaryRepository.DictionaryResult.Failure -> {
                    _state.update { it.copy(onlineLoading = false) }
                    _events.emit(result.message)
                }
            }
        }
    }

    fun toggleFamiliar() {
        val word = _state.value.word ?: return
        viewModelScope.launch {
            val next = !word.familiar
            runCatching { wordRepository.setFamiliar(word.id, next) }
                .onSuccess { _events.emit(if (next) "已标记为熟悉" else "已取消熟悉标记") }
        }
    }

    fun toggleWrong() {
        val word = _state.value.word ?: return
        viewModelScope.launch {
            if (_state.value.isWrong) {
                reviewRepository.removeWrong(word.id)
                _events.emit("已移出错题本")
            } else {
                reviewRepository.recordWrong(word.id)
                _events.emit("已加入错题本")
            }
        }
    }

    fun markMastered() {
        val word = _state.value.word ?: return
        viewModelScope.launch {
            reviewRepository.markWrongMastered(word.id)
            _events.emit("已标记为掌握")
        }
    }
}
