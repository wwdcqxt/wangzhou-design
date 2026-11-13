package com.wwdcqxt.wordmemory.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.core.data.model.DueWord
import com.wwdcqxt.wordmemory.core.data.repository.ReviewRepository
import com.wwdcqxt.wordmemory.core.data.repository.SettingsRepository
import com.wwdcqxt.wordmemory.core.data.repository.StatsRepository
import com.wwdcqxt.wordmemory.core.data.repository.WordRepository
import com.wwdcqxt.wordmemory.core.domain.stats.StudyStreak
import com.wwdcqxt.wordmemory.data.SeedWordBookImporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页（今日复习）ViewModel。
 *
 * 数据来源全部是 Room 的 Flow，数据库一变更 UI 自动刷新；
 * 到期时间依赖"当前时刻"，所以刷新时会重新按新的 now 建立查询。
 */
class HomeViewModel(
    private val reviewRepository: ReviewRepository,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val seedImporter: SeedWordBookImporter,
    private val statsRepository: StatsRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val error: String? = null,
        val dueWords: List<DueWord> = emptyList(),
        val dueCount: Int = 0,
        val todayReviews: Int = 0,
        val dailyGoal: Int = 20,
        val streakDays: Int = 0,
        val learnedCount: Int = 0,
        val totalWords: Int = 0,
        val reminderEnabled: Boolean = false,
        val importing: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    private var dueJob: Job? = null
    private var statsJob: Job? = null

    init {
        observeSettings()
        observeWordCount()
        observeStats()
        refresh()
    }

    /** 重新按当前时间拉取今日队列（下拉刷新 / 复习结束返回时调用） */
    fun refresh() {
        dueJob?.cancel()
        dueJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                reviewRepository.observeDueWords(now).collect { words ->
                    _state.update {
                        it.copy(loading = false, error = null, dueWords = words, dueCount = words.size)
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, error = error.message ?: "加载今日队列失败")
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(dailyGoal = settings.dailyGoal, reminderEnabled = settings.reminderEnabled) }
            }
        }
    }

    private fun observeWordCount() {
        viewModelScope.launch {
            wordRepository.observeTotalCount().collect { total ->
                _state.update { it.copy(totalWords = total) }
            }
        }
    }

    private fun observeStats() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val from = StatsRepository.startOfDay(now) - 30L * StatsRepository.DAY_MS
                reviewRepository.observeDaily(from).collect { daily ->
                    val todayKey = StatsRepository.dayKey(now)
                    val todayReviews = daily.firstOrNull { it.day == todayKey }?.reviews ?: 0
                    _state.update {
                        it.copy(
                            todayReviews = todayReviews,
                            streakDays = StudyStreak.of(daily, now)
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            runCatching {
                reviewRepository.observeLearnedCount().collect { learned ->
                    _state.update { it.copy(learnedCount = learned) }
                }
            }
        }
    }

    /** 首页空态时加载内置词库 */
    fun importSeedWordBook() {
        if (_state.value.importing) return
        viewModelScope.launch {
            _state.update { it.copy(importing = true) }
            runCatching { seedImporter.importBuiltin() }
                .onSuccess { result ->
                    _events.emit("已导入 ${result.words.size} 个内置单词")
                    refresh()
                }
                .onFailure { error ->
                    _events.emit("导入失败：${error.message ?: "未知错误"}")
                }
            _state.update { it.copy(importing = false) }
        }
    }
}
