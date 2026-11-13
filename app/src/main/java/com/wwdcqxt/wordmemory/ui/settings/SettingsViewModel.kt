package com.wwdcqxt.wordmemory.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.core.data.repository.SettingsRepository
import com.wwdcqxt.wordmemory.core.data.repository.StatsRepository
import com.wwdcqxt.wordmemory.core.data.repository.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * “我的”页 ViewModel：设置项读写（DataStore）+ 学习概览文字。
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val settings: UserSettings = UserSettings(),
        val overview: String = ""
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                settingsRepository.settings.collect { settings ->
                    _state.update { it.copy(loading = false, settings = settings) }
                }
            }
        }
        loadOverview()
    }

    fun loadOverview() {
        viewModelScope.launch {
            runCatching { statsRepository.snapshot() }.onSuccess { snapshot ->
                val accuracy = (snapshot.accuracy * 100).toInt()
                val text = "单词 ${snapshot.totalWords} · 已掌握 ${snapshot.learnedCards} · " +
                    "复习 ${snapshot.totalReviews} 次 · 正确率 $accuracy% · 待复习 ${snapshot.dueCards}"
                _state.update { it.copy(overview = text) }
            }
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(goal) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReminderEnabled(enabled) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.setReminderTime(hour, minute) }
    }
}
