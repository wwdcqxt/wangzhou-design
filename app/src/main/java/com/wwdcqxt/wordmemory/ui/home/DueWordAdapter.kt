package com.wwdcqxt.wordmemory.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wwdcqxt.wordmemory.core.data.model.DueWord
import com.wwdcqxt.wordmemory.core.domain.algorithm.EbbinghausScheduler
import com.wwdcqxt.wordmemory.databinding.ItemDueWordBinding
import com.wwdcqxt.wordmemory.util.TimeFormats

/**
 * 今日复习队列适配器。
 * 列表里只显示单词与记忆状态，不显示释义——避免提前看到答案影响回忆。
 */
class DueWordAdapter(
    private val onClick: (DueWord) -> Unit
) : ListAdapter<DueWord, DueWordAdapter.DueWordViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DueWordViewHolder {
        val binding = ItemDueWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DueWordViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: DueWordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DueWordViewHolder(
        private val binding: ItemDueWordBinding,
        private val onClick: (DueWord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DueWord) {
            binding.tvHeadword.text = item.word.headword
            binding.tvPhonetic.text = item.word.phonetic
            binding.tvState.text = EbbinghausScheduler.stateLabel(item.state)
            binding.tvInterval.text = if (item.intervalDays > 0) {
                "间隔 ${item.intervalDays} 天"
            } else {
                TimeFormats.relativeDue(item.dueAt)
            }
            binding.pbStrength.progress = item.strengthPercent
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<DueWord>() {
            override fun areItemsTheSame(oldItem: DueWord, newItem: DueWord): Boolean =
                oldItem.word.id == newItem.word.id

            override fun areContentsTheSame(oldItem: DueWord, newItem: DueWord): Boolean =
                oldItem.word == newItem.word &&
                    oldItem.dueAt == newItem.dueAt &&
                    oldItem.repetitions == newItem.repetitions &&
                    oldItem.intervalDays == newItem.intervalDays
        }
    }
}
