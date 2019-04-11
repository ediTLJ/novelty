package ro.edi.novelty.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import ro.edi.novelty.model.Feed

class FeedDiffCallback : DiffUtil.ItemCallback<Feed>() {
    override fun areItemsTheSame(oldItem: Feed, newItem: Feed): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Feed, newItem: Feed): Boolean {
        return oldItem == newItem
    }
}