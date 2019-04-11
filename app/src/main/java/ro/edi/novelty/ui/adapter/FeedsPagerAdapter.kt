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

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import ro.edi.novelty.R
import ro.edi.novelty.ui.FeedFragment
import ro.edi.novelty.ui.MyFeedsFragment
import ro.edi.novelty.ui.MyNewsFragment
import ro.edi.novelty.ui.viewmodel.FeedsViewModel

class FeedsPagerAdapter(fm: FragmentManager, application: Application, private val feedsModel: FeedsViewModel) :
    FragmentStatePagerAdapter(fm) {
    private val txtMyNews: CharSequence = application.getText(R.string.tab_my_news)
    private val txtMyFeeds: CharSequence = application.getText(R.string.tab_my_feeds)

    override fun getCount(): Int {
        return feedsModel.feeds.value?.size?.plus(2) ?: 2
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> MyNewsFragment.newInstance()
            1 -> MyFeedsFragment.newInstance()
            else -> {
//        val feedState = mFeedState[feed.title]
//        if (feedState == null) {
//            mFeedState[feed.title] = FEED_STATE_NEEDS_REFRESH
//        }
                feedsModel.getFeed(position - 2)?.let {
                    return FeedFragment.newInstance(it.id)
                }

                return MyNewsFragment.newInstance() // should never happen
            }
        }
    }
//    companion object {
//        private const val FEED_STATE_NEEDS_REFRESH = 0
//        private const val FEED_STATE_REFRESHED = 1
//    }

//    override fun getItemId(position: Int): Long {
//        return when (position) {
//            0 -> -1
//            1 -> 0
//            else -> feedsModel.getFeedId(position).toLong()
//        }
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

    // FIXME
//    override fun getItemPosition(@NonNull `object`: Any): Int {
//        if (`object` is BookmarksFragment) {
//            return if (mFeedsManager.feedsCount < 2) {
//                PagerAdapter.POSITION_NONE // the fragment will be re-created
//            } else PagerAdapter.POSITION_UNCHANGED
//        }
//
//        val f = `object` as FeedFragment
//        val args = f.arguments
//            ?: return PagerAdapter.POSITION_NONE // this should never happen
//
//        val fPosition = args.getInt("position")
//        val fTitle = args.getString("feedId")
//
//        val feedsManager = FeedsManager.instance
//        for (k in 1 until FeedsManager.MAX_FEEDS_COUNT + 1) {
//            val feed = feedsManager.getFeed(k)
//
//            if (feed != null && feed.title.equals(fTitle!!, ignoreCase = true)) {
//                if (k == fPosition) {
//                    return PagerAdapter.POSITION_UNCHANGED
//                }
//            }
//        }
//
//        return PagerAdapter.POSITION_NONE // the fragment will be re-created
//    }
//
//    fun getFragment(position: Int): Fragment {
//        return mFragments.get(position)
//    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> txtMyNews
            1 -> txtMyFeeds
            else -> feedsModel.getFeed(position - 2)?.title?.toUpperCase()
        }
    }
}