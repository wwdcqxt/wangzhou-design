package com.wwdcqxt.wordmemory.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity
import com.wwdcqxt.wordmemory.core.data.repository.ReviewRepository
import com.wwdcqxt.wordmemory.core.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 测试模式 ViewModel：从词库随机抽词生成四选一中文释义题。
 *
 * - 普通模式：全库随机 10 题；
 * - 错题模式：只抽错题本里的单词（不足 4 个会提示无法开始）。
 * 答错会写入错题本，这是“错题回顾”功能的数据入口。
 */
class QuizViewModel(
    private val wrongOnly: Boolean,
    private val wordRepository: WordRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    data class Question(
        val word: WordEntity,
        val options: List<String>,
        val correctIndex: Int
    )

    data class UiState(
        val loading: Boolean = true,
        val questions: List<Question> = emptyList(),
        val index: Int = 0,
        val selected: Int? = null,
        val correctCount: Int = 0,
        val finished: Boolean = false,
        val error: String? = null
    ) {
        val current: Question? get() = questions.getOrNull(index)
        val isAnswered: Boolean get() = selected != null
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { buildQuestions() }
                .onSuccess { questions ->
                    _state.update {
                        it.copy(
                            loading = false,
                            questions = questions,
                            index = 0,
                            selected = null,
                            correctCount = 0,
                            finished = false,
                            error = if (questions.isEmpty()) "至少需要 4 个单词才能开始测试" else null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.message ?: "生成题目失败") }
                }
        }
    }

    private suspend fun buildQuestions(): List<Question> {
        val pool = if (wrongOnly) {
            reviewRepository.observeWrongWords().first().map { it.word }
        } else {
            wordRepository.randomWords(MAX_POOL)
        }
        val distinct = pool.distinctBy { it.id }
        if (distinct.size < MIN_WORDS) return emptyList()

        val meanings = distinct.map { it.meaning }.filter { it.isNotBlank() }.distinct()
        if (meanings.size < MIN_WORDS) return emptyList()

        return distinct.shuffled().take(QUESTIONS_PER_ROUND).map { word ->
            val distractors = meanings.filter { it != word.meaning }.shuffled().take(3)
            val options = (distractors + word.meaning).shuffled()
            Question(word = word, options = options, correctIndex = options.indexOf(word.meaning))
        }
    }

    fun select(optionIndex: Int) {
        val current = _state.value.current ?: return
        if (_state.value.isAnswered) return

        val isCorrect = optionIndex == current.correctIndex
        _state.update {
            it.copy(
                selected = optionIndex,
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount
            )
        }
        if (!isCorrect) {
            viewModelScope.launch { reviewRepository.recordWrong(current.word.id) }
        }
    }

    fun next() {
        val state = _state.value
        if (!state.isAnswered) return
        val nextIndex = state.index + 1
        if (nextIndex >= state.questions.size) {
            _state.update { it.copy(finished = true) }
        } else {
            _state.update { it.copy(index = nextIndex, selected = null) }
        }
    }

    fun restart() = load()

    private companion object {
        const val MIN_WORDS = 4
        const val MAX_POOL = 60
        const val QUESTIONS_PER_ROUND = 10
    }
}
