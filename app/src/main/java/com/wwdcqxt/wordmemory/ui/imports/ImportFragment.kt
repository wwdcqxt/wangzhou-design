package com.wwdcqxt.wordmemory.ui.imports

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.databinding.FragmentImportBinding
import com.wwdcqxt.wordmemory.ui.common.appContainer
import com.wwdcqxt.wordmemory.ui.common.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 词库导入页：系统文件选择器（SAF）读取 CSV / TXT / JSON，先预览再确认导入。
 */
class ImportFragment : Fragment() {

    private var _binding: FragmentImportBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ImportViewModel by viewModels {
        viewModelFactory { ImportViewModel(appContainer.wordRepository) }
    }

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { readFile(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnPickFile.setOnClickListener {
            pickFileLauncher.launch(arrayOf("text/plain", "text/comma-separated-values", "application/json", "*/*"))
        }
        binding.btnConfirm.setOnClickListener { viewModel.confirmImport() }
        binding.etName.doAfterTextChanged { text -> viewModel.onNameChanged(text?.toString().orEmpty()) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch {
                    viewModel.events.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun render(state: ImportViewModel.UiState) {
        if (_binding == null) return

        binding.progressBar.isVisible = state.parsing || state.importing
        binding.btnConfirm.isEnabled = state.canImport && !state.importing
        binding.btnPickFile.isEnabled = !state.parsing && !state.importing

        if (binding.etName.text?.toString() != state.name && state.name.isNotEmpty()) {
            binding.etName.setText(state.name)
            binding.etName.setSelection(state.name.length)
        }

        binding.tvFileName.isVisible = state.fileName != null
        binding.tvFileName.text = state.fileName?.let { "已选择：$it" }

        val parsed = state.parsed
        binding.tvPreview.text = when {
            parsed == null && state.error != null -> state.error
            parsed == null -> getString(R.string.import_preview_empty)
            parsed.words.isEmpty() -> getString(R.string.import_empty_result)
            else -> getString(R.string.import_preview_format, parsed.words.size, parsed.skipped)
        }

        val errors = parsed?.errors.orEmpty()
        binding.tvErrors.isVisible = errors.isNotEmpty()
        binding.tvErrors.text = errors.take(5).joinToString("\n")
    }

    private fun readFile(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val name = queryDisplayName(uri) ?: "import.txt"
                    val content = requireContext().contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error("无法读取文件")
                    name to content
                }
            }
            result.onSuccess { (name, content) ->
                viewModel.onFilePicked(name, content)
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.import_read_failed, error.message ?: "未知错误"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            ?: return uri.lastPathSegment
        return cursor.use {
            if (!it.moveToFirst()) return null
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) it.getString(index) else uri.lastPathSegment
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
