/*
* Copyright 2019 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package ro.edi.novelty.ui.adapter

import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import ro.edi.novelty.R
import ro.edi.novelty.databinding.FeedItemBinding
import ro.edi.novelty.model.Feed
import ro.edi.novelty.ui.viewmodel.FeedsFoundViewModel

class FeedsFoundAdapter(
    private val feedsFoundModel: FeedsFoundViewModel,
    private val itemClickListener: (View, Int) -> Unit
) :
    BaseAdapter<Feed>(FeedDiffCallback()) {
    companion object {
        const val FEED_TITLE = "feed_title"
        const val FEED_URL = "feed_url"
        const val FEED_TYPE = "feed_type"
    }

    override fun getModel(): ViewModel {
        return feedsFoundModel
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun getItemLayoutId(position: Int): Int {
        return R.layout.feed_found_item
    }

    override fun onItemClick(itemView: View, position: Int) {
        itemClickListener(itemView, position)
    }

    override fun bind(binding: ViewDataBinding, position: Int, payloads: MutableList<Any>) {
        val b = binding as FeedItemBinding

        val payload = payloads.first() as Set<*>
        payload.forEach {
            when (it) {
                FEED_TITLE -> b.feedTitle.text = getItem(position).title
                FEED_URL -> b.feedUrl.text = getItem(position).url
                FEED_TYPE -> b.feedType.text =
                    binding.root.context.getText(feedsFoundModel.getTypeTextRes(position))
            }
        }
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<Feed>() {
        override fun areItemsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem.title == newItem.title
                    && oldItem.url == newItem.url
                    && oldItem.type == newItem.type
        }

        override fun getChangePayload(oldItem: Feed, newItem: Feed): Any? {
            val payload = mutableSetOf<String>()

            if (oldItem.title != newItem.title) {
                payload.add(FEED_TITLE)
            }
            if (oldItem.url != newItem.url) {
                payload.add(FEED_URL)
            }
            if (oldItem.type != newItem.type) {
                payload.add(FEED_TYPE)
            }

            if (payload.isEmpty()) {
                return null
            }

            return payload
        }
    }
}