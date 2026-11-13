package com.wwdcqxt.wordmemory.ui.wordbook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wwdcqxt.wordmemory.core.data.entity.WordBookEntity
import com.wwdcqxt.wordmemory.databinding.ItemWordBookBinding

/**
 * 词库列表适配器：点击进入词库内单词列表，右侧按钮删除。
 */
class WordBookAdapter(
    private val onClick: (WordBookEntity) -> Unit,
    private val onDelete: (WordBookEntity) -> Unit
) : ListAdapter<WordBookEntity, WordBookAdapter.BookViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemWordBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding, onClick, onDelete)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BookViewHolder(
        private val binding: ItemWordBookBinding,
        private val onClick: (WordBookEntity) -> Unit,
        private val onDelete: (WordBookEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: WordBookEntity) {
            binding.tvBookName.text = book.name
            binding.tvBookMeta.text = "${book.wordCount} 词 · ${sourceLabel(book.source)}"
            binding.root.setOnClickListener { onClick(book) }
            binding.btnDelete.setOnClickListener { onDelete(book) }
        }

        private fun sourceLabel(source: String): String = when (source) {
            WordBookEntity.SOURCE_BUILTIN -> "内置词库"
            WordBookEntity.SOURCE_CSV -> "CSV 导入"
            WordBookEntity.SOURCE_TXT -> "TXT 导入"
            WordBookEntity.SOURCE_JSON -> "JSON 导入"
            else -> "导入"
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<WordBookEntity>() {
            override fun areItemsTheSame(oldItem: WordBookEntity, newItem: WordBookEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WordBookEntity, newItem: WordBookEntity): Boolean =
                oldItem == newItem
        }
    }
}
