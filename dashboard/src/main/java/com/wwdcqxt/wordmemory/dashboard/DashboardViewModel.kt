package com.wwdcqxt.wordmemory.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wwdcqxt.wordmemory.dashboard.data.DashboardData
import com.wwdcqxt.wordmemory.dashboard.data.StatsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 看板 ViewModel：通过 StatsReader 读 ContentProvider，失败时把异常交给 UI 分类提示。
 */
class DashboardViewModel(
    private val reader: StatsReader
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val data: DashboardData? = null,
        val error: Throwable? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = withContext(Dispatchers.IO) { reader.load() }
            result.onSuccess { data ->
                _state.update { it.copy(loading = false, data = data) }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error) }
            }
        }
    }
}
