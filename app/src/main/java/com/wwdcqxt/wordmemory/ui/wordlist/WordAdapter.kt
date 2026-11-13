package com.wwdcqxt.wordmemory.ui.wordlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity
import com.wwdcqxt.wordmemory.databinding.ItemWordBinding

/** 单词列表项：单词 + 音标 + 释义 */
class WordAdapter(
    private val onClick: (WordEntity) -> Unit
) : ListAdapter<WordEntity, WordAdapter.WordViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WordViewHolder(
        private val binding: ItemWordBinding,
        private val onClick: (WordEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(word: WordEntity) {
            binding.tvHeadword.text = word.headword
            binding.tvMeaning.text = word.meaning
            binding.tvPhonetic.text = word.phonetic
            binding.root.setOnClickListener { onClick(word) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<WordEntity>() {
            override fun areItemsTheSame(oldItem: WordEntity, newItem: WordEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WordEntity, newItem: WordEntity): Boolean =
                oldItem == newItem
        }
    }
}
