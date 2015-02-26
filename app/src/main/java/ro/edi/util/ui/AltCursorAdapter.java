/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright 2015 Eduard Scarlat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.edi.util.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * An easy adapter to map columns from a cursor to TextViews or ImageViews defined in an XML file. You can specify which
 * columns you want, which views you want to display the columns, and the XML file that defines the appearance of these
 * views.
 * <p/>
 * Binding occurs in two phases. First, if a {@link AltCursorAdapter.ViewBinder} is available,
 * {@link ViewBinder#setViewValue(android.view.View, android.database.Cursor, int)} is invoked. If the returned value is
 * true, binding has occured. If the returned value is false and the view to bind is a TextView,
 * {@link #setViewText(TextView, String)} is invoked. If the returned value is false and the view to bind is an
 * ImageView, {@link #setViewImage(ImageView, String)} is invoked. If no appropriate binding can be found, an
 * {@link IllegalStateException} is thrown.
 * <p/>
 * If this adapter is used with filtering, for instance in an {@link android.widget.AutoCompleteTextView}, you can use
 * the {@link AltCursorAdapter.CursorToStringConverter} and the {@link android.widget.FilterQueryProvider} interfaces to
 * get control over the filtering process. You can refer to {@link #convertToString(android.database.Cursor)} and
 * {@link #runQueryOnBackgroundThread(CharSequence)} for more information.
 */
public class AltCursorAdapter extends CursorAdapter {
    private final Context mContext;

    private int[] mFrom; // a list of columns containing the data to bind to the UI
    private int[] mTo; // a list of view ids representing the views to which the data must be bound

    private int mStringConversionColumn = -1;
    private CursorToStringConverter mCursorToStringConverter;
    private ViewBinder mViewBinder;
    private String[] mOriginalFrom;

    private final LayoutInflater mInflater;
    private final int[] mLayouts;

    /**
     * The resources indicating what views to inflate to display the content of this array adapter in a drop down
     * widget.
     */
    private int[] mDropDownLayouts;

    /**
     * Constructor.
     *
     * @param context The context where the ListView associated with this SimpleListItemFactory is running
     * @param layouts array of resource identifiers of layout files that define the views for this list item.
     *                getViewTypeCount() will return the length of this array.
     * @param c       The database cursor. Can be null if the cursor is not available yet.
     * @param from    A list of column names representing the data to bind to the UI. Can be null if the cursor is not
     *                available yet.
     * @param to      The views that should display column in the "from" parameter. The first N views in this list are given
     *                the values of the first N columns in the from parameter. Can be null if the cursor is not available
     *                yet.
     */
    public AltCursorAdapter(Context context, int[] layouts, Cursor c, String[] from, int[] to) {
        super(context, c, 0);
        mContext = context;
        mLayouts = layouts;
        mDropDownLayouts = layouts;
        mTo = to;

        mOriginalFrom = from;

        // hack to avoid annoying Log.e messages from android.database.sqlite.SQLiteCursor
        // if (Integer.parseInt(Build.VERSION.SDK) > 7)
        // {
        for (int i = 0; i < mOriginalFrom.length; ++i) {
            int idxDot = mOriginalFrom[i].lastIndexOf('.');
            if (idxDot != -1) {
                mOriginalFrom[i] = mOriginalFrom[i].substring(idxDot + 1);
            }
        }
        // }

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        findColumns(from);
    }

    /**
     * @see android.widget.CursorAdapter#getView(int, View, ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getCursor() == null) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }

        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        View v = convertView == null ? newView(mContext, getCursor(), parent) : convertView;

        bindView(v, mContext, getCursor());
        return v;
    }

    /**
     * Inflates view(s) from the specified XML file.
     *
     * @see android.widget.CursorAdapter#newView(android.content.Context, android.database.Cursor, ViewGroup)
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int type = getItemViewType(cursor.getPosition());
        // Log.i("ACA", "getItemViewType: " + type + ", position: " + cursor.getPosition());
        return mInflater.inflate(mLayouts[type], parent, false);
    }

    /**
     * Binds all of the field names passed into the "to" parameter of the constructor with their corresponding cursor
     * columns as specified in the "from" parameter.
     * <p/>
     * Binding occurs in two phases. First, if a {@link AltCursorAdapter.ViewBinder} is available,
     * {@link ViewBinder#setViewValue(android.view.View, android.database.Cursor, int)} is invoked. If the returned
     * value is true, binding has occurred. If the returned value is false and the view to bind is a TextView,
     * {@link #setViewText(TextView, String)} is invoked. If the returned value is false and the view to bind is an
     * ImageView, {@link #setViewImage(ImageView, String)} is invoked. If no appropriate binding can be found, an
     * {@link IllegalStateException} is thrown.
     *
     * @throws IllegalStateException if binding cannot occur
     * @see android.widget.CursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
     * @see #getViewBinder()
     * @see #setViewBinder(AltCursorAdapter.ViewBinder)
     * @see #setViewImage(ImageView, String)
     * @see #setViewText(TextView, String)
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewBinder binder = mViewBinder;
        int count = mTo.length;
        int[] from = mFrom;
        int[] to = mTo;

        for (int i = 0; i < count; i++) {
            View v = view.findViewById(to[i]);
            if (v != null) {
                boolean bound = false;
                if (binder != null) {
                    bound = binder.setViewValue(v, cursor, from[i]);
                }

                if (!bound) {
                    String text = cursor.getString(from[i]);
                    if (text == null) {
                        text = "";
                    }

                    if (v instanceof TextView) {
                        setViewText((TextView) v, text);
                    } else if (v instanceof ImageView) {
                        setViewImage((ImageView) v, text);
                    } else {
                        throw new IllegalStateException(v.getClass().getName() + " is not a "
                                + " view that can be bounds by this AltCursorAdapter");
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Sets the layout resources to create the drop down views.
     * </p>
     *
     * @param layouts the layout resources defining the drop down views
     * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
     */
    public void setDropDownLayouts(int[] layouts) {
        mDropDownLayouts = layouts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (getCursor() == null) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!getCursor().moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        View v = convertView == null ? newDropDownView(mContext, getCursor(), parent) : convertView;

        bindView(v, mContext, getCursor());
        return v;
    }

    @Override
    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
        int type = getItemViewType(cursor.getPosition());
        // Log.i("ACA", "getItemViewType: " + type + ", position: " + cursor.getPosition());
        return mInflater.inflate(mDropDownLayouts[type], parent, false);
    }

    /**
     * Returns the {@link ViewBinder} used to bind data to views.
     *
     * @return a ViewBinder or null if the binder does not exist
     * @see #bindView(android.view.View, android.content.Context, android.database.Cursor)
     * @see #setViewBinder(AltCursorAdapter.ViewBinder)
     */
    public ViewBinder getViewBinder() {
        return mViewBinder;
    }

    /**
     * Sets the binder used to bind data to views.
     *
     * @param viewBinder the binder used to bind data to views, can be null to remove the existing binder
     * @see #bindView(android.view.View, android.content.Context, android.database.Cursor)
     * @see #getViewBinder()
     */
    public void setViewBinder(ViewBinder viewBinder) {
        mViewBinder = viewBinder;
    }

    @Override
    public int getViewTypeCount() {
        return mLayouts.length;
    }

    @Override
    public int getItemViewType(int position) {
        if (mViewBinder == null) {
            // Log.i("ACA", "binder null");
            return 0;
        }

        return mViewBinder.getItemViewType(getCursor(), position);
    }

    /**
     * Called by bindView() to set the image for an ImageView but only if there is no existing ViewBinder or if the
     * existing ViewBinder cannot handle binding to an ImageView.
     * <p/>
     * By default, the value will be treated as an image resource. If the value cannot be used as an image resource, the
     * value is used as an image Uri.
     * <p/>
     * Intended to be overridden by Adapters that need to filter strings retrieved from the database.
     *
     * @param v     ImageView to receive an image
     * @param value the value retrieved from the cursor
     */
    public void setViewImage(ImageView v, String value) {
        try {
            v.setImageResource(Integer.parseInt(value));
        } catch (NumberFormatException nfe) {
            v.setImageURI(Uri.parse(value));
        }
    }

    /**
     * Called by bindView() to set the text for a TextView but only if there is no existing ViewBinder or if the
     * existing ViewBinder cannot handle binding to an TextView.
     * <p/>
     * Intended to be overridden by Adapters that need to filter strings retrieved from the database.
     *
     * @param v    TextView to receive text
     * @param text the text to be set for the TextView
     */
    public void setViewText(TextView v, String text) {
        v.setText(text);
    }

    /**
     * Return the index of the column used to get a String representation of the Cursor.
     *
     * @return a valid index in the current Cursor or -1
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     * @see #setStringConversionColumn(int)
     * @see #setCursorToStringConverter(AltCursorAdapter.CursorToStringConverter)
     * @see #getCursorToStringConverter()
     */
    public int getStringConversionColumn() {
        return mStringConversionColumn;
    }

    /**
     * Defines the index of the column in the Cursor used to get a String representation of that Cursor. The column is
     * used to convert the Cursor to a String only when the current CursorToStringConverter is null.
     *
     * @param stringConversionColumn a valid index in the current Cursor or -1 to use the default conversion mechanism
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     * @see #getStringConversionColumn()
     * @see #setCursorToStringConverter(AltCursorAdapter.CursorToStringConverter)
     * @see #getCursorToStringConverter()
     */
    public void setStringConversionColumn(int stringConversionColumn) {
        mStringConversionColumn = stringConversionColumn;
    }

    /**
     * Returns the converter used to convert the filtering Cursor into a String.
     *
     * @return null if the converter does not exist or an instance of {@link AltCursorAdapter.CursorToStringConverter}
     * @see #setCursorToStringConverter(AltCursorAdapter.CursorToStringConverter)
     * @see #getStringConversionColumn()
     * @see #setStringConversionColumn(int)
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     */
    public CursorToStringConverter getCursorToStringConverter() {
        return mCursorToStringConverter;
    }

    /**
     * Sets the converter used to convert the filtering Cursor into a String.
     *
     * @param cursorToStringConverter the Cursor to String converter, or null to remove the converter
     * @see #setCursorToStringConverter(AltCursorAdapter.CursorToStringConverter)
     * @see #getStringConversionColumn()
     * @see #setStringConversionColumn(int)
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     */
    public void setCursorToStringConverter(CursorToStringConverter cursorToStringConverter) {
        mCursorToStringConverter = cursorToStringConverter;
    }

    /**
     * Returns a CharSequence representation of the specified Cursor as defined by the current CursorToStringConverter.
     * If no CursorToStringConverter has been set, the String conversion column is used instead. If the conversion
     * column is -1, the returned String is empty if the cursor is null or Cursor.toString().
     *
     * @param cursor the Cursor to convert to a CharSequence
     * @return a non-null CharSequence representing the cursor
     */
    @Override
    public CharSequence convertToString(Cursor cursor) {
        if (mCursorToStringConverter != null) {
            return mCursorToStringConverter.convertToString(cursor);
        }

        if (mStringConversionColumn > -1) {
            return cursor.getString(mStringConversionColumn);
        }

        return super.convertToString(cursor);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        if (mDataValid && mCursor != null) {
            if (mCursor.moveToPosition(position)) {
                return mCursor.getLong(mRowIDColumn);
            }

            return 0;
        }

        return 0;
    }

    /**
     * Create a map from an array of strings to an array of column-id integers in mCursor. If mCursor is null, the array
     * will be discarded.
     *
     * @param from the Strings naming the columns of interest
     */
    private void findColumns(String[] from) {
        Cursor c = getCursor();
        if (c != null) {
            int count = from.length;
            if (mFrom == null || mFrom.length != count) {
                mFrom = new int[count];
            }

            // Log.i("ACA", TextUtils.join(" ", c.getColumnNames()));

            for (int i = 0; i < count; ++i) {
                mFrom[i] = c.getColumnIndex(from[i]);
            }
        } else {
            mFrom = null;
        }
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        // Log.i("ACA", c == null ? "null" : TextUtils.join(", ", c.getColumnNames()));

        Cursor res = super.swapCursor(c);
        // rescan columns in case cursor layout is different
        findColumns(mOriginalFrom);
        return res;
    }

    @Override
    public void changeCursor(Cursor c) {
        // Log.i("ACA", "changeCursor()");
        super.changeCursor(c);

        // rescan columns in case cursor layout is different
        findColumns(mOriginalFrom);
    }

    /**
     * Change the cursor and change the column-to-view mappings at the same time.
     *
     * @param c    The database cursor. Can be null if the cursor is not available yet.
     * @param from A list of column names representing the data to bind to the UI. Can be null if the cursor is not
     *             available yet.
     * @param to   The views that should display column in the "from" parameter. These should all be TextViews. The first
     *             N views in this list are given the values of the first N columns in the from parameter. Can be null if
     *             the cursor is not available yet.
     */
    public void changeCursorAndColumns(Cursor c, String[] from, int[] to) {
        // Log.i("ACA", "changeCursorAndColumns()");

        mOriginalFrom = from;
        mTo = to;
        super.changeCursor(c);

        // rescan columns in case cursor layout is different
        findColumns(mOriginalFrom);
    }

    /**
     * Change the column-to-view mappings.
     *
     * @param from A list of column names representing the data to bind to the UI.
     * @param to   The views that should display column in the "from" parameter.
     */
    public void changeColumns(String[] from, int[] to) {
        mOriginalFrom = from;
        mTo = to;
        findColumns(mOriginalFrom);
    }

    /**
     * This class can be used by external clients of AltCursorAdapter to bind values from the Cursor to views.
     * <p/>
     * You should use this class to bind values from the Cursor to views that are not directly supported by
     * AltCursorAdapter or to change the way binding occurs for views supported by AltCursorAdapter.
     *
     * @see AltCursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
     * @see AltCursorAdapter#setViewImage(ImageView, String)
     * @see AltCursorAdapter#setViewText(TextView, String)
     */
    public static interface ViewBinder {
        /**
         * Binds the Cursor column defined by the specified index to the specified view.
         * <p/>
         * When binding is handled by this ViewBinder, this method must return true. If this method returns false,
         * AltCursorAdapter will attempts to handle the binding on its own.
         *
         * @param view        the view to bind the data to
         * @param cursor      the cursor to get the data from
         * @param columnIndex the column at which the data can be found in the cursor
         * @return true if the data was bound to the view, false otherwise
         */
        boolean setViewValue(View view, Cursor cursor, int columnIndex);

        int getItemViewType(Cursor cursor, int position);
    }

    /**
     * This class can be used by external clients of AltCursorAdapter to define how the Cursor should be converted to a
     * String.
     *
     * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
     */
    public static interface CursorToStringConverter {
        /**
         * Returns a CharSequence representing the specified Cursor.
         *
         * @param cursor the cursor for which a CharSequence representation is requested
         * @return a non-null CharSequence representing the cursor
         */
        CharSequence convertToString(Cursor cursor);
    }
}
