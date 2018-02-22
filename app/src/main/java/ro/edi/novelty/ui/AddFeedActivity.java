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

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import ro.edi.novelty.R;
import ro.edi.novelty.core.FeedsManager;
import ro.edi.novelty.data.Feed;

public class AddFeedActivity extends BaseActivity {
    private static final int MAX_CHARS = 14; // TODO keep it as a resource

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_add_feed;
    }

    @Override
    protected Toolbar initToolbar() {
        Toolbar toolbar = super.initToolbar();
        if (toolbar != null) {
            // noinspection ConstantConditions
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.setTitle(R.string.description_add_feed);
        }
        return toolbar;
    }

    @Override
    protected void initUI() {
        super.initUI();

        final EditText editTitle = findViewById(R.id.feed_title);

        final EditText editUrl = findViewById(R.id.feed_url);

        final View btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> {
            editTitle.setError(null);
            editUrl.setError(null);

            String title = editTitle.getText().toString().trim();
            String url = editUrl.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                editTitle.setError(getText(R.string.feed_title_required));
                editTitle.requestFocus();
            } else if (title.length() > MAX_CHARS) {
                editTitle.setError(getText(R.string.feed_title_too_long));
                editTitle.requestFocus();
            } else if (TextUtils.isEmpty(url)) {
                editUrl.setError(getText(R.string.feed_url_required));
                editUrl.requestFocus();
            } else {
                if (url.startsWith("HTTPS://")) {
                    url = url.replaceFirst("HTTPS", "https");
                } else if (url.startsWith("HTTP://")) {
                    url = url.replaceFirst("HTTP", "http");
                } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url;
                }

                FeedsManager feedsManager = FeedsManager.getInstance();

                boolean isDuplicate = false;
                for (int k = 1; k < FeedsManager.MAX_FEEDS_COUNT + 1; ++k) {
                    Feed feed = feedsManager.getFeed(k);

                    if (feed != null) {
                        if (feed.getUrl().equals(url)) {
                            editUrl.setError(getText(R.string.feed_url_duplicate));
                            editUrl.requestFocus();
                            isDuplicate = true;
                        }

                        if (feed.getTitle().equalsIgnoreCase(title)) {
                            editTitle.setError(getText(R.string.feed_title_duplicate));
                            editTitle.requestFocus();
                            isDuplicate = true;
                        }

                        if (isDuplicate) {
                            break;
                        }
                    }
                }
                if (isDuplicate) {
                    return;
                }

                // TODO check if link valid... if not, show error... if yes, check if 1 feed or more

                // if 1 feed
                feedsManager.addFeed(title, url);
                setResult(Activity.RESULT_OK);
                finish();

                // TODO display list of feeds to pick from, if more feeds found
            }
        });

        editUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnAdd.performClick();
                return true;
            }
            return false;
        });
    }
}
