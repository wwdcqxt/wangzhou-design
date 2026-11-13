package com.wwdcqxt.wordmemory.ui.wrong

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wwdcqxt.wordmemory.core.data.model.WrongWord
import com.wwdcqxt.wordmemory.databinding.ItemWrongWordBinding

/** 错题列表项：单词、释义、错误次数，右侧“已掌握”按钮 */
class WrongWordAdapter(
    private val onClick: (WrongWord) -> Unit,
    private val onMastered: (WrongWord) -> Unit
) : ListAdapter<WrongWord, WrongWordAdapter.WrongViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WrongViewHolder {
        val binding = ItemWrongWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WrongViewHolder(binding, onClick, onMastered)
    }

    override fun onBindViewHolder(holder: WrongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WrongViewHolder(
        private val binding: ItemWrongWordBinding,
        private val onClick: (WrongWord) -> Unit,
        private val onMastered: (WrongWord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WrongWord) {
            binding.tvHeadword.text = item.word.headword
            binding.tvMeaning.text = item.word.meaning
            binding.tvWrongCount.text = "错 ${item.wrongCount} 次"
            binding.root.setOnClickListener { onClick(item) }
            binding.btnMastered.setOnClickListener { onMastered(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<WrongWord>() {
            override fun areItemsTheSame(oldItem: WrongWord, newItem: WrongWord): Boolean =
                oldItem.word.id == newItem.word.id

            override fun areContentsTheSame(oldItem: WrongWord, newItem: WrongWord): Boolean =
                oldItem == newItem
        }
    }
}
