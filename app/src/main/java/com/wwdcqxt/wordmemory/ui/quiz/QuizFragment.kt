package com.wwdcqxt.wordmemory.ui.quiz

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.databinding.FragmentQuizBinding
import com.wwdcqxt.wordmemory.ui.common.appContainer
import com.wwdcqxt.wordmemory.ui.common.viewModelFactory
import kotlinx.coroutines.launch

/**
 * 测试模式页面：四选一，答完自动判分，结果用对话框展示。
 */
class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val wrongOnly: Boolean by lazy {
        requireArguments().getBoolean("wrongOnly", false)
    }

    private val viewModel: QuizViewModel by viewModels {
        viewModelFactory {
            QuizViewModel(
                wrongOnly = wrongOnly,
                wordRepository = appContainer.wordRepository,
                reviewRepository = appContainer.reviewRepository
            )
        }
    }

    private val optionButtons: List<MaterialButton>
        get() = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)

    /** 结果对话框只弹一次，避免每次状态刷新重复弹窗 */
    private var resultDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener { findNavController().popBackStack() }
        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.select(index) }
        }
        binding.btnNext.setOnClickListener { viewModel.next() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: QuizViewModel.UiState) {
        if (_binding == null) return

        if (state.error != null && state.questions.isEmpty()) {
            binding.tvProgress.text = state.error
            binding.tvFeedback.isVisible = true
            binding.tvFeedback.text = state.error
            binding.tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_error))
            optionButtons.forEach { it.isVisible = false }
            binding.btnNext.isVisible = false
            return
        }

        val current = state.current ?: return

        binding.tvProgress.text = getString(
            R.string.quiz_question_format,
            state.index + 1,
            state.questions.size
        )
        binding.tvHeadword.text = current.word.headword
        binding.tvPhonetic.text = current.word.phonetic

        optionButtons.forEachIndexed { index, button ->
            button.isVisible = true
            button.isEnabled = !state.isAnswered
            button.text = current.options.getOrElse(index) { "" }
            resetOptionStyle(button)
        }

        state.selected?.let { selected ->
            binding.tvFeedback.isVisible = true
            val correct = selected == current.correctIndex
            binding.tvFeedback.text = if (correct) {
                getString(R.string.quiz_correct)
            } else {
                getString(R.string.quiz_wrong, current.options[current.correctIndex])
            }
            binding.tvFeedback.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (correct) R.color.quality_easy else R.color.brand_error
                )
            )
            optionButtons.getOrNull(selected)?.let { styleOption(it, R.color.quality_again) }
            optionButtons.getOrNull(current.correctIndex)?.let { styleOption(it, R.color.quality_easy) }
            binding.btnNext.isVisible = true
            binding.btnNext.text = getString(
                if (state.index + 1 >= state.questions.size) R.string.quiz_finish else R.string.quiz_next
            )
        }

        if (state.finished) {
            if (resultDialog == null) showResultDialog(state)
        } else {
            resultDialog = null
        }
    }

    private fun resetOptionStyle(button: MaterialButton) {
        button.backgroundTintList = null
        button.strokeWidth = 1
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
    }

    private fun styleOption(button: MaterialButton, colorRes: Int) {
        val color = ContextCompat.getColor(requireContext(), colorRes)
        button.backgroundTintList = ColorStateList.valueOf(color)
        button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    private fun showResultDialog(state: QuizViewModel.UiState) {
        if (!isAdded) return
        binding.btnNext.isVisible = false
        val total = state.questions.size
        val accuracy = if (total == 0) 0 else state.correctCount * 100 / total

        resultDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.quiz_result_title)
            .setMessage(getString(R.string.quiz_result_format, state.correctCount, total, accuracy))
            .setCancelable(false)
            .setPositiveButton(R.string.quiz_again) { dialog, _ ->
                dialog.dismiss()
                resultDialog = null
                viewModel.restart()
            }
            .setNegativeButton(R.string.common_back) { dialog, _ ->
                dialog.dismiss()
                resultDialog = null
                findNavController().popBackStack()
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
