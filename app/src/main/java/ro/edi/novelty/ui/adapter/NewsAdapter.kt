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

import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import ro.edi.novelty.R
import ro.edi.novelty.databinding.NewsItemBinding
import ro.edi.novelty.model.News
import ro.edi.novelty.ui.NewsInfoActivity
import ro.edi.novelty.ui.viewmodel.NewsViewModel

class NewsAdapter(private val newsModel: NewsViewModel) : BaseAdapter<News>(NewsDiffCallback()) {
    companion object {
        const val NEWS_PUB_DATE = "news_pub_date"
        const val NEWS_FEED_TITLE = "news_feed_title"
        const val NEWS_TITLE = "news_title"
        const val NEWS_STATE = "news_state"
    }

    override fun getModel(): ViewModel {
        return newsModel
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun getItemLayoutId(position: Int): Int {
        return R.layout.news_item
    }

    override fun onItemClick(itemView: View, position: Int) {
        newsModel.setIsRead(position, true)

        val i = Intent(itemView.context, NewsInfoActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        i.putExtra(NewsInfoActivity.EXTRA_NEWS_ID, getItem(position).id)
        itemView.context.startActivity(i)
    }

    override fun bind(binding: ViewDataBinding, position: Int, payloads: MutableList<Any>) {
        val b = binding as NewsItemBinding

        val payload = payloads.first() as Set<*>
        payload.forEach {
            when (it) {
                NEWS_PUB_DATE -> b.date.text = newsModel.getDisplayDate(position)
                NEWS_FEED_TITLE -> b.feed.text = getItem(position).feedTitle
                NEWS_TITLE -> b.title.text = getItem(position).title
                NEWS_STATE -> {
                    val colorInfo = ContextCompat.getColor(
                        binding.root.context,
                        newsModel.getInfoTextColorRes(binding.root.context, position)
                    )
                    val colorTitle = ContextCompat.getColor(
                        binding.root.context,
                        newsModel.getTitleTextColorRes(binding.root.context, position)
                    )

                    b.feed.setTextColor(colorInfo)
                    b.title.setTextColor(colorTitle)
                    b.date.setTextColor(colorInfo)
                }
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<News>() {
        override fun areItemsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: News, newItem: News): Boolean {
            return false // we do this because we want the shown date to always be updated
        }

        override fun getChangePayload(oldItem: News, newItem: News): Any? {
            val payload = mutableSetOf<String>()

            payload.add(NEWS_PUB_DATE) // always add this (because we show the relative date/time in the UI)

            if (oldItem.feedTitle != newItem.feedTitle) {
                payload.add(NEWS_FEED_TITLE)
            }
            if (oldItem.title != newItem.title) {
                payload.add(NEWS_TITLE)
            }
            if (oldItem.isRead != newItem.isRead || oldItem.isStarred != newItem.isStarred) {
                payload.add(NEWS_STATE)
            }

            if (payload.isEmpty()) {
                return null
            }

            return payload
        }
    }
}