package com.wwdcqxt.wordmemory.ui.wordbook

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.databinding.FragmentWordbookBinding
import com.wwdcqxt.wordmemory.ui.common.appContainer
import com.wwdcqxt.wordmemory.ui.common.viewModelFactory
import kotlinx.coroutines.launch

/**
 * 词库页：词库列表 + 导入入口 + 删除确认。
 */
class WordBookFragment : Fragment() {

    private var _binding: FragmentWordbookBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: WordBookViewModel by viewModels {
        viewModelFactory {
            WordBookViewModel(
                wordRepository = appContainer.wordRepository,
                seedImporter = appContainer.seedWordBookImporter
            )
        }
    }

    private val adapter = WordBookAdapter(
        onClick = { book ->
            findNavController().navigate(
                R.id.action_wordbooks_to_list,
                android.os.Bundle().apply {
                    putLong("bookId", book.id)
                    putString("bookName", book.name)
                }
            )
        },
        onDelete = { book -> confirmDelete(book) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordbookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvBooks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBooks.adapter = adapter
        binding.fabImport.setOnClickListener { findNavController().navigate(R.id.action_wordbooks_to_import) }
        binding.btnSeed.setOnClickListener { viewModel.loadSeedWordBook() }

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

    private fun render(state: WordBookViewModel.UiState) {
        if (_binding == null) return
        binding.progressBar.isVisible = state.loading
        binding.groupEmpty.isVisible = !state.loading && state.books.isEmpty()
        binding.rvBooks.isVisible = !state.loading && state.books.isNotEmpty()
        binding.tvBookSummary.text = "共 ${state.books.size} 个词库 · ${state.totalWords} 个单词"
        adapter.submitList(state.books)
    }

    private fun confirmDelete(book: com.wwdcqxt.wordmemory.core.data.entity.WordBookEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wordbook_delete)
            .setMessage(getString(R.string.wordbook_delete_confirm, book.name))
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.common_ok) { _, _ -> viewModel.deleteBook(book) }
            .show()
    }

    override fun onDestroyView() {
        binding.rvBooks.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
