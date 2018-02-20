/*
* Copyright 2015 Eduard Scarlat
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
package ro.edi.novelty.ui.util;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import ro.edi.novelty.R;
import ro.edi.novelty.data.Feed;
import ro.edi.novelty.core.FeedsManager;
import ro.edi.novelty.ui.BookmarksFragment;
import ro.edi.novelty.ui.FeedFragment;
import ro.edi.util.Log;

import java.util.HashMap;

public class FeedsAdapter extends FragmentStatePagerAdapter {
    private static final String TAG = "FEEDS.ADAPTER";

    // sparse array to keep track of adapter fragments/items
    private SparseArray<Fragment> mFragments;

    private final CharSequence mTxtBookmarks, mTxtMyNews;
    private final FeedsManager mFeedsManager;

    private static final int FEED_STATE_NEEDS_REFRESH = 0;
    private static final int FEED_STATE_REFRESHED = 1;
    private HashMap<String, Integer> mFeedState;

    /**
     * Constructor.
     *
     * @param context a general app context: e.g. getApplication()
     */
    public FeedsAdapter(Context context, FragmentManager fm) {
        super(fm);

        mTxtBookmarks = context.getText(R.string.tab_bookmarks);
        mTxtMyNews = context.getText(R.string.tab_my_news);

        mFeedsManager = FeedsManager.getInstance();
        mFragments = new SparseArray<Fragment>(mFeedsManager.getFeedsCount() + 1);
        mFeedState = new HashMap<String, Integer>(mFeedsManager.getFeedsCount() + 1);

        EventBus.getDefault().register(this);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        mFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return BookmarksFragment.newInstance();
        }

        Feed feed = mFeedsManager.getFeed(position);
        if (feed == null) {
            return null;
        }

        Integer feedState = mFeedState.get(feed.getTitle());
        if (feedState == null) {
            mFeedState.put(feed.getTitle(), FEED_STATE_NEEDS_REFRESH);
        }

        return FeedFragment.newInstance(position, feed.getTitle(), feed.getUrl());
    }

    public void refreshFeed(int position) {
        Log.i(TAG, "refresh: ", position);

        if (position <= 0) {
            return;
        }

        FeedFragment f = (FeedFragment) getFragment(position);
        if (f == null) {
            Log.i(TAG, "refresh denied: ", position);
            return;
        }

        String feedTitle = f.getArguments().getString("feedId");

        Integer feedState = mFeedState.get(feedTitle);
        Log.i(TAG, "refresh: ", position, ": state ", feedState);

        if (feedState != null && feedState == FEED_STATE_NEEDS_REFRESH) {
            Log.i(TAG, "refresh: ", position, ": ", feedTitle);
            f.refresh();
            mFeedState.put(feedTitle, FEED_STATE_REFRESHED);
        }
    }

    @Override
    public int getItemPosition(Object object) {
        if (object instanceof BookmarksFragment) {
            if (mFeedsManager.getFeedsCount() < 2) {
                return POSITION_NONE; // the fragment will be re-created
            }
            return POSITION_UNCHANGED;
        }

        FeedFragment f = (FeedFragment) object;
        Bundle args = f.getArguments();
        int fPosition = args.getInt("position");
        String fTitle = args.getString("feedId");

        FeedsManager feedsManager = FeedsManager.getInstance();
        for (int k = 1; k < FeedsManager.MAX_FEEDS_COUNT + 1; ++k) {
            Feed feed = feedsManager.getFeed(k);

            if (feed != null && feed.getTitle().equalsIgnoreCase(fTitle)) {
                if (k == fPosition) {
                    return POSITION_UNCHANGED;
                }
            }
        }

        return POSITION_NONE; // the fragment will be re-created
    }

    public Fragment getFragment(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFeedsManager.getFeedsCount() + 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            return mFeedsManager.getFeedsCount() > 0 ? mTxtBookmarks : mTxtMyNews;
        }

        Feed feed = mFeedsManager.getFeed(position);
        if (feed == null) {
            return null;
        }

        return feed.getTitle().toUpperCase();
    }

    @Subscribe
    public void onEvent(FeedsEvent event) {
        Log.i(TAG, "FeedsEvent");

        notifyDataSetChanged();
        EventBus.getDefault().post(new FeedTitlesEvent(event.getType()));
    }
}
