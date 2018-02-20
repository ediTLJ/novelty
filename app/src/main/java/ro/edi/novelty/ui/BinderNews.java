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

import android.content.Context;
import android.content.res.Resources;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import ro.edi.novelty.R;
import ro.edi.novelty.data.DB;
import ro.edi.util.ui.AltCursorAdapter.ViewBinder;

public class BinderNews implements ViewBinder {
    private int idxIsRead = -1;

    private final int colorNew, colorRead;

    public BinderNews(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();

        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        colorNew = ContextCompat.getColor(context, typedValue.resourceId);

        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        colorRead = ContextCompat.getColor(context, typedValue.resourceId);
        // context.getResources().getColor(typedValue.resourceId);
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        switch (view.getId()) {
            case R.id.news_title:
                TextView tvText = (TextView) view;

                CharArrayBuffer textCache = (CharArrayBuffer) view.getTag();
                if (textCache == null) {
                    textCache = new CharArrayBuffer(192);
                    view.setTag(textCache);
                }
                cursor.copyStringToBuffer(columnIndex, textCache);

                int nameSize = textCache.sizeCopied;
                if (nameSize != 0) {
                    tvText.setText(textCache.data, 0, nameSize);
                }

                idxIsRead = idxIsRead < 0 ? cursor.getColumnIndex(DB.MyNews.IS_READ) : idxIsRead;

                tvText.setTextColor(cursor.getInt(idxIsRead) > 0 ? colorRead : colorNew);
                tvText.setTypeface(null, cursor.getInt(idxIsRead) > 0 ? Typeface.NORMAL : Typeface.BOLD);
                break;
            case R.id.news_feed:
                TextView tvFeed = (TextView) view;
                String feed = cursor.getString(columnIndex);
                if (TextUtils.isEmpty(feed)) {
                    tvFeed.setVisibility(View.GONE);
                } else {
                    tvFeed.setText(feed);
                    tvFeed.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.news_date:
                TextView tvDate = (TextView) view;
                long t = cursor.getLong(columnIndex);
                CharSequence time = t > 0 ? DateUtils.getRelativeTimeSpanString(t) : "";

                if (!time.equals(tvDate.getText())) {
                    tvDate.setText(time);
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public int getItemViewType(Cursor cursor, int position) {
        return 0;
    }
}