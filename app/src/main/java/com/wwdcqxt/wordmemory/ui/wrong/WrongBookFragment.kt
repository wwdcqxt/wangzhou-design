package com.wwdcqxt.wordmemory.ui.wrong

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
import com.wwdcqxt.wordmemory.databinding.FragmentWrongbookBinding
import com.wwdcqxt.wordmemory.ui.common.appContainer
import com.wwdcqxt.wordmemory.ui.common.viewModelFactory
import kotlinx.coroutines.launch

/**
 * 错题本页面：列表 + 单个标记掌握 + 错题再练入口。
 */
class WrongBookFragment : Fragment() {

    private var _binding: FragmentWrongbookBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: WrongBookViewModel by viewModels {
        viewModelFactory { WrongBookViewModel(appContainer.reviewRepository) }
    }

    private val adapter = WrongWordAdapter(
        onClick = { item ->
            findNavController().navigate(
                R.id.action_wrong_to_detail,
                Bundle().apply { putLong("wordId", item.word.id) }
            )
        },
        onMastered = { item -> viewModel.markMastered(item.word.id) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWrongbookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvWrong.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWrong.adapter = adapter
        binding.btnPractice.setOnClickListener {
            findNavController().navigate(
                R.id.action_wrong_to_quiz,
                Bundle().apply { putBoolean("wrongOnly", true) }
            )
        }

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

    private fun render(state: WrongBookViewModel.UiState) {
        if (_binding == null) return
        binding.tvSummary.text = "共 ${state.items.size} 个错词 · 累计错误 ${state.totalWrongCount} 次"
        binding.rvWrong.isVisible = state.items.isNotEmpty()
        binding.groupEmpty.isVisible = !state.loading && state.items.isEmpty()
        binding.btnPractice.isVisible = state.items.isNotEmpty()
        adapter.submitList(state.items)
    }

    override fun onDestroyView() {
        binding.rvWrong.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
