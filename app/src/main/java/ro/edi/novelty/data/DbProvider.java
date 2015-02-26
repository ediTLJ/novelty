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

import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

import ro.edi.novelty.data.DB.MyNews;
import ro.edi.novelty.data.DB.News;
import ro.edi.util.InsertHelper;
import ro.edi.util.Log;

public class DbProvider extends ContentProvider {
    public static ContentResolver contentResolver;

    private static final String TAG = "DB.PROVIDER";
    public static final String DB_NAME = "novelty.db";
    private static final int DB_VERSION = 1;

    private static final int NEWS = 0; // all the news
    private static final int MY_NEWS = 1; // my news

    private static final String MY_NEWS_TABLES = DB.MyNews.TABLE_NAME + ',' + DB.News.TABLE_NAME;
    private static final String MY_NEWS_WHERE = DB.MyNews.ID + '=' + DB.News.ID + " AND " + DB.MyNews.FEED_ID + '='
            + DB.News.FEED_ID; // join ON

    private static final UriMatcher sURIMatcher = buildUriMatcher();

    private static DbHelper mOpenHelper;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DbHelper extends SQLiteOpenHelper {
        DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // basic stuff:
            // - Android automatically creates indexes for primary keys
            // - Android 3.0+ automatically creates indexes for unique columns
            // - indexes are recommended for columns used for ordering, in WHERE statements or for joining tables
            // - adding indexes improves query speed, but decreases insert speed

            // create News table
            StringBuilder sbNews = new StringBuilder("CREATE TABLE ");
            sbNews.append(News.TABLE_NAME);
            sbNews.append(" (");
            sbNews.append(News._ID);
            sbNews.append(" INTEGER PRIMARY KEY,");
            sbNews.append(News.ID);
            sbNews.append(" TEXT,");
            sbNews.append(News.AUTHOR);
            sbNews.append(" TEXT,");
            sbNews.append(News.TITLE);
            sbNews.append(" TEXT,");
            sbNews.append(News.PUBLISHED_DATE);
            sbNews.append(" INTEGER,");
            sbNews.append(News.FEED_ID);
            sbNews.append(" TEXT,");
            sbNews.append(News.CONTENT_VALUES);
            sbNews.append(" TEXT,");
            sbNews.append(News.URL);
            sbNews.append(" TEXT,");
            sbNews.append(News.SAVED_DATE);
            sbNews.append(" INTEGER");
            sbNews.append(", UNIQUE (");
            sbNews.append(News.ID);
            sbNews.append(", ");
            sbNews.append(News.FEED_ID);
            sbNews.append(')');
            sbNews.append(");");

            // index
            sbNews.append(" CREATE INDEX IF NOT EXISTS idx_id ON ");
            sbNews.append(News.TABLE_NAME);
            sbNews.append(" (");
            sbNews.append(News.ID);
            sbNews.append(");");

            sbNews.append(" CREATE INDEX IF NOT EXISTS idx_pub_date ON ");
            sbNews.append(News.TABLE_NAME);
            sbNews.append(" (");
            sbNews.append(News.PUBLISHED_DATE);
            sbNews.append(");");

            sbNews.append(" CREATE INDEX IF NOT EXISTS idx_feed_id ON ");
            sbNews.append(News.TABLE_NAME);
            sbNews.append(" (");
            sbNews.append(News.FEED_ID);
            sbNews.append(");");

            // create MyNews table
            StringBuilder sbMyNews = new StringBuilder("CREATE TABLE ");
            sbMyNews.append(MyNews.TABLE_NAME);
            sbMyNews.append(" (");
            sbMyNews.append(MyNews._ID);
            sbMyNews.append(" INTEGER PRIMARY KEY,");
            sbMyNews.append(MyNews.ID);
            sbMyNews.append(" TEXT,");
            sbMyNews.append(MyNews.FEED_ID);
            sbMyNews.append(" TEXT,");
            sbMyNews.append(MyNews.IS_READ);
            sbMyNews.append(" INTEGER,");
            sbMyNews.append(MyNews.IS_BOOKMARK);
            sbMyNews.append(" INTEGER");
            sbMyNews.append(", UNIQUE (");
            sbMyNews.append(MyNews.ID);
            sbMyNews.append(", ");
            sbMyNews.append(MyNews.FEED_ID);
            sbMyNews.append(')');
            sbMyNews.append(");");

            // index
            sbMyNews.append(" CREATE INDEX IF NOT EXISTS idx_id ON ");
            sbMyNews.append(MyNews.TABLE_NAME);
            sbMyNews.append(" (");
            sbMyNews.append(MyNews.ID);
            sbMyNews.append(");");

            sbMyNews.append(" CREATE INDEX IF NOT EXISTS idx_feed_id ON ");
            sbMyNews.append(MyNews.TABLE_NAME);
            sbMyNews.append(" (");
            sbMyNews.append(MyNews.FEED_ID);
            sbMyNews.append(");");

            sbMyNews.append(" CREATE INDEX IF NOT EXISTS idx_is_read ON ");
            sbMyNews.append(MyNews.TABLE_NAME);
            sbMyNews.append(" (");
            sbMyNews.append(MyNews.IS_READ);
            sbMyNews.append(");");

            sbMyNews.append(" CREATE INDEX IF NOT EXISTS idx_is_starred ON ");
            sbMyNews.append(MyNews.TABLE_NAME);
            sbMyNews.append(" (");
            sbMyNews.append(MyNews.IS_BOOKMARK);
            sbMyNews.append(");");

            db.beginTransaction();
            try {
                db.execSQL(sbNews.toString());
                db.execSQL(sbMyNews.toString());

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            switch (oldVersion) {
                case 1:
                    break;
                default:
                    break;
            }
        }

        @Override
        public synchronized SQLiteDatabase getWritableDatabase() {
            return super.getWritableDatabase();
        }

        @Override
        public synchronized SQLiteDatabase getReadableDatabase() {
            return super.getReadableDatabase();
        }

        @Override
        public synchronized void close() {
            super.close();
        }
    }

    /**
     * Sets up a URI matcher for shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(DB.AUTHORITY, News.TABLE_NAME, NEWS);
        matcher.addURI(DB.AUTHORITY, MyNews.TABLE_NAME, MY_NEWS);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        Context ctx = getContext();
        contentResolver = ctx.getContentResolver();
        mOpenHelper = new DbHelper(ctx);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sURIMatcher.match(uri)) {
            case NEWS:
                qb.setTables(News.TABLE_NAME);
                break;
            case MY_NEWS:
                qb.setTables(MY_NEWS_TABLES);
                qb.appendWhere(MY_NEWS_WHERE);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // get the database and run the query
        Cursor c = qb.query(mOpenHelper.getWritableDatabase(), projection, selection, selectionArgs, null, null,
                sortOrder);

        // tell the cursor what URI to watch, so it knows when its source data changes
        c.setNotificationUri(contentResolver, uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] values) {
        int rowCount = values.length;

        if (rowCount == 0) {
            return 0;
        }

        long t = System.currentTimeMillis();
        int okCount = 0;
        SQLiteDatabase dbWritable = mOpenHelper.getWritableDatabase();
        String tableName = getTableName(uri);

        // create a single InsertHelper to handle this set of insertions
        InsertHelper ih = new InsertHelper(dbWritable, tableName);
        dbWritable.beginTransaction();

        try {
            for (ContentValues value : values) {
                long rowId = ih.insert(value);
                okCount += rowId < 0 ? 0 : 1;

                // try
                // {
                // long rowId = dbWritable.insertOrThrow(tableName, BaseColumns._ID, values[i]);
                // okCount += rowId < 0 ? 0 : 1;
                // }
                // catch (Exception e)
                // {
                // // ignore...
                // // Log.i(TAG, "Exception (replace): " + e.getMessage());
                // }
            }

            dbWritable.setTransactionSuccessful();
        } finally {
            ih.close();
            dbWritable.endTransaction();
        }

        Log.i(TAG, "INSERT table: ", tableName);
        Log.i(TAG, "INSERT ok: ", okCount);
        Log.i(TAG, "INSERT failed: ", rowCount - okCount);

        Log.i("TIME", "INSERT table: ", tableName);
        Log.i("TIME", "INSERT time (ms): ", System.currentTimeMillis() - t);

        return okCount;
    }

    public static int bulkReplace(Uri uri, ContentValues[] values) {
        int rowCount = values.length;

        if (rowCount == 0) {
            return 0;
        }

        Log.i(TAG, "bulk replace");

        long t = System.currentTimeMillis();
        int okCount = 0;
        SQLiteDatabase dbWritable = mOpenHelper.getWritableDatabase();
        String tableName = getTableName(uri);

        // create a single InsertHelper to handle this set of insertions
        InsertHelper ih = new InsertHelper(dbWritable, tableName);
        dbWritable.beginTransaction();
        Log.i(TAG, "begin transaction");

        try {
            for (ContentValues value : values) {
                long rowId = ih.replace(value);
                okCount += rowId < 0 ? 0 : 1;

                // try
                // {
                // long rowId = dbWritable.replaceOrThrow(tableName, BaseColumns._ID, values[i]);
                // okCount += rowId < 0 ? 0 : 1;
                // }
                // catch (Exception e)
                // {
                // // ignore...
                // Log.i(TAG, "Exception (replace): " + e.getMessage());
                // }
            }

            Log.i(TAG, "done all replaces");
            dbWritable.setTransactionSuccessful();
            Log.i(TAG, "set transaction successful");
        } finally {
            ih.close();
            Log.i(TAG, "ih closed");
            dbWritable.endTransaction();
            Log.i(TAG, "ended transaction");
        }

        Log.i(TAG, "REPLACE table: ", tableName);
        Log.i(TAG, "REPLACE ok: ", okCount);
        Log.i(TAG, "REPLACE failed: ", rowCount - okCount);

        Log.i("TIME", "REPLACE table: ", tableName);
        Log.i("TIME", "REPLACE time (ms): ", System.currentTimeMillis() - t);

        return okCount;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase dbWritable = mOpenHelper.getWritableDatabase();

        dbWritable.beginTransaction();

        int count = dbWritable.update(getTableName(uri), values, selection, selectionArgs);

        dbWritable.setTransactionSuccessful();
        dbWritable.endTransaction();

        // Log.i(TAG, "Updated rows: " + count);
        if (count > 0) {
            BackupManager.dataChanged(getContext().getPackageName());
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase dbWritable = mOpenHelper.getWritableDatabase();

        int count = dbWritable.delete(getTableName(uri), selection, selectionArgs);
        // contentResolver.notifyChange(uri, null, false);

        Log.i(TAG, "deleted ", count, " rows!");

        return count;
    }

    /**
     * Get query type for this provider...
     */
    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case NEWS:
                return News.TYPE;
            case MY_NEWS:
                return MyNews.TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    private static String getTableName(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case NEWS:
                return News.TABLE_NAME;
            case MY_NEWS:
                return MyNews.TABLE_NAME;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
}
