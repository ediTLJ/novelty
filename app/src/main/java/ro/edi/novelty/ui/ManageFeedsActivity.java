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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ro.edi.novelty.R;
import ro.edi.novelty.data.Feed;
import ro.edi.novelty.core.FeedsManager;
import ro.edi.util.recyclerview.DividerItemDecoration;
import ro.edi.util.recyclerview.OnRecyclerClickItemListener;
import ro.edi.util.recyclerview.OnRecyclerDragItemListener;
import ro.edi.util.recyclerview.OnRecyclerSwipeItemListener;

public class ManageFeedsActivity extends BaseActivity {
    // private static final String TAG = "MANAGE.FEEDS";

    private ManageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_manage_feeds;
    }

    @Override
    protected Toolbar initToolbar() {
        Toolbar toolbar = super.initToolbar();
        if (toolbar != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.setTitle(R.string.menu_manage_feeds);
        }
        return toolbar;
    }

    @Override
    protected void initUI() {
        super.initUI();

        RecyclerView recyclerView = (RecyclerView) findViewById(android.R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setHasFixedSize(true);

        // populate list with our feeds
        FeedsManager feedsManager = FeedsManager.getInstance();
        int count = feedsManager.getFeedsCount();
        ArrayList<Feed> feedsList = new ArrayList<Feed>(count);
        for (int k = 1; k < count + 1; ++k) {
            feedsList.add(feedsManager.getFeed(k));
        }

        mAdapter = new ManageAdapter(feedsList);
        recyclerView.setAdapter(mAdapter);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_feed);

        OnRecyclerSwipeItemListener swipeListener = new OnRecyclerSwipeItemListener(recyclerView,
                new OnRecyclerSwipeItemListener.DismissCallbacks() {
                    @Override
                    public OnRecyclerSwipeItemListener.SwipeDirection canDismiss(int position) {
                        return OnRecyclerSwipeItemListener.SwipeDirection.BOTH;
                    }

                    @Override
                    public void onDismiss(RecyclerView view,
                                          List<OnRecyclerSwipeItemListener.PendingDismissData> dismissData) {
                        for (OnRecyclerSwipeItemListener.PendingDismissData data : dismissData) {
                            mAdapter.removeItem(data.position);
                            mAdapter.notifyItemRemoved(data.position);

                            if (mAdapter.getItemCount() == 0) {
                                finish();
                                return;
                            }

                            if (mAdapter.getItemCount() == FeedsManager.MAX_FEEDS_COUNT - 1) {
                                // we were at MAX_FEEDS_COUNT previously, so the FAB was hidden
                                fab.setVisibility(View.VISIBLE);
                                fab.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent iAdd = new Intent(getApplication(), AddFeedActivity.class);
                                        startActivityForResult(iAdd, 201);
                                    }
                                });
                                fab.attachToRecyclerView(view);
                            }

                            // TODO snackbar with undo
//                            new SnackBar.Builder(ManageFeedsActivity.this)
//                                    .withOnClickListener(new SnackBar.OnMessageClickListener() {
//                                        @Override
//                                        public void onMessageClick(Parcelable parcelable) {
//                                            // ...
//                                        }
//                                    })
//                                .withMessageId(R.string...)
//                                .withActionMessageId(R.string...)
//                                .withTextColorId(R.color...)
//                                .withDuration(duration)
//                                    .show();
                        }
                    }
                });
        recyclerView.addOnItemTouchListener(swipeListener);

        final OnRecyclerDragItemListener onRecyclerDragItemListener = new OnRecyclerDragItemListener(recyclerView, this) {
            @Override
            protected void onItemSwitch(RecyclerView recyclerView, int from, int to) {
                if (from == to) {
                    return;
                }

                // TODO use onItemDrop()

                mAdapter.swapPositions(from, to);
                mAdapter.notifyItemRangeChanged(from < to ? from : to,
                        from < to ? to - from + 1 : from - to + 1);
            }

            @Override
            protected void onItemDrop(RecyclerView recyclerView, int position) {
            }
        };
        recyclerView.addOnItemTouchListener(onRecyclerDragItemListener);

        recyclerView.addOnItemTouchListener(new OnRecyclerClickItemListener(recyclerView) {
            @Override
            public void onItemClick(RecyclerView parent, View clickedView, int position) {
                // Log.i(TAG, "onItemClick()");
            }

            @Override
            public void onItemLongClick(RecyclerView parent, View clickedView, int position) {
                // Log.i(TAG, "onItemLongClick()");
                onRecyclerDragItemListener.startDrag();
            }
        });

        if (FeedsManager.getInstance().canAddFeed()) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent iAdd = new Intent(getApplication(), AddFeedActivity.class);
                    startActivityForResult(iAdd, 201);
                }
            });
            fab.attachToRecyclerView(recyclerView);
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 201) {
            if (resultCode == RESULT_OK) {
                // TODO notify via events when the feed was added
                recreate();
            }
        }
    }

    private class ManageAdapter extends RecyclerView.Adapter<FeedHolder> {
        ArrayList<Feed> feedsList = new ArrayList<Feed>();

        ManageAdapter(ArrayList<Feed> feedsList) {
            setHasStableIds(true);
            this.feedsList = feedsList;
        }

        @Override
        public FeedHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.feed_item, parent, false);
            return new FeedHolder(v);
        }

        @Override
        public void onBindViewHolder(FeedHolder viewHolder, int position) {
            Feed feed = feedsList.get(position);
            viewHolder.tvTitle.setText(feed.getTitle());
            viewHolder.tvUrl.setText(feed.getUrl());
        }

        @Override
        public int getItemCount() {
            return feedsList.size();
        }

        @Override
        public long getItemId(int position) {
            if (position < 0 || position > feedsList.size() - 1) {
                return -1;
            }
            return feedsList.get(position).getTitle().hashCode();
        }

        public void swapPositions(int from, int to) {
            Collections.swap(feedsList, from, to);
            FeedsManager.getInstance().swapFeeds(from + 1, to + 1);
        }

        public void removeItem(int pos) {
            feedsList.remove(pos);
            FeedsManager.getInstance().removeFeed(pos + 1);
        }
    }

    private class FeedHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvUrl;

        public FeedHolder(View v) {
            super(v);

            tvTitle = (TextView) v.findViewById(R.id.feed_title);
            tvUrl = (TextView) v.findViewById(R.id.feed_url);
        }
    }
}
