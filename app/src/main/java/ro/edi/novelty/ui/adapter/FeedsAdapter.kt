/*
* Copyright 2019-2023 Eduard Scarlat
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

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ro.edi.novelty.R
import ro.edi.novelty.databinding.FeedItemBinding
import ro.edi.novelty.model.Feed
import ro.edi.novelty.ui.FeedInfoActivity
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import ro.edi.util.getColorRes


class FeedsAdapter(private val activity: Activity, private val feedsModel: FeedsViewModel) :
    BaseAdapter<Feed>(FeedDiffCallback()) {
    companion object {
        const val FEED_TITLE = "feed_title"
        const val FEED_URL = "feed_url"
        const val FEED_TYPE = "feed_type"
        const val FEED_IS_STARRED = "feed_is_starred"
    }

    val itemTouchHelper by lazy(LazyThreadSafetyMode.NONE) {
        ItemTouchHelper(FeedTouchCallback())
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
        val i = Intent(activity, FeedInfoActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        i.putExtra(FeedInfoActivity.EXTRA_FEED_ID, getItem(position).id)
        val options = ActivityOptions.makeSceneTransitionAnimation(
            activity,
            itemView,
            "shared_feed_container"
        )
        activity.startActivity(i, options.toBundle())
    }

    override fun getTouchableViewIds(): IntArray {
        val ids = IntArray(1)
        ids[0] = R.id.feed_drag

        return ids
    }

    override fun onTouch(
        v: View,
        event: MotionEvent?,
        holder: RecyclerView.ViewHolder,
        position: Int
    ): Boolean {
        if (v.id == R.id.feed_drag) {
            if (event?.actionMasked == MotionEvent.ACTION_DOWN) {
                itemTouchHelper.startDrag(holder)
            }
            return true
        }

        return false
    }

    override fun getClickableViewIds(): IntArray {
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
                FEED_URL -> b.feedUrl.text = getItem(position).url
                FEED_TYPE -> b.feedType.text =
                    binding.root.context.getText(feedsModel.getTypeTextRes(position))
                FEED_IS_STARRED -> b.feedStar.setImageDrawable(
                    ContextCompat.getDrawable(
                        binding.root.context,
                        feedsModel.getStarredImageRes(position)
                    )
                )
            }
        }
    }

    fun moveItem(oldPosition: Int, newPosition: Int) {
        feedsModel.moveFeed(oldPosition, newPosition)
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<Feed>() {
        override fun areItemsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem.title == newItem.title
                    && oldItem.url == newItem.url
                    && oldItem.type == newItem.type
                    && oldItem.isStarred == newItem.isStarred
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
            if (oldItem.isStarred != newItem.isStarred) {
                payload.add(FEED_IS_STARRED)
            }

            if (payload.isEmpty()) {
                return null
            }

            return payload
        }
    }

    class FeedTouchCallback : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0
    ) {
        private var colorDrag = -1
        private var fromPos = -1
        private var toPos = -1

        override fun isLongPressDragEnabled() = true
        override fun isItemViewSwipeEnabled() = false

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val oldPos = viewHolder.bindingAdapterPosition
            val newPos = target.bindingAdapterPosition

            // logi("onMove() oldPos: $oldPos, newPos: $newPos")
            // logi("onMove() fromPos: $fromPos, newPos: $newPos")

            val adapter = recyclerView.adapter as FeedsAdapter

            if (oldPos != fromPos || newPos != toPos) {
                // logi("onMove() do notifyItemMoved")
                // adapter.notifyItemMoved(oldPos, newPos)

                if (oldPos != newPos) {
                    // logi("onMove() swap pages in db")
                    adapter.moveItem(oldPos, newPos)
                }
            }

            if (fromPos < 0) {
                fromPos = oldPos
            }
            toPos = newPos

            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)

            val itemView = viewHolder?.itemView
            itemView ?: return

            if (colorDrag < 0) {
                colorDrag = ContextCompat.getColor(
                    itemView.context,
                    getColorRes(itemView.context, R.attr.colorControlHighlight)
                )
            }

            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                itemView.setBackgroundColor(colorDrag)
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)

            viewHolder.itemView.setBackgroundColor(0)

            fromPos = -1
            toPos = -1
        }
    }
}