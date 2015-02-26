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

import android.app.IntentService;
import android.content.Intent;
import android.text.format.DateUtils;
import ro.edi.novelty.data.DB;
import ro.edi.novelty.data.DbProvider;
import ro.edi.util.Log;

public class CleanupService extends IntentService {
    private static final String TAG = "CLEANUP.SERVICE";

    public CleanupService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "cleanup...");

        DbProvider.contentResolver.delete(DB.MyNews.URI,
                DB.MyNews.ID + " IN (SELECT " + DB.MyNews.ID + " FROM " + DB.News.TABLE_NAME + ',' + DB.MyNews.TABLE_NAME
                        + " WHERE (" + DB.News.ID + '=' + DB.MyNews.ID + " AND " + DB.News.FEED_ID + '=' + DB.MyNews.FEED_ID
                        + ") AND (" + DB.News.SAVED_DATE + "<?) AND " + DB.MyNews.IS_BOOKMARK + "<>1)",
                new String[]{String.valueOf(System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS)});

        DbProvider.contentResolver.delete(DB.News.URI, DB.News.ID + "||' '||" + DB.News.FEED_ID + " NOT IN (SELECT "
                + DB.MyNews.ID + "||' '||" + DB.MyNews.FEED_ID + " FROM " + DB.MyNews.TABLE_NAME + ')', null);
    }
}
