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
package ro.edi.util.ui;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * A ListView with the following improvements:<br />
 * - method to scroll (smoothly or otherwise) to a specific position (this takes advantage of similar functionality that
 * exists in {@link ListView} and enhances it).<br />
 * - keeps the scroll position when the data is changed, if requested to do so.<br />
 * - it expands, if requested to do so.<br />
 */
public class AltListView extends ListView {
    private static final int NOT_SET = -10;

    // position the element at about 1/3 of the list height
    private static final float PREFERRED_SELECTION_OFFSET_FROM_TOP = 0.33f;

    private int mRequestedScrollPosition = -1;
    private boolean mSmoothScrollRequested;

    private boolean mKeepScrollPosition = true; // keep scroll position flag
    private boolean mIsExpanded = false; // expand the listview flag

    private long mLastRestoredId = NOT_SET;
    private int mLastRestoredY = NOT_SET;
    private long mSavedId = NOT_SET;
    private int mSavedY = NOT_SET; // y relative position of the first child

    private View mFirstChild;

    private boolean mIgnoreLayoutRequests = false;

    @SuppressWarnings("unused")
    public AltListView(Context context) {
        super(context);
    }

    @SuppressWarnings("unused")
    public AltListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("unused")
    public AltListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Tell the ListView to keep its position or not.
     *
     * @param keepScrollPosition the keepScrollPosition to set
     */
    @SuppressWarnings("unused")
    public void setKeepScrollPosition(boolean keepScrollPosition) {
        mKeepScrollPosition = keepScrollPosition;
        // Log.i("AltListView", "set keepScrollPosition: ", keepScrollPosition);
    }

    /**
     * Tell the ListView to expand (or not) in order to fit all its children.
     */
    public void setIsExpanded(boolean expand) {
        mIsExpanded = expand;
    }

    /**
     * Tell the ListView to temporary ignore layout requests.
     */
    public void setIgnoreLayoutRequests(boolean ignore) {
        mIgnoreLayoutRequests = ignore;
    }

    /**
     * Brings the specified position to view by optionally performing a jump-scroll maneuver: first it jumps to some
     * position near the one requested and then does a smooth scroll to the requested position. This creates an
     * impression of full smooth scrolling without actually traversing the entire list. If smooth scrolling is not
     * requested, instantly positions the requested item at a preferred offset.
     */
    public void requestPositionToScreen(int position, boolean smoothScroll) {
        mRequestedScrollPosition = position;
        mSmoothScrollRequested = smoothScroll;
        requestLayout();
    }

    @Override
    public void requestLayout() {
        if (mIgnoreLayoutRequests) {
            return;
        }

        super.requestLayout();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if (mRequestedScrollPosition == -1) {
            return;
        }

        int position = mRequestedScrollPosition;
        mRequestedScrollPosition = -1;

        int firstPosition = getFirstVisiblePosition() + 1;
        int lastPosition = getLastVisiblePosition();
        if (position >= firstPosition && position <= lastPosition) {
            return; // already on screen
        }

        int offset = (int) (getHeight() * PREFERRED_SELECTION_OFFSET_FROM_TOP);

        if (mSmoothScrollRequested) {
            // We will first position the list a couple of screens before or after
            // the new selection and then scroll smoothly to it.
            int twoScreens = (lastPosition - firstPosition) * 2;
            int preliminaryPosition;
            if (position < firstPosition) {
                preliminaryPosition = position + twoScreens;
                if (preliminaryPosition >= getCount()) {
                    preliminaryPosition = getCount() - 1;
                }
                if (preliminaryPosition < firstPosition) {
                    setSelection(preliminaryPosition);
                    super.layoutChildren();
                }
            } else {
                preliminaryPosition = position - twoScreens;
                if (preliminaryPosition < 0) {
                    preliminaryPosition = 0;
                }
                if (preliminaryPosition > lastPosition) {
                    setSelection(preliminaryPosition);
                    super.layoutChildren();
                }
            }

            smoothScrollToPositionFromTop(position, offset);
        } else {
            setSelectionFromTop(position, offset);

            // Since we have changed the scrolling position, we need to redo child layout
            // Calling "requestLayout" in the middle of a layout pass has no effect,
            // so we call layoutChildren explicitly
            super.layoutChildren();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIsExpanded) {
            // pass it a very big size with AT_MOST to show all the rows
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(65536, MeasureSpec.AT_MOST);

            // super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            // ViewGroup.LayoutParams params = getLayoutParams();
            // params.height = getMeasuredHeight();
            // return;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mFirstChild = getChildCount() > 0 ? getChildAt(0) : null;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);

        long id = mSavedId;
        if (id != NOT_SET) // pos < adapter.getCount()
        {
            doSetPosition(id, mSavedY);
            // Log.i("AltListView", "restore/setAdapter ", id, ", ", savedY);
        }
        mSavedId = NOT_SET;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (mKeepScrollPosition) {
            Parcelable parent = super.onSaveInstanceState();

            long id = mSavedId == NOT_SET ? mLastRestoredId == NOT_SET ? getItemIdAtPosition(getFirstVisiblePosition())
                    : mLastRestoredId : mSavedId;
            // Log.i("AltListView", "save id: ", id);

            return new SavedState(parent, id, getSavedY());
        }

        return super.onSaveInstanceState();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!mKeepScrollPosition) // !(state instanceof SavedState)
        {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;

        mIgnoreLayoutRequests = true;
        super.onRestoreInstanceState(ss.getSuperState());
        mIgnoreLayoutRequests = false;

        long id = ss.firstId;
        int y = ss.firstY;

        if (id != NOT_SET && getAdapter() != null) {
            doSetPosition(id, y);
            mSavedId = NOT_SET;
        } else {
            mSavedId = id;
            mSavedY = y;
        }
    }

    private int getSavedY() {
        // Log.i("AltListView", "savedY: ", mSavedY);
        // Log.i("AltListView", "lastRestoredY: ", mLastRestoredY);

        if (mSavedY != NOT_SET) {
            return mSavedY;
        }
        if (mLastRestoredY != NOT_SET) {
            return mLastRestoredY;
        }

        // Log.i("AltListView", "Child count: ", getChildCount());

        View child = getChildAt(0);
        if (child == null) {
            child = mFirstChild;
            // Log.i("AltListView", "Use first child from onLayout");
        }

        if (child == null) {
            // Log.i("AltListView", "Cannot get the first child");
            return 0;
        }

        // Log.i("AltListView", "Saved y: ", child.getTop());
        return child.getTop();
    }

    private void doSetPosition(long id, int y) {
        mLastRestoredId = NOT_SET;
        mLastRestoredY = NOT_SET;

        if (id == getItemIdAtPosition(getFirstVisiblePosition())) {
            // also check for y? I don't think it's needed...
            return; // already on screen & in the right position
        }

        int count = getCount();
        int position = count - 1;

        // Log.i("AltListView", "searching for id: ", id);

        for (int i = 0; i < count - 1; ++i) {
            // Log.i("AltListView", "id", i, ": ", getItemIdAtPosition(i));

            if (getItemIdAtPosition(i) == id) {
                position = i;
                break;
            }
        }

        // Log.i("AltListView", "set selection from top: ", position);
        setSelectionFromTop(position, y);
    }

    /**
     * Saved state for our list view.
     */
    static class SavedState extends BaseSavedState {
        /**
         * Creator... do NOT delete this, even if it appears not being used :)
         */
        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        final long firstId; // saved first id
        final int firstY; // saved first top

        SavedState(Parcelable parent, long firstId, int firstY) {
            super(parent);
            this.firstId = firstId;
            this.firstY = firstY;
        }

        private SavedState(Parcel in) {
            super(in);
            firstId = in.readLong();
            firstY = in.readInt();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(firstId);
            dest.writeInt(firstY);
        }
    }
}