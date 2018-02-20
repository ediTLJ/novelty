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
package ro.edi.novelty.core;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import org.greenrobot.eventbus.EventBus;
import ro.edi.novelty.MyApp;
import ro.edi.novelty.data.Feed;
import ro.edi.novelty.data.Keys;
import ro.edi.novelty.ui.util.FeedsEvent;

import java.util.LinkedHashMap;

public class FeedsManager {
    // private static final String TAG = "FEEDS.MANAGER";

    public static final int MAX_FEEDS_COUNT = 20;

    private final SharedPreferences mPrefs;
    private LinkedHashMap<Integer, Feed> mFeeds = null; // feed 1, feed 2, etc.

    // private constructor prevents instantiation from other classes
    private FeedsManager() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(MyApp.getContext());
        mFeeds = new LinkedHashMap<Integer, Feed>(MAX_FEEDS_COUNT);

        for (int k = 1; k < MAX_FEEDS_COUNT + 1; ++k) {
            String feedData = mPrefs.getString(Keys.FEED_PREFIX + k, null);

            if (feedData != null) {
                int idx = feedData.indexOf(' ');

                Feed feed = new Feed();
                feed.setTitle(feedData.substring(idx + 1));
                feed.setUrl(feedData.substring(0, idx));

                mFeeds.put(k, feed);
            }
        }
    }

    /**
     * FeedsManagerHolder is loaded on the first execution of FeedsManager.getInstance()
     * or the first access to FeedsManagerHolder.INSTANCE, not before.
     */
    private static class FeedsManagerHolder {
        public static final FeedsManager INSTANCE = new FeedsManager();
    }

    /**
     * Creates a new instance of FeedsManager.
     */
    public static FeedsManager getInstance() {
        return FeedsManagerHolder.INSTANCE;
    }

    public Feed getFeed(int key) {
        if (key < 1 || key > MAX_FEEDS_COUNT) {
            return null;
        }

        return mFeeds.get(key);
    }

    public int getFeedsCount() {
        return mFeeds.size();
    }

    public boolean canAddFeed() {
        return mFeeds.size() < MAX_FEEDS_COUNT;
    }

    public void addFeed(String title, String url) {
        if (canAddFeed()) {
            int key = mFeeds.size() + 1;

            Feed feed = new Feed();
            feed.setTitle(title);
            feed.setUrl(url);
            mFeeds.put(key, feed);

            Editor editor = mPrefs.edit();
            editor.putString(Keys.FEED_PREFIX + key, url + ' ' + title);
            editor.apply();

            EventBus.getDefault().post(new FeedsEvent(FeedsEvent.TYPE_ADD));
        }
    }

    public void removeFeed(int key) {
        if (key < 1 || key > MAX_FEEDS_COUNT) {
            return;
        }

        Editor editor = mPrefs.edit();
        for (int k = key; k < MAX_FEEDS_COUNT + 1; ++k) {
            Feed feed = mFeeds.get(k + 1);
            if (feed == null) { // last key
                mFeeds.remove(k);
                editor.remove(Keys.FEED_PREFIX + k);
            } else {
                mFeeds.put(k, feed);
                editor.putString(Keys.FEED_PREFIX + k, feed.getUrl() + ' ' + feed.getTitle());
            }
        }
        editor.apply();

        EventBus.getDefault().post(new FeedsEvent(FeedsEvent.TYPE_REMOVE));
    }

    public void swapFeeds(int key1, int key2) {
        if (key1 < 1 || key2 < 1 || key1 > MAX_FEEDS_COUNT || key2 > MAX_FEEDS_COUNT) {
            return;
        }

        Feed feed1 = mFeeds.get(key1);
        Feed feed2 = mFeeds.get(key2);

        mFeeds.put(key1, feed2);
        mFeeds.put(key2, feed1);

        Editor editor = mPrefs.edit();
        editor.putString(Keys.FEED_PREFIX + key1, feed2.getUrl() + ' ' + feed2.getTitle());
        editor.putString(Keys.FEED_PREFIX + key2, feed1.getUrl() + ' ' + feed1.getTitle());
        editor.apply();

        EventBus.getDefault().post(new FeedsEvent(FeedsEvent.TYPE_SWAP));
    }

    /**
     * Titles cannot be changed, they are final.
     * This method is for updating the URL only.
     */
    public void updateFeed(String title, String url, boolean notify) {
        Editor editor = mPrefs.edit();
        for (int k = 1; k < MAX_FEEDS_COUNT + 1; ++k) {
            Feed feed = mFeeds.get(k);
            if (feed != null && feed.getTitle().equalsIgnoreCase(title)) {
                feed.setUrl(url);
                mFeeds.put(k, feed);
                editor.putString(Keys.FEED_PREFIX + k, feed.getUrl() + ' ' + feed.getTitle());
            }
        }
        editor.apply();

        if (notify) {
            EventBus.getDefault().post(new FeedsEvent(FeedsEvent.TYPE_UPDATE));
        }
    }
}
