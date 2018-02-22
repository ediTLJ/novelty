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
package ro.edi.novelty.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for DbProvider.
 */
public final class DB {
    static final String AUTHORITY = "ro.edi.novelty";

    // this class cannot be instantiated
    private DB() {

    }

    /**
     * News table.
     */
    public static final class News implements BaseColumns {
        public static final String TABLE_NAME = "news";
        public static final Uri URI = Uri.parse("content://" + AUTHORITY + '/' + TABLE_NAME);
        static final String TYPE = "vnd.android.cursor.dir/vnd.edi.novelty." + TABLE_NAME;

        public static final String ID = TABLE_NAME + "_id"; // TEXT
        public static final String AUTHOR = TABLE_NAME + "_author"; // TEXT
        public static final String TITLE = TABLE_NAME + "_title"; // TEXT
        public static final String PUBLISHED_DATE = TABLE_NAME + "_published_date"; // INTEGER
        public static final String FEED_ID = TABLE_NAME + "_feed_id"; // TEXT
        // public static final String CONTENT_TYPES = TABLE_NAME + "_content_types"; // TEXT
        public static final String CONTENT_VALUES = TABLE_NAME + "_content_values"; // TEXT
        public static final String URL = TABLE_NAME + "_url"; // TEXT
        public static final String SAVED_DATE = TABLE_NAME + "_saved_date"; // INTEGER
    }

    /**
     * MyNews table.
     */
    public static final class MyNews implements BaseColumns {
        public static final String TABLE_NAME = "my_news";
        public static final Uri URI = Uri.parse("content://" + AUTHORITY + '/' + TABLE_NAME);
        static final String TYPE = "vnd.android.cursor.dir/vnd.edi.novelty." + TABLE_NAME;

        public static final String ID = TABLE_NAME + "_id"; // TEXT
        public static final String FEED_ID = TABLE_NAME + "_feed_id"; // TEXT
        public static final String IS_READ = TABLE_NAME + "_is_read"; // INTEGER
        public static final String IS_BOOKMARK = TABLE_NAME + "_is_starred"; // INTEGER
    }
}
