package com.wwdcqxt.wordmemory.ui.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.core.domain.algorithm.EbbinghausScheduler
import com.wwdcqxt.wordmemory.databinding.FragmentReviewBinding
import com.wwdcqxt.wordmemory.ui.common.appContainer
import com.wwdcqxt.wordmemory.ui.common.viewModelFactory
import kotlinx.coroutines.launch

/**
 * 复习卡片页：先回忆 → 点卡片看释义 → 四档评分 → 下一张。
 * 评分会写回 Room，下一条复习时间由艾宾浩斯算法决定。
 */
class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ReviewViewModel by viewModels {
        viewModelFactory { ReviewViewModel(appContainer.reviewRepository) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener { findNavController().popBackStack() }
        binding.btnBackHome.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, inclusive = false)
        }
        binding.cardWord.setOnClickListener { viewModel.reveal() }
        binding.btnShowAnswer.setOnClickListener { viewModel.reveal() }
        binding.btnAgain.setOnClickListener { viewModel.grade(EbbinghausScheduler.QUALITY_AGAIN) }
        binding.btnHard.setOnClickListener { viewModel.grade(EbbinghausScheduler.QUALITY_HARD) }
        binding.btnGood.setOnClickListener { viewModel.grade(EbbinghausScheduler.QUALITY_GOOD) }
        binding.btnEasy.setOnClickListener { viewModel.grade(EbbinghausScheduler.QUALITY_EASY) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: ReviewViewModel.UiState) {
        if (_binding == null) return

        binding.pbReview.max = state.total.coerceAtLeast(1)
        binding.pbReview.progress = state.index
        binding.tvProgress.text = getString(R.string.review_progress, state.index, state.total)

        val current = state.current
        val showSession = !state.finished && current != null

        binding.cardWord.isVisible = showSession
        binding.answerActions.isVisible = showSession
        binding.groupFinished.isVisible = state.finished && !state.loading

        if (state.finished && !state.loading) {
            binding.tvFinishedDesc.text = getString(
                R.string.review_finished_desc,
                state.reviewedCount,
                state.remaining
            )
            return
        }

        if (current == null) return

        binding.tvHeadword.text = current.word.headword
        binding.tvPhonetic.text = current.word.phonetic
        binding.tvMeaning.text = current.word.meaning

        // 未揭晓答案时释义保持不可见（INVISIBLE 占位，避免卡片高度跳动）
        binding.tvMeaning.isVisible = state.revealed
        binding.tvExample.isVisible = state.revealed && current.word.example.isNotBlank()
        binding.tvExample.text = current.word.example

        binding.btnShowAnswer.isVisible = !state.revealed
        binding.qualityGroup.isVisible = state.revealed
        binding.tvTapHint.isVisible = !state.revealed

        state.hints[EbbinghausScheduler.QUALITY_AGAIN]?.let {
            binding.btnAgain.text = "${getString(R.string.review_again)}\n$it"
        }
        state.hints[EbbinghausScheduler.QUALITY_HARD]?.let {
            binding.btnHard.text = "${getString(R.string.review_hard)}\n$it"
        }
        state.hints[EbbinghausScheduler.QUALITY_GOOD]?.let {
            binding.btnGood.text = "${getString(R.string.review_good)}\n$it"
        }
        state.hints[EbbinghausScheduler.QUALITY_EASY]?.let {
            binding.btnEasy.text = "${getString(R.string.review_easy)}\n$it"
        }
        binding.tvNextHint.text = "评分后按艾宾浩斯曲线安排下一次复习"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
