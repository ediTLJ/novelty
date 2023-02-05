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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import ro.edi.novelty.ui.FeedFragment
import ro.edi.novelty.ui.StarredFeedsFragment
import ro.edi.novelty.ui.StarredNewsFragment
import ro.edi.novelty.ui.viewmodel.FeedsViewModel

class FeedsPagerAdapter(fa: FragmentActivity, private val feedsModel: FeedsViewModel) :
    FragmentStateAdapter(fa) {

    override fun getItemId(page: Int): Long {
        return when (page) {
            0 -> 0L
            1 -> 1L
            else -> feedsModel.getFeed(page - 2)?.id?.toLong() ?: RecyclerView.NO_ID
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return when (itemId) {
            0L, 1L -> true
            else -> feedsModel.feeds.value?.find { it.id.toLong() == itemId } != null
        }
    }

    override fun getItemCount(): Int {
        return feedsModel.feeds.value?.size?.plus(2) ?: 2
    }

    override fun createFragment(page: Int): Fragment {
        return when (page) {
            0 -> StarredNewsFragment.newInstance()
            1 -> StarredFeedsFragment.newInstance()
            else -> {
                // val feedState = mFeedState[feed.title]
                // if (feedState == null) {
                //     mFeedState[feed.title] = FEED_STATE_NEEDS_REFRESH
                // }
                feedsModel.getFeed(page - 2)?.let {
                    return FeedFragment.newInstance(it.id)
                }

                return StarredNewsFragment.newInstance() // should never happen
            }
        }
    }

//    companion object {
//        private const val FEED_STATE_NEEDS_REFRESH = 0
//        private const val FEED_STATE_REFRESHED = 1
//    }

//    fun refreshFeed(position: Int) {
//        logi("refresh: %d", position)
//
//        if (position <= 0) {
//            return
//        }
//
//        val f = getFragment(position) as FeedFragment
//        if (f == null || f.arguments == null) {
//            logi("refresh denied: %d", position)
//            return
//        }
//
//        val feedTitle = f.arguments!!.getString("feedId")
//
//        val feedState = mFeedState[feedTitle]
//        logi("refresh: %d: state %d", position, feedState)
//
//        if (feedState != null && feedState == FEED_STATE_NEEDS_REFRESH) {
//            logi(TAG, "refresh: %d: %s", position, feedTitle)
//            f.refresh()
//            mFeedState[feedTitle] = FEED_STATE_REFRESHED
//        }
//    }
}