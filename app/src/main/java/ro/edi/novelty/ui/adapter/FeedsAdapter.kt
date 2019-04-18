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
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import ro.edi.novelty.R
import ro.edi.novelty.databinding.FeedItemBinding
import ro.edi.novelty.model.Feed
import ro.edi.novelty.ui.viewmodel.FeedsViewModel

class FeedsAdapter(private val feedsModel: FeedsViewModel) : BaseAdapter<Feed>(FeedDiffCallback()) {
    companion object {
        const val FEED_TITLE = "feed_title"
        const val FEED_IS_STARRED = "feed_is_starred"
    }

    override fun getModel(): ViewModel {
        return feedsModel
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun getItemLayoutId(position: Int): Int {
        return R.layout.feed_item
    }

    override fun onItemClick(itemView: View, position: Int) {
        // FIXME edit feed
//        val i = Intent(context, NewsInfoActivity::class.java)
//        i.putExtra(NewsInfoActivity.EXTRA_NEWS_ID, getItem(position).id)
//        context.startActivity(i)
    }

    override fun getClickableViewIds(): IntArray? {
        val ids = IntArray(1)
        ids[0] = R.id.feed_star

        return ids
    }

    override fun onClick(v: View, position: Int) {
        if (v.id == R.id.feed_star) {
            feedsModel.setIsStarred(position, !getItem(position).isStarred)
        }
    }

    override fun bind(binding: ViewDataBinding, position: Int, payloads: MutableList<Any>) {
        val b = binding as FeedItemBinding

        val payload = payloads.first() as Set<*>
        payload.forEach {
            when (it) {
                FEED_TITLE -> b.feedTitle.text = getItem(position).title
                FEED_IS_STARRED -> b.feedStar.setImageDrawable(
                    ContextCompat.getDrawable(
                        binding.root.context,
                        feedsModel.getStarredImageRes(position)
                    )
                )
            }
        }
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<Feed>() {
        override fun areItemsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Feed, newItem: Feed): Any? {
            val payload = mutableSetOf<String>()

            if (oldItem.title != newItem.title) {
                payload.add(FEED_TITLE)
            }
            if (oldItem.isStarred != newItem.isStarred) {
                payload.add(FEED_IS_STARRED)
            }

            if (payload.isEmpty()) {
                return null
            }

            return payload
        }
    }
}