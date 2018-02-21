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

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.escape.synder.DefaultContext;
import com.escape.synder.DefaultParseContext;
import com.github.mrengineer13.snackbar.SnackBar;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndLink;

import org.greenrobot.eventbus.EventBus;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import ro.edi.novelty.R;
import ro.edi.novelty.core.FeedsManager;
import ro.edi.novelty.data.DB;
import ro.edi.novelty.data.DbProvider;
import ro.edi.novelty.ui.util.FeedEvent;
import ro.edi.novelty.ui.util.LoaderIds;
import ro.edi.util.Entities;
import ro.edi.util.Log;
import ro.edi.util.ui.AltCursorAdapter;
import ro.edi.util.ui.AsyncLoader;
import ro.edi.util.ui.LoaderPayload;

public class FeedFragment extends ListFragment implements AbsListView.OnScrollListener {
    private static final String TAG = "FEED.FRAGMENT";

    private static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    private static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    private static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    private static final Pattern PATTERN_TAG_IMG = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>([^<]*</img>)*");
    private static final Pattern PATTERN_EMPTY_TAGS = Pattern.compile("<[^>]*>\\s*</[^>]*>");
    private static final Pattern PATTERN_TAG_BR = Pattern.compile("<br\\s*/?>");

    private AltCursorAdapter mAdapter;

    public FeedFragment() {

    }

    public static FeedFragment newInstance(int position, String feedId, String feedUrl) {
        FeedFragment f = new FeedFragment();

        Bundle b = new Bundle();
        b.putInt("position", position); // FIXME constants & better names
        b.putString("feedId", feedId);
        b.putString("feedUrl", feedUrl);

        f.setArguments(b);
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new AltCursorAdapter(getActivity(), new int[]{R.layout.news_item}, null, new String[]{
                DB.News.TITLE, DB.News.PUBLISHED_DATE}, new int[]{R.id.news_title, R.id.news_date});

        mAdapter.setViewBinder(new BinderNews(getActivity()));
        setListAdapter(mAdapter);

        initUI();

        // on screen orientation changes, the loaders will return the last result,
        // without doing the background work again
        LoaderManager lm = getLoaderManager();
        if (getArguments() != null) {
            lm.initLoader(LoaderIds.CURSOR_GET_FEED + getArguments().getInt("position"), null, cursorCallbacks);
        }
    }

    @SuppressWarnings("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.base_list, null);
        v.findViewById(android.R.id.empty).setId(INTERNAL_EMPTY_ID);
        v.findViewById(R.id.progress_container).setId(INTERNAL_PROGRESS_CONTAINER_ID);
        v.findViewById(R.id.list_container).setId(INTERNAL_LIST_CONTAINER_ID);
        return v;
    }

    private void initUI() {
        if (getActivity() == null || getView() == null) {
            return;
        }

        // start out with a progress indicator
        setListShownNoAnimation(false);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();

        // noinspection ResourceType
        SwipeRefreshLayout swipeLayout = getView().findViewById(INTERNAL_LIST_CONTAINER_ID);

        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        swipeLayout.setColorSchemeResources(typedValue.resourceId);

        swipeLayout.setOnRefreshListener(() -> {
            LoaderManager lm = getLoaderManager();
            if (getArguments() != null) {
                lm.restartLoader(LoaderIds.ASYNC_GET_FEED + getArguments().getInt("position"), null, loaderCallbacks);
            }
        });

        ListView listView = getListView();
        listView.setVelocityScale(2.0f);

        setEmptyText(getText(R.string.empty_news));

        TextView tvEmpty = (TextView) listView.getEmptyView();
        tvEmpty.setTextAppearance(getActivity(), R.style.TextAppearance_AppCompat_Title);

        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        tvEmpty.setTextColor(getResources().getColor(typedValue.resourceId));

        listView.setOnScrollListener(this);

        // listView().setOnCreateContextMenuListener(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Cursor c = mAdapter.getCursor();

        Intent iNewsInfo = new Intent(getActivity(), NewsInfoActivity.class);
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_ID, c.getString(c.getColumnIndex(DB.MyNews.ID)));
        if (getArguments() != null) {
            iNewsInfo.putExtra(NewsInfoActivity.EXTRA_FEED_ID, getArguments().getString("feedId"));
        }
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_AUTHOR, c.getString(c.getColumnIndex(DB.News.AUTHOR)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_TITLE, c.getString(c.getColumnIndex(DB.News.TITLE)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_DATE, c.getLong(c.getColumnIndex(DB.News.PUBLISHED_DATE)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_CONTENT_VALUES,
                c.getString(c.getColumnIndex(DB.News.CONTENT_VALUES)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_URL, c.getString(c.getColumnIndex(DB.News.URL)));
        iNewsInfo.putExtra(NewsInfoActivity.EXTRA_IS_BOOKMARK, c.getInt(c.getColumnIndex(DB.MyNews.IS_BOOKMARK)) > 0);
        startActivity(iNewsInfo);

        if (c.getInt(c.getColumnIndex(DB.MyNews.IS_READ)) == 0) {
            LoaderManager lm = getLoaderManager();
            Bundle b = new Bundle();
            b.putString("newsId", c.getString(c.getColumnIndex(DB.MyNews.ID)));
            lm.restartLoader(LoaderIds.ASYNC_UPDATE_READ, b, loaderCallbacks);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount > 0 && getArguments() != null) {
            int pos = getArguments().getInt("position");
            if (pos > 0) {
                EventBus.getDefault().post(new FeedEvent(FeedEvent.TYPE_UPDATE_NEW, pos, firstVisibleItem));
                // FIXME optimize: send event when scrolling up only
            }
        }
    }

    public void refresh() {
        if (getArguments() != null) {
            Log.i(TAG, "refresh: ", getArguments().getString("feedId"));
        }

        if (getView() == null) {
            Log.i(TAG, "refresh denied: ", getArguments().getString("feedId"));
            return;
        }

        // noinspection ResourceType
        SwipeRefreshLayout swipeLayout = getView().findViewById(INTERNAL_LIST_CONTAINER_ID);
        swipeLayout.setRefreshing(true);

        LoaderManager lm = getLoaderManager();
        lm.restartLoader(LoaderIds.ASYNC_GET_FEED + getArguments().getInt("position"), null, loaderCallbacks);
    }

    private void hideLoading() {
        // the list should now be shown
        setListShownNoAnimation(true);

        if (getView() != null) {
            // noinspection ResourceType
            SwipeRefreshLayout swipeLayout = getView().findViewById(INTERNAL_LIST_CONTAINER_ID);
            swipeLayout.setRefreshing(false);
        }
    }

    private final LoaderCallbacks<Cursor> cursorCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle data) {
            if (getArguments() != null && id == LoaderIds.CURSOR_GET_FEED + getArguments().getInt("position")) {
                String[] projection = {DB.MyNews.TABLE_NAME + '.' + DB.MyNews._ID, DB.MyNews.ID, DB.News.AUTHOR,
                        DB.News.TITLE, DB.News.PUBLISHED_DATE, DB.News.CONTENT_VALUES,
                        DB.News.URL, DB.MyNews.IS_READ, DB.MyNews.IS_BOOKMARK};
                String selection = DB.MyNews.FEED_ID + "=? AND " + DB.News.SAVED_DATE + ">=?";
                String[] selectionArgs = {getArguments().getString("feedId"),
                        String.valueOf(System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS)};
                String sortOrder = DB.News.PUBLISHED_DATE + " DESC";

                if (getActivity() == null) {
                    return null;
                }
                return new CursorLoader(getActivity(), DB.MyNews.URI, projection, selection, selectionArgs, sortOrder);
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            if (getArguments() == null) {
                return;
            }

            int pos = getArguments().getInt("position");

            if (loader.getId() == LoaderIds.CURSOR_GET_FEED + pos) {
                int count = mAdapter.getCount();

                if (count > 0) {
                    ListView listView = getListView();

                    // save listview state
                    Parcelable state = listView.onSaveInstanceState();

                    mAdapter.swapCursor(c);

                    // restore previous state (including selected item index and scroll position)
                    listView.onRestoreInstanceState(state);

                    setListShownNoAnimation(true);

                    int countNew = mAdapter.getCount() - count;
                    if (pos > 0 && countNew > 0) {
                        EventBus.getDefault().post(new FeedEvent(FeedEvent.TYPE_SHOW_NEW, pos, countNew));
                    }
                } else {
                    mAdapter.swapCursor(c);
                    setListShownNoAnimation(true);
                }

                LoaderManager lm = getLoaderManager();

                // noinspection StatementWithEmptyBody
                if (lm.getLoader(LoaderIds.ASYNC_GET_FEED + pos) == null) // first query
                {
//                    if (pos == 1 && FeedsPagerAdapter.firstTime.get(1)) {
//                        FeedsPagerAdapter.firstTime.put(1, false);
//                        refresh();
//                    } else {
//                        hideLoading();
//                    }
                } else {
                    hideLoading();
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }
    };

    private final LoaderManager.LoaderCallbacks<LoaderPayload> loaderCallbacks = new LoaderCallbacks<LoaderPayload>() {
        @Override
        public Loader<LoaderPayload> onCreateLoader(int id, Bundle data) {
            if (getArguments() == null) {
                return null;
            }

            int pos = getArguments().getInt("position");

            if (id == LoaderIds.ASYNC_GET_FEED + pos) {
                return new GetFeedLoader(getActivity(), FeedFragment.this);
            }

            if (id == LoaderIds.ASYNC_UPDATE_READ) {
                return new UpdateReadLoader(getActivity(), data, getArguments());
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<LoaderPayload> loader, LoaderPayload payload) {
            if (getArguments() == null) {
                return;
            }

            int id = loader.getId();
            int pos = getArguments().getInt("position");

            if (id == LoaderIds.ASYNC_GET_FEED + pos) {
                if (payload.getStatus() == LoaderPayload.STATUS_OK) {
                    getLoaderManager().restartLoader(LoaderIds.CURSOR_GET_FEED + pos, null, cursorCallbacks);
                } else {
                    // FIXME empty view not shown
                    hideLoading();
                    if (getActivity() != null && getView() != null) {
                        new SnackBar.Builder(getActivity().getApplicationContext(), getView())
                                .withMessageId(R.string.error_news)
                                .show();
                    }
                }
            } else if (id == LoaderIds.ASYNC_UPDATE_READ) {
                if (payload.getStatus() == LoaderPayload.STATUS_OK) {
                    getLoaderManager().restartLoader(LoaderIds.CURSOR_GET_FEED + pos, null, cursorCallbacks);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<LoaderPayload> loader) {

        }
    };

    private static class GetFeedLoader extends AsyncLoader<LoaderPayload> {
        private WeakReference<FragmentActivity> activityRef;
        private WeakReference<FeedFragment> fragRef;

        // only retain a weak reference to the activity
        GetFeedLoader(FragmentActivity activity, FeedFragment fragment) {
            super(activity);
            activityRef = new WeakReference<>(activity);
            fragRef = new WeakReference<>(fragment);
        }

        @Override
        public LoaderPayload loadInBackground() {
            FragmentActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing()) {
                return null;
            }

            FeedFragment f = fragRef.get();
            if (f == null || f.isRemoving()) {
                return null;
            }

            String url = f.getArguments().getString("feedUrl");
            HttpURLConnection conn = null;
            InputStream inStream = null;
            BufferedReader reader = null;
            SyndFeed sf = null;

            try {
                conn = setupConnection(url);
                String contentType = conn.getContentType();

                if (contentType != null && contentType.startsWith("text/html")) {
                    String encoding = conn.getContentEncoding();
                    if ("gzip".equals(encoding) || "x-gzip".equals(encoding)) {
                        reader = new BufferedReader(new InputStreamReader(
                                new GZIPInputStream(conn.getInputStream()), getCharset(conn)));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(
                                conn.getInputStream(), getCharset(conn)));
                    }

                    String line;
                    boolean isBody = false;
                    int pos;
                    int posStart = -1;

                    while ((line = reader.readLine()) != null) {
                        if (line.contains("<body") || line.contains("<BODY")) {
                            // body starts... stop looking for alt links
                            isBody = true;
                            break;
                        }

                        pos = line.indexOf("<link ");
                        if (pos < 0) { // no link on this line, keep looking
                            pos = line.indexOf("<LINK ");
                            if (pos < 0) { // no link on this line, keep looking
                                continue;
                            }
                        }

                        // if link type is not alternate, keep looking
                        if (line.indexOf(" rel=\"alternate\"", pos) < 0) {
                            if (line.indexOf(" rel='alternate'", pos) < 0) {
                                if (line.indexOf(" rel=alternate", pos) < 0) {
                                    if (line.indexOf(" REL=\"alternate\"", pos) < 0) {
                                        if (line.indexOf(" REL='alternate'", pos) < 0) {
                                            if (line.indexOf(" REL=alternate", pos) < 0) {
                                                continue;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        int quote = '\"';
                        posStart = line.indexOf("href=\"", pos);
                        if (posStart < 0) {
                            posStart = line.indexOf("HREF=\"", pos);
                            if (posStart < 0) {
                                quote = '\'';
                                posStart = line.indexOf("href='", pos);
                                if (posStart < 0) {
                                    posStart = line.indexOf("HREF='", pos);
                                    if (posStart < 0) {
                                        continue;
                                    }
                                }
                            }
                        }

                        String link = line.substring(posStart + 6, line.indexOf(quote, posStart + 10))
                                .replace("&amp;", "&");

                        if (link.startsWith("/")) {
                            link = url + link;
                        } else if (!link.startsWith("http://") && !link.startsWith("https://")
                                && !link.startsWith("HTTP://") && !link.startsWith("HTTPS://")) {
                            link = url + '/' + link;
                        }

                        f.getArguments().putString("feedUrl", link);

                        FeedsManager feedsManager = FeedsManager.getInstance();
                        feedsManager.updateFeed(f.getArguments().getString("feedId"), link, false);

                        reader.close();
                        conn.disconnect();
                        conn = setupConnection(link);
                        // contentType = conn.getContentType();

                        break;
                    }

                    if (isBody) {
                        while ((line = reader.readLine()) != null) {
                            pos = line.indexOf("<a ");
                            if (pos < 0) { // no link on this line, keep looking
                                pos = line.indexOf("<A ");
                                if (pos < 0) { // no link on this line, keep looking
                                    continue;
                                }
                            }

                            // if link class is not rss, keep looking
                            if (line.indexOf(" class=\"rss\"", pos) < 0) {
                                if (line.indexOf(" class='rss'", pos) < 0) {
                                    if (line.indexOf(" class=rss", pos) < 0) {
                                        if (line.indexOf(" CLASS=\"rss\"", pos) < 0) {
                                            if (line.indexOf(" CLASS='rss'", pos) < 0) {
                                                if (line.indexOf(" CLASS=rss", pos) < 0) {
                                                    continue;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            int quote = '\"';
                            posStart = line.indexOf("href=\"", pos);
                            if (posStart < 0) {
                                posStart = line.indexOf("HREF=\"", pos);
                                if (posStart < 0) {
                                    quote = '\'';
                                    posStart = line.indexOf("href='", pos);
                                    if (posStart < 0) {
                                        posStart = line.indexOf("HREF='", pos);
                                        if (posStart < 0) {
                                            continue;
                                        }
                                    }
                                }
                            }

                            String link = line.substring(posStart + 6, line.indexOf(quote, posStart + 10))
                                    .replace("&amp;", "&");

                            if (link.startsWith("/")) {
                                link = url + link;
                            } else if (!link.startsWith("http://") && !link.startsWith("https://")
                                    && !link.startsWith("HTTP://") && !link.startsWith("HTTPS://")) {
                                link = url + '/' + link;
                            }

                            if (link.equals(f.getArguments().getString("feedUrl"))) {
                                posStart = -1;
                                continue;
                            }

                            f.getArguments().putString("feedUrl", link);

                            FeedsManager feedsManager = FeedsManager.getInstance();
                            feedsManager.updateFeed(f.getArguments().getString("feedId"), link, false);

                            reader.close();
                            conn.disconnect();
                            conn = setupConnection(link);
                            // contentType = conn.getContentType();

                            break;
                        }
                    }

                    if (posStart < 0) { // this indicates a wrong configured feed
                        reader.close();
                        conn.disconnect();
                        conn = setupConnection(url);
                        // contentType = conn.getContentType();
                    }
                }

                String encoding = conn.getContentEncoding();
                if ("gzip".equals(encoding) || "x-gzip".equals(encoding)) {
                    inStream = new GZIPInputStream(conn.getInputStream());
                } else {
                    inStream = conn.getInputStream();
                }

                reader = new BufferedReader(new InputStreamReader(inStream, getCharset(conn)));

                InputSource is = new InputSource(reader);
                is.setEncoding(getCharset(conn));

                DefaultContext dsc = new DefaultContext(activity.getResources().openRawResource(R.raw.synder));
                DefaultParseContext sfi = new DefaultParseContext(dsc);
                sf = sfi.parse(is);
            } catch (Exception e) {
                Log.printStackTrace(TAG, e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ioe) {
                        // Log.printStackTrace(TAG, ioe);
                    }
                }

                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException ioe) {
                        // Log.printStackTrace(TAG, ioe);
                    }
                }

                if (conn != null) {
                    conn.disconnect();
                }
            }

            if (sf == null) {
                return new LoaderPayload(LoaderPayload.STATUS_ERROR);
            }

            String feedId = f.getArguments().getString("feedId");

            List<SyndEntry> news = sf.getEntries();
            ArrayList<ContentValues> values = new ArrayList<>(news.size());
            ArrayList<ContentValues> myValues = new ArrayList<>(news.size());

            for (SyndEntry entry : news) {
                ContentValues v = new ContentValues(14);
                ContentValues myV = new ContentValues(6);

                v.put(DB.News.AUTHOR, entry.getAuthor());

                String title = entry.getTitle().trim();
                v.put(DB.News.TITLE, Entities.HTML40.unescape(title));

                Date d = entry.getPublishedDate();
                if (d != null) {
                    v.put(DB.News.PUBLISHED_DATE, d.getTime());
                } else {
                    v.put(DB.News.PUBLISHED_DATE, 0);
                }

                v.put(DB.News.FEED_ID, feedId);
                myV.put(DB.MyNews.FEED_ID, feedId);

                List<SyndContent> contents = entry.getContents();
                if (contents.isEmpty()) {
                    v.put(DB.News.CONTENT_VALUES, "");
                } else {
                    StringBuilder sb = new StringBuilder(256);
                    for (SyndContent content : contents) {
                        String value = content.getValue();

                        if (value != null) {
                            // some cleaning up... ugly code follows
                            String txt = PATTERN_TAG_IMG.matcher(value).replaceAll("");
                            txt = PATTERN_TAG_BR.matcher(txt).replaceAll("\n");
                            txt = PATTERN_EMPTY_TAGS.matcher(txt).replaceAll("");
                            txt = PATTERN_EMPTY_TAGS.matcher(txt).replaceAll(""); // pff...
                            txt = txt.trim();

                            int len = txt.length();

                            for (int i = 0; i < len; ++i) {
                                char c = txt.charAt(i);

                                if (i < 2) {
                                    sb.append(c);
                                    continue;
                                }

                                if (c != '\n') {
                                    sb.append(c);
                                    continue;
                                } // else: we've reached a \n

                                if (c != txt.charAt(i - 1)) {
                                    if (i < 4
                                            || txt.charAt(i - 1) != '>'
                                            || txt.charAt(i - 2) != 'p'
                                            || txt.charAt(i - 3) != '/'
                                            || txt.charAt(i - 4) != '<') {
                                        if (i > len - 4
                                                || txt.charAt(i + 1) != '<'
                                                || txt.charAt(i + 2) != 'p'
                                                || txt.charAt(i + 3) != '>') {
                                            sb.append('<');
                                            sb.append('b');
                                            sb.append('r');
                                            sb.append('>');
                                        } // else skip \n if it's before <p>
                                    } // else skip \n if it's after </p>
                                    continue;
                                }
                                // else: we've reached a 2nd consecutive \n

                                if (c != txt.charAt(i - 2)) {
                                    sb.append('<');
                                    sb.append('b');
                                    sb.append('r');
                                    sb.append('>');
                                } // else: we've reached the 3rd consecutive \n

                                // skip the 3rd consecutive \n
                            }

                            sb.append('<');
                            sb.append('b');
                            sb.append('r');
                            sb.append('>');
                        }
                    }

                    sb.setLength(sb.length() - 4);

                    v.put(DB.News.CONTENT_VALUES, sb.toString());
                }

                List<SyndLink> links = entry.getLinks();
                if (links != null && !links.isEmpty()) {
                    SyndLink link = entry.getLinks().get(0);
                    if (link != null) {
                        v.put(DB.News.URL, link.getHref());
                    }
                }

                String uri = entry.getUri();

                if (TextUtils.isEmpty(uri)) {
                    if (links != null) {
                        for (SyndLink link : links) {
                            if (link.getType().equals("guid")) {
                                uri = link.getHref();
                            } // else {
                            // Log.i("NOVELTY", "type: ", link.getType());
                            // }
                        }
                    } else {
                        Log.w(TAG, "oops!");
                    }
                }

                if (TextUtils.isEmpty(uri)) {
                    if (links != null && !links.isEmpty()) {
                        SyndLink link = entry.getLinks().get(0);
                        if (link != null) {
                            uri = link.getHref();
                        }
                    }
                }

                v.put(DB.News.SAVED_DATE, System.currentTimeMillis());

                v.put(DB.News.ID, uri);
                myV.put(DB.MyNews.ID, uri);

                StringBuilder sbUniqueId = new StringBuilder(64);
                sbUniqueId.append(uri);
                sbUniqueId.append(feedId);
                int uniqueId = sbUniqueId.toString().hashCode();

                v.put(DB.News._ID, uniqueId);
                v.put(DB.MyNews._ID, uniqueId);

                myV.put(DB.MyNews.IS_READ, 0);
                myV.put(DB.MyNews.IS_BOOKMARK, 0);

                values.add(v);
                myValues.add(myV);
            }

            DbProvider.bulkReplace(DB.News.URI, values.toArray(new ContentValues[values.size()]));
            DbProvider.contentResolver.bulkInsert(DB.MyNews.URI,
                    myValues.toArray(new ContentValues[myValues.size()]));

            return new LoaderPayload(LoaderPayload.STATUS_OK);
        }

        private HttpURLConnection setupConnection(String url) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            conn.setRequestProperty("Accept-Charset", "utf-8,*");
            // conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 6.0; ru; rv:1.9.0.11) Gecko/2009060215 Firefox/3.0.11 (.NET CLR 3.5.30729)");
            conn.connect();

            return conn;
        }

        private String getCharset(HttpURLConnection conn) {
            String contentType = conn.getContentType();
            String[] values = contentType.split(";"); // values.length must be equal to 2

            for (String value : values) {
                value = value.trim();

                if (value.toLowerCase().startsWith("charset=")) {
                    return value.substring("charset=".length());
                }
            }

            return "UTF-8"; // just in case...
        }
    }

    private static class UpdateReadLoader extends AsyncLoader<LoaderPayload> {
        // private WeakReference<Context> activityRef;
        private Bundle data, args;

        // only retain a weak reference to the activity
        UpdateReadLoader(Context context, Bundle data, Bundle args) {
            super(context);
            // activityRef = new WeakReference<>(context);
            this.data = data;
            this.args = args;
        }

        @Override
        public LoaderPayload loadInBackground() {
            String newsId = data.getString("newsId");
            String feedId = args.getString("feedId");

            ContentValues v = new ContentValues(2);
            v.put(DB.MyNews.IS_READ, 1);

            if (DbProvider.contentResolver.update(DB.MyNews.URI, v, DB.MyNews.ID + "=? AND "
                    + DB.MyNews.FEED_ID + "=?", new String[]{newsId, feedId}) > 0) {
                DbProvider.contentResolver.notifyChange(DB.MyNews.URI, null, false);
                return new LoaderPayload(LoaderPayload.STATUS_OK);
            }

            return new LoaderPayload(LoaderPayload.STATUS_ERROR);
        }
    }
}
