package com.wwdcqxt.wordmemory.ui.detail

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
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.core.domain.algorithm.EbbinghausScheduler
import com.wwdcqxt.wordmemory.databinding.FragmentWordDetailBinding
import com.wwdcqxt.wordmemory.databinding.ItemReviewLogBinding
import com.wwdcqxt.wordmemory.ui.common.appContainer
import com.wwdcqxt.wordmemory.ui.common.viewModelFactory
import com.wwdcqxt.wordmemory.util.TimeFormats
import kotlinx.coroutines.launch

/**
 * 单词详情页：释义、例句、记忆状态、复习记录，以及在线释义 / 熟悉 / 错题操作。
 */
class WordDetailFragment : Fragment() {

    private var _binding: FragmentWordDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val wordId: Long by lazy { requireArguments().getLong("wordId") }

    private val viewModel: WordDetailViewModel by viewModels {
        viewModelFactory {
            WordDetailViewModel(
                wordId = wordId,
                wordRepository = appContainer.wordRepository,
                reviewRepository = appContainer.reviewRepository,
                dictionaryRepository = appContainer.dictionaryRepository
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnOnline.setOnClickListener { viewModel.lookupOnline() }
        binding.btnFamiliar.setOnClickListener { viewModel.toggleFamiliar() }
        binding.btnWrong.setOnClickListener { viewModel.toggleWrong() }

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

    private fun render(state: WordDetailViewModel.UiState) {
        if (_binding == null) return
        val word = state.word ?: return

        binding.tvHeadword.text = word.headword
        binding.tvPhonetic.text = word.phonetic.ifBlank { "—" }
        binding.tvMeaning.text = word.meaning

        val hasExample = word.example.isNotBlank() || word.exampleCn.isNotBlank()
        binding.cardExample.isVisible = hasExample
        binding.tvExample.text = word.example
        binding.tvExampleCn.text = word.exampleCn

        val card = state.card
        binding.tvStateChip.text = EbbinghausScheduler.stateLabel(card?.state ?: 0)
        binding.tvInterval.text = when {
            card == null -> "—"
            card.intervalDays > 0 -> "${card.intervalDays} 天"
            else -> "学习阶段（分钟级）"
        }
        binding.tvEase.text = String.format(java.util.Locale.CHINA, "%.2f", card?.easeFactor ?: 2.5)
        binding.tvDueAt.text = card?.let { TimeFormats.dateTime(it.dueAt) } ?: "—"

        val strength = EbbinghausScheduler.memoryStrength(
            card?.intervalDays ?: 0,
            card?.easeFactor ?: 2.5,
            card?.repetitions ?: 0
        )
        binding.pbStrength.progress = (strength * 100).toInt()
        binding.tvStrengthLabel.text = "记忆强度 ${(strength * 100).toInt()}%"

        binding.btnFamiliar.text = getString(
            if (word.familiar) R.string.word_detail_unfamiliar else R.string.word_detail_familiar
        )
        binding.btnWrong.text = getString(
            if (state.isWrong) R.string.word_detail_remove_wrong else R.string.word_detail_add_wrong
        )

        binding.pbOnline.isVisible = state.onlineLoading
        binding.btnOnline.isEnabled = !state.onlineLoading
        state.onlineText?.let {
            binding.tvOnlineLabel.isVisible = true
            binding.tvOnlineLabel.text = it
        }

        renderHistory(state)
    }

    private fun renderHistory(state: WordDetailViewModel.UiState) {
        binding.llHistory.removeAllViews()
        binding.tvNoHistory.isVisible = state.logs.isEmpty()
        state.logs.forEach { log ->
            val item = ItemReviewLogBinding.inflate(layoutInflater, binding.llHistory, false)
            item.tvDate.text = TimeFormats.dateTime(log.reviewedAt)
            item.tvQuality.text = qualityLabel(log.quality)
            item.tvInterval.text = if (log.nextIntervalDays > 0) {
                "间隔 ${log.prevIntervalDays} 天 → ${log.nextIntervalDays} 天"
            } else {
                "已遗忘，${log.nextIntervalDays.coerceAtLeast(0)} 天后重新学习"
            }
            binding.llHistory.addView(item.root)
        }
    }

    private fun qualityLabel(quality: Int): String = when (quality) {
        EbbinghausScheduler.QUALITY_AGAIN -> "忘记"
        EbbinghausScheduler.QUALITY_HARD -> "模糊"
        EbbinghausScheduler.QUALITY_GOOD -> "记得"
        EbbinghausScheduler.QUALITY_EASY -> "简单"
        else -> "质量 $quality"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
