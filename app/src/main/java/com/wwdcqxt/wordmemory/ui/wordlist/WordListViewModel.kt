package com.wwdcqxt.wordmemory.ui.wordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity
import com.wwdcqxt.wordmemory.core.data.repository.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * 词库内单词列表：关键词搜索带 250ms 防抖，避免每敲一个字母都查库。
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WordListViewModel(
    private val wordRepository: WordRepository,
    private val bookId: Long
) : ViewModel() {

    private val keywordFlow = MutableStateFlow("")
    val keyword: StateFlow<String> = keywordFlow.asStateFlow()

    val words: StateFlow<List<WordEntity>> = keywordFlow
        .debounce(250)
        .flatMapLatest { keyword -> wordRepository.observeWords(bookId, keyword) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onKeywordChanged(keyword: String) {
        keywordFlow.update { keyword }
    }
}
