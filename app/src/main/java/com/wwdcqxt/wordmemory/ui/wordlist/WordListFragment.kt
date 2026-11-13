package com.wwdcqxt.wordmemory.ui.wordlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity
import com.wwdcqxt.wordmemory.databinding.FragmentWordlistBinding
import com.wwdcqxt.wordmemory.ui.common.appContainer
import com.wwdcqxt.wordmemory.ui.common.viewModelFactory
import kotlinx.coroutines.launch

/**
 * 词库内单词列表：支持搜索，点击进入单词详情（列表 → 详情导航）。
 */
class WordListFragment : Fragment() {

    private var _binding: FragmentWordlistBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val bookId: Long by lazy { requireArguments().getLong(ARG_BOOK_ID) }
    private val bookName: String by lazy {
        requireArguments().getString(ARG_BOOK_NAME) ?: getString(R.string.nav_wordbooks)
    }

    private val viewModel: WordListViewModel by viewModels {
        viewModelFactory { WordListViewModel(appContainer.wordRepository, bookId) }
    }

    private val adapter = WordAdapter { word -> openDetail(word) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = bookName
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.rvWords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWords.adapter = adapter

        binding.etSearch.doAfterTextChanged { text ->
            viewModel.onKeywordChanged(text?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.words.collect { words ->
                    adapter.submitList(words)
                    binding.tvEmpty.isVisible = words.isEmpty()
                }
            }
        }
    }

    private fun openDetail(word: WordEntity) {
        findNavController().navigate(
            R.id.action_list_to_detail,
            Bundle().apply { putLong("wordId", word.id) }
        )
    }

    override fun onDestroyView() {
        binding.rvWords.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_BOOK_ID = "bookId"
        const val ARG_BOOK_NAME = "bookName"
    }
}
