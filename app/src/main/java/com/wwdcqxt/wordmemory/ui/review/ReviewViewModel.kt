package com.wwdcqxt.wordmemory.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.core.data.model.DueWord
import com.wwdcqxt.wordmemory.core.data.repository.ReviewRepository
import com.wwdcqxt.wordmemory.core.domain.algorithm.EbbinghausScheduler
import com.wwdcqxt.wordmemory.core.domain.algorithm.ReviewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 复习会话 ViewModel。
 *
 * 进入时对“今日到期队列”拍一次快照，之后逐张评分：
 * 评分后卡片 dueAt 会变化，但会话内仍按快照顺序走完，避免列表抖动。
 */
class ReviewViewModel(
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val words: List<DueWord> = emptyList(),
        val index: Int = 0,
        val revealed: Boolean = false,
        val reviewedCount: Int = 0,
        val finished: Boolean = false,
        val remaining: Int = 0,
        val submitting: Boolean = false,
        val error: String? = null,
        /** 四档按钮的“下次复习”提示，key = quality */
        val hints: Map<Int, String> = emptyMap()
    ) {
        val current: DueWord? get() = words.getOrNull(index)
        val total: Int get() = words.size
        val progressPercent: Int
            get() = if (total == 0) 0 else (index * 100 / total)
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                reviewRepository.observeDueWords(now, limit = 100).first()
            }.onSuccess { words ->
                _state.update {
                    it.copy(
                        loading = false,
                        words = words,
                        finished = words.isEmpty(),
                        hints = previewHints(words.firstOrNull())
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "加载复习队列失败") }
            }
        }
    }

    private fun previewHints(word: DueWord?): Map<Int, String> {
        if (word == null) return emptyMap()
        val state = ReviewState(
            easeFactor = word.easeFactor,
            intervalDays = word.intervalDays,
            repetitions = word.repetitions,
            lapses = word.lapses,
            state = word.state
        )
        val now = System.currentTimeMillis()
        return listOf(
            EbbinghausScheduler.QUALITY_AGAIN,
            EbbinghausScheduler.QUALITY_HARD,
            EbbinghausScheduler.QUALITY_GOOD,
            EbbinghausScheduler.QUALITY_EASY
        ).associateWith { quality ->
            val result = EbbinghausScheduler.next(state, quality, now)
            if (result.intervalDays == 0) {
                val minutes = ((result.dueAt - now) / 60_000.0).roundToInt()
                "$minutes 分钟后"
            } else {
                "${result.intervalDays} 天后"
            }
        }
    }

    fun reveal() {
        _state.update { it.copy(revealed = true) }
    }

    fun grade(quality: Int) {
        val current = _state.value.current ?: return
        if (_state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            runCatching { reviewRepository.submitReview(current.word.id, quality) }
                .onSuccess {
                    val nextIndex = _state.value.index + 1
                    val finished = nextIndex >= _state.value.words.size
                    val remaining = if (finished) runCatching { reviewRepository.countDue(System.currentTimeMillis()) }.getOrDefault(0) else 0
                    _state.update {
                        it.copy(
                            submitting = false,
                            index = nextIndex.coerceAtMost(it.words.size),
                            reviewedCount = it.reviewedCount + 1,
                            revealed = false,
                            finished = finished,
                            remaining = remaining,
                            hints = previewHints(it.words.getOrNull(nextIndex))
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(submitting = false, error = error.message ?: "提交失败") }
                }
        }
    }
}
