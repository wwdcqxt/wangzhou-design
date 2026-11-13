package com.wwdcqxt.wordmemory.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.databinding.FragmentHomeBinding
import com.wwdcqxt.wordmemory.notification.NotificationHelper
import com.wwdcqxt.wordmemory.ui.common.appContainer
import com.wwdcqxt.wordmemory.ui.common.viewModelFactory
import kotlinx.coroutines.launch

/**
 * 首页：今日复习队列 + 学习概览 + 空/加载/错误三态。
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory {
            HomeViewModel(
                reviewRepository = appContainer.reviewRepository,
                wordRepository = appContainer.wordRepository,
                settingsRepository = appContainer.settingsRepository,
                seedImporter = appContainer.seedWordBookImporter,
                statsRepository = appContainer.statsRepository
            )
        }
    }

    private val adapter = DueWordAdapter { dueWord ->
        findNavController().navigate(R.id.action_home_to_review)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvDue.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDue.adapter = adapter

        binding.btnStartReview.setOnClickListener {
            if (viewModel.state.value.dueCount > 0) {
                findNavController().navigate(R.id.action_home_to_review)
            } else {
                Toast.makeText(requireContext(), R.string.home_empty_title, Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnQuiz.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_quiz)
        }
        binding.btnSeed.setOnClickListener { viewModel.importSeedWordBook() }
        binding.btnRetry.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch {
                    viewModel.events.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从复习页返回时重新计算到期队列
        viewModel.refresh()
    }

    private fun render(state: HomeViewModel.UiState) {
        if (_binding == null) return

        binding.progressBar.isVisible = state.loading && state.dueWords.isEmpty()
        binding.groupError.isVisible = state.error != null
        binding.tvError.text = state.error.orEmpty()

        binding.rvDue.isVisible = !state.loading && state.error == null && state.dueWords.isNotEmpty()

        val showEmpty = !state.loading && state.error == null && state.dueWords.isEmpty()
        binding.groupEmpty.isVisible = showEmpty
        binding.btnSeed.isVisible = showEmpty && state.totalWords == 0

        binding.tvReviewedValue.text = "${state.todayReviews} / ${state.dailyGoal}"
        binding.tvStreakValue.text = "${state.streakDays} 天"
        binding.tvLearnedValue.text = "${state.learnedCount} 词"

        val notificationDenied = state.reminderEnabled && !NotificationHelper.canPost(requireContext())
        binding.tvReminderBanner.isVisible = notificationDenied

        binding.btnStartReview.isEnabled = state.dueCount > 0

        adapter.submitList(state.dueWords)
    }

    override fun onDestroyView() {
        binding.rvDue.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
