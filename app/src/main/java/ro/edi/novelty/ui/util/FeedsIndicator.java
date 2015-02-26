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
import android.util.AttributeSet;
import de.greenrobot.event.EventBus;
import ro.edi.util.Log;
import ro.edi.util.ui.TitlePageIndicator;

public class FeedsIndicator extends TitlePageIndicator {
    private static final String TAG = "FEEDS.INDICATOR";

    public FeedsIndicator(Context context) {
        this(context, null);
    }

    public FeedsIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FeedsIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        EventBus.getDefault().register(this);
    }

    @SuppressWarnings("unused")
    public void onEvent(FeedTitlesEvent event) {
        Log.i(TAG, "FeedTitlesEvent");

        notifyDataSetChanged();

        switch (event.getType()) {
            case FeedsEvent.TYPE_ADD:
                setCurrentItem(getItemsCount() - 1);
                break;
            case FeedsEvent.TYPE_REMOVE:
                // TODO keep the current title (or go to the next/previous one, if it was removed)
                break;
            case FeedsEvent.TYPE_SWAP:
                // TODO keep the current title
                break;
            case FeedsEvent.TYPE_UPDATE:
                // do nothing... the feeds order/count is the same
                break;
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(FeedEvent event) {
        switch (event.getType()) {
            case FeedEvent.TYPE_SHOW_NEW:
                Log.i(TAG, "FeedEvent SHOW_NEW");
                showNotification(event.getPosition(), event.getCountNew());
                break;
            case FeedEvent.TYPE_UPDATE_NEW:
                if (event.getCountNew() < getNotificationCount(event.getPosition())) {
                    Log.i(TAG, "FeedEvent UPDATE_NEW");
                    showNotification(event.getPosition(), event.getCountNew());
                }
                break;
            default:
                break;
        }
    }
}
