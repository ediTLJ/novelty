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
package ro.edi.novelty.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.TypedValue;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import ro.edi.novelty.R;
import ro.edi.novelty.data.DB;
import ro.edi.novelty.data.DbProvider;
import ro.edi.novelty.core.FeedsManager;
import ro.edi.novelty.ui.util.LoaderIds;
import ro.edi.util.ui.AltCursorAdapter;
import ro.edi.util.ui.AsyncLoader;
import ro.edi.util.ui.LoaderPayload;

public class BookmarksFragment extends ListFragment {
    private AltCursorAdapter mAdapter;

    public BookmarksFragment() {

    }

    public static BookmarksFragment newInstance() {
        return new BookmarksFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new AltCursorAdapter(getActivity(), new int[]{R.layout.news_item}, null,
                new String[]{DB.MyNews.FEED_ID, DB.News.TITLE, DB.News.PUBLISHED_DATE},
                new int[]{R.id.news_feed, R.id.news_title, R.id.news_date});

        mAdapter.setViewBinder(new BinderNews(getActivity()));
        setListAdapter(mAdapter);

        initUI();

        // on screen orientation changes, the loaders will return the last result,
        // without doing the background work again
        LoaderManager lm = getLoaderManager();
        lm.initLoader(LoaderIds.CURSOR_GET_STARRED, null, cursorCallbacks);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Cursor c = mAdapter.getCursor();

        Intent iNewsInfo = new Intent(getActivity(), NewsInfoActivity.class);
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_ID, c.getString(c.getColumnIndex(DB.MyNews.ID)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_FEED_ID, c.getString(c.getColumnIndex(DB.MyNews.FEED_ID)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_AUTHOR, c.getString(c.getColumnIndex(DB.News.AUTHOR)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_TITLE, c.getString(c.getColumnIndex(DB.News.TITLE)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_DATE, c.getLong(c.getColumnIndex(DB.News.PUBLISHED_DATE)));
        iNewsInfo
                .putExtra(NewsInfoActivity.EXTRA_CONTENT_VALUES, c.getString(c.getColumnIndex(DB.News.CONTENT_VALUES)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_URL, c.getString(c.getColumnIndex(DB.News.URL)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_IS_BOOKMARK, c.getInt(c.getColumnIndex(DB.MyNews.IS_BOOKMARK)) > 0);
        startActivity(iNewsInfo);

        if (c.getInt(c.getColumnIndex(DB.MyNews.IS_READ)) == 0) {
            LoaderManager lm = getLoaderManager();
            Bundle b = new Bundle();
            b.putString("newsId", c.getString(c.getColumnIndex(DB.MyNews.ID)));
            b.putString("feedId", c.getString(c.getColumnIndex(DB.MyNews.FEED_ID)));
            lm.restartLoader(LoaderIds.ASYNC_UPDATE_READ, b, loaderCallbacks);
        }
    }

    private void initUI() {
        // start out with a progress indicator
        setListShownNoAnimation(false);

        ListView listView = getListView();
        listView.setVelocityScale(2.0f);

        FeedsManager feedsManager = FeedsManager.getInstance();

        setEmptyText(getText(feedsManager.getFeedsCount() > 0 ? R.string.empty_bookmarks
                : R.string.add_feed_info));

        TextView tvEmpty = (TextView) listView.getEmptyView();
        tvEmpty.setTextAppearance(getActivity(), R.style.TextAppearance_AppCompat_Title);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        tvEmpty.setTextColor(getResources().getColor(typedValue.resourceId));

        if (feedsManager.getFeedsCount() == 0) {
            tvEmpty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent iAdd = new Intent(getActivity(), AddFeedActivity.class);
                    getActivity().startActivityForResult(iAdd, 101);
                }
            });
        }
    }

    private final LoaderCallbacks<Cursor> cursorCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle data) {
            switch (id) {
                case LoaderIds.CURSOR_GET_STARRED:
                    String[] projection = {DB.MyNews.TABLE_NAME + '.' + DB.MyNews._ID, DB.MyNews.ID, DB.News.AUTHOR,
                            DB.News.TITLE, DB.News.PUBLISHED_DATE, DB.News.CONTENT_VALUES,
                            DB.News.URL, DB.MyNews.IS_READ, DB.MyNews.IS_BOOKMARK, DB.MyNews.FEED_ID};
                    String selection = DB.MyNews.IS_BOOKMARK + "='1'";
                    String sortOrder = DB.News.PUBLISHED_DATE + " DESC";

                    return new CursorLoader(getActivity(), DB.MyNews.URI, projection, selection, null,
                            sortOrder);
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            switch (loader.getId()) {
                case LoaderIds.CURSOR_GET_STARRED:
                    mAdapter.swapCursor(c);
                    // getListView().invalidate();

                    setListShownNoAnimation(true);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }
    };

    private final LoaderManager.LoaderCallbacks<LoaderPayload> loaderCallbacks = new LoaderCallbacks<LoaderPayload>() {
        @Override
        public Loader<LoaderPayload> onCreateLoader(int id, final Bundle data) {
            switch (id) {
                case LoaderIds.ASYNC_UPDATE_READ:
                    return new AsyncLoader<LoaderPayload>(getActivity().getApplication()) {
                        @Override
                        public LoaderPayload loadInBackground() {
                            String newsId = data.getString("newsId");
                            String feedId = data.getString("feedId");

                            ContentValues v = new ContentValues(2);
                            v.put(DB.MyNews.IS_READ, 1);

                            if (DbProvider.contentResolver.update(DB.MyNews.URI, v, DB.MyNews.ID + "=? AND "
                                    + DB.MyNews.FEED_ID + "=?", new String[]{newsId, feedId}) > 0) {
                                DbProvider.contentResolver.notifyChange(DB.MyNews.URI, null, false);
                                return new LoaderPayload(LoaderPayload.STATUS_OK);
                            }

                            return new LoaderPayload(LoaderPayload.STATUS_ERROR);
                        }
                    };
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
            switch (loader.getId()) {
                case LoaderIds.ASYNC_UPDATE_READ:
                    if (payload.getStatus() == LoaderPayload.STATUS_OK) {
                        LoaderManager lm = getLoaderManager();
                        lm.restartLoader(LoaderIds.CURSOR_GET_STARRED, null, cursorCallbacks);
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<LoaderPayload> loader) {

        }
    };
}
