package com.wwdcqxt.wordmemory.dashboard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wwdcqxt.wordmemory.core.domain.stats.StudyStreak
import com.wwdcqxt.wordmemory.dashboard.data.DashboardData
import com.wwdcqxt.wordmemory.dashboard.data.StatsReader
import com.wwdcqxt.wordmemory.dashboard.databinding.ActivityDashboardBinding
import com.wwdcqxt.wordmemory.dashboard.databinding.ItemWrongRowBinding
import com.wwdcqxt.wordmemory.dashboard.export.CsvExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 看板主界面：总览卡片 + 自绘柱状图 + 自绘环形图 + 错词榜 + CSV 导出。
 *
 * 这是独立的 APK，可单独从桌面启动；数据来自主 App 的 ContentProvider。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: DashboardViewModel

    private val updatedFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

    private companion object {
        const val DAYS_IN_CHART = 14
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = statusBar.top)
            insets
        }

        viewModel = ViewModelProvider(
            this,
            viewModelFactory {
                initializer { DashboardViewModel(StatsReader(applicationContext)) }
            }
        )[DashboardViewModel::class.java]

        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
        binding.btnRetry.setOnClickListener { viewModel.refresh() }
        binding.btnExport.setOnClickListener { exportCsv() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: DashboardViewModel.UiState) {
        binding.progressBar.isVisible = state.loading
        binding.groupError.isVisible = state.error != null && !state.loading

        state.error?.let { error ->
            binding.tvError.text = when (error) {
                is SecurityException -> getString(R.string.error_permission)
                else -> getString(R.string.error_generic, error.message ?: "未知错误")
            }
        }

        val data = state.data ?: return
        binding.groupContent.isVisible = !state.loading && data.hasData
        binding.groupEmpty.isVisible = !state.loading && !data.hasData
        binding.btnExport.isEnabled = !state.loading && data.daily.isNotEmpty()

        bindOverview(data)
        bindCharts(data)
        bindWrongList(data)

        binding.tvUpdated.text = getString(R.string.updated_at, updatedFormat.format(Date()))
    }

    private fun bindOverview(data: DashboardData) {
        val overview = data.overview
        binding.tvTotalWords.text = overview.totalWords.toString()
        binding.tvLearned.text = overview.learnedCards.toString()
        binding.tvDue.text = overview.dueCards.toString()
        binding.tvReviews.text = overview.totalReviews.toString()
        binding.tvAccuracy.text = "${(overview.accuracy * 100).toInt()}%"
        binding.tvStreak.text = "${overview.streakDays} 天"
    }

    private fun bindCharts(data: DashboardData) {
        binding.barChart.setData(fillLastDays(data, DAYS_IN_CHART))
        binding.donutChart.setPercent(data.overview.accuracy)
        binding.tvCorrectDetail.text =
            "答对 ${data.overview.correctReviews} / 共 ${data.overview.totalReviews} 次"
        binding.tvTodayDetail.text =
            "今日复习 ${data.overview.todayReviews} 次 · 今日正确率 ${(data.overview.todayAccuracy * 100).toInt()}%"
    }

    /**
     * 把稀疏的按天数据补成连续 14 天：没有复习的日期补 0，
     * 否则只有一天数据时柱状图会退化成一根占满全屏的柱子。
     */
    private fun fillLastDays(data: DashboardData, days: Int): List<Pair<String, Int>> {
        val byDay = data.daily.associate { it.day to it.reviews }
        val today = StudyStreak.startOfDay(System.currentTimeMillis())
        return (days - 1 downTo 0).map { offset ->
            val key = StudyStreak.dayKey(today - offset * StudyStreak.DAY_MS)
            key to (byDay[key] ?: 0)
        }
    }

    private fun bindWrongList(data: DashboardData) {
        binding.llWrong.removeAllViews()
        binding.tvWrongEmpty.isVisible = data.wrong.isEmpty()
        data.wrong.forEach { item ->
            val row = ItemWrongRowBinding.inflate(layoutInflater, binding.llWrong, false)
            row.tvHeadword.text = item.headword
            row.tvMeaning.text = item.meaning
            row.tvCount.text = getString(R.string.wrong_count_format, item.wrongCount)
            binding.llWrong.addView(row.root)
        }
    }

    private fun exportCsv() {
        val data = viewModel.state.value.data ?: return
        if (data.daily.isEmpty()) {
            Toast.makeText(this, R.string.export_empty, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            CsvExporter.export(applicationContext, data)
                .onSuccess { location ->
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.export_success, location),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.export_failed, error.message ?: "未知错误"),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
