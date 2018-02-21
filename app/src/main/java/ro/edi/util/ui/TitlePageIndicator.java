/*
 * Copyright (C) 2011 Jake Wharton
 * Copyright (C) 2011 Patrik Akerfeldt
 * Copyright (C) 2011 Francisco Figueiredo Jr.
 * Copyright (C) 2015 Eduard Scarlat.
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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;

import ro.edi.novelty.R;
import ro.edi.util.Log;

/**
 * A TitlePageIndicator is a PageIndicator which displays the title of left view (if exist), the title of the current
 * select view (centered) and the title of the right view (if exist). When the user scrolls the ViewPager then titles
 * are also scrolled.
 */
public class TitlePageIndicator extends View implements PageIndicator {
    /**
     * Percentage indicating what percentage of the screen width away from center should the underline be fully faded. A
     * value of 0.25 means that halfway between the center of the screen and an edge.
     */
    private static final float SELECTION_FADE_PERCENTAGE = 1f;

    /**
     * Percentage indicating what percentage of the screen width away from center should the selected text bold turn
     * off. A value of 0.05 means that 10% between the center and an edge.
     */
    private static final float BOLD_FADE_PERCENTAGE = 0.5f;

    /**
     * Title text used when no title is provided by the adapter.
     */
    private static final String EMPTY_TITLE = "";

    /**
     * Interface for a callback when the center item has been clicked.
     */
    public interface OnCenterItemClickListener {
        /**
         * Callback when the center item has been clicked.
         *
         * @param position Position of the current center item.
         */
        void onCenterItemClick(int position);
    }

    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mListener;
    private int mCurrentPage = -1;
    private float mPageOffset;
    private int mScrollState;
    private final Paint mPaintText = new Paint();
    private boolean mBoldText;
    private int mColorText;
    private int mColorSelected;
    private Path mPath = new Path();
    private final Rect mBounds = new Rect();
    private final Paint mPaintFooterLine = new Paint();
    private final Paint mPaintFooterIndicator = new Paint();
    private float mFooterIndicatorHeight;
    private float mFooterIndicatorUnderlinePadding;
    private float mFooterPadding;
    private float mTitlePadding;
    private float mTopPadding;
    /**
     * Left and right side padding for not active view titles.
     */
    private float mClipPadding;
    private float mFooterLineHeight;

    private static final int INVALID_POINTER = -1;

    private int mTouchSlop;
    private float mLastMotionX = -1;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsDragging;

    private OnCenterItemClickListener mCenterItemClickListener;

    private int[] mNotifications;

    public TitlePageIndicator(Context context) {
        this(context, null);
    }

    public TitlePageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TitlePageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (isInEditMode()) {
            return;
        }

        // TODO clean this up
        // Load defaults from resources
        Resources res = getResources();
        int defaultFooterColor = res.getColor(R.color.default_title_indicator_footer_color);
        float defaultFooterLineHeight = res.getDimension(R.dimen.default_title_indicator_footer_line_height);
        float defaultFooterIndicatorHeight = res.getDimension(R.dimen.default_title_indicator_footer_indicator_height);
        float defaultFooterIndicatorUnderlinePadding = res
                .getDimension(R.dimen.default_title_indicator_footer_indicator_underline_padding);
        float defaultFooterPadding = res.getDimension(R.dimen.default_title_indicator_footer_padding);
        int defaultSelectedColor = res.getColor(R.color.default_title_indicator_selected_color);
        boolean defaultSelectedBold = res.getBoolean(R.bool.default_title_indicator_selected_bold);
        int defaultTextColor = res.getColor(R.color.default_title_indicator_text_color);
        float defaultTextSize = res.getDimension(R.dimen.default_title_indicator_text_size);
        float defaultTitlePadding = res.getDimension(R.dimen.default_title_indicator_title_padding);
        float defaultClipPadding = res.getDimension(R.dimen.default_title_indicator_clip_padding);
        float defaultTopPadding = res.getDimension(R.dimen.default_title_indicator_top_padding);

        // Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TitlePageIndicator, defStyle, 0);

        // Retrieve the colors to be used for this view and apply them.
        mFooterLineHeight = a.getDimension(R.styleable.TitlePageIndicator_footerLineHeight, defaultFooterLineHeight);
        mFooterIndicatorHeight = a.getDimension(R.styleable.TitlePageIndicator_footerIndicatorHeight,
                defaultFooterIndicatorHeight);
        mFooterIndicatorUnderlinePadding = a.getDimension(
                R.styleable.TitlePageIndicator_footerIndicatorUnderlinePadding, defaultFooterIndicatorUnderlinePadding);
        mFooterPadding = a.getDimension(R.styleable.TitlePageIndicator_footerPadding, defaultFooterPadding);
        mTopPadding = a.getDimension(R.styleable.TitlePageIndicator_topPadding, defaultTopPadding);
        mTitlePadding = a.getDimension(R.styleable.TitlePageIndicator_titlePadding, defaultTitlePadding);
        mClipPadding = a.getDimension(R.styleable.TitlePageIndicator_clipPadding, defaultClipPadding);
        mColorSelected = a.getColor(R.styleable.TitlePageIndicator_selectedColor, defaultSelectedColor);
        mColorText = a.getColor(R.styleable.TitlePageIndicator_textColor, defaultTextColor);
        mBoldText = a.getBoolean(R.styleable.TitlePageIndicator_selectedBold, defaultSelectedBold);

        float textSize = a.getDimension(R.styleable.TitlePageIndicator_textSize, defaultTextSize);
        int footerColor = a.getColor(R.styleable.TitlePageIndicator_footerColor, defaultFooterColor);
        mPaintText.setTextSize(textSize);
        mPaintText.setAntiAlias(true);
        mPaintText.setDither(true);
        mPaintText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        mPaintFooterLine.setStyle(Paint.Style.FILL_AND_STROKE);
        // noinspection SuspiciousNameCombination
        mPaintFooterLine.setStrokeWidth(mFooterLineHeight);
        mPaintFooterLine.setColor(footerColor);
        mPaintFooterIndicator.setDither(true);

        // http://code.google.com/p/android/issues/detail?id=24873
        // bug when enabling hardware acceleration

        // workaround #1: disable hw acceleration => the drawing will be rough
        // setLayerType(View.LAYER_TYPE_SOFTWARE, mPaintFooterIndicator);

        // workaround #2: avoid using 0 as stroke width => the drawing will be smooth as butter
        mPaintFooterIndicator.setStrokeWidth(1);

        mPaintFooterIndicator.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintFooterIndicator.setColor(footerColor);

        a.recycle();

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledPagingTouchSlop();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mViewPager == null || mViewPager.getAdapter() == null) {
            return;
        }

        int count = mViewPager.getAdapter().getCount();
        if (count == 0) {
            return;
        }

        // mCurrentPage is -1 on first start and after orientation changed.
        // if so, retrieve the correct index from viewpager.
        if (mCurrentPage == -1 && mViewPager != null) {
            mCurrentPage = mViewPager.getCurrentItem();
        }

        // calculate views bounds
        ArrayList<Rect> bounds = calculateAllBounds(mPaintText);
        int boundsSize = bounds.size();

        // make sure we're on a page that still exists
        if (mCurrentPage >= boundsSize) {
            setCurrentItem(boundsSize - 1);
            return;
        }

        int left = getLeft();
        float leftClip = left + mClipPadding;
        int width = getWidth();
        int height = getHeight();
        // float halfWidth = width / 2f;
        int right = left + width;
        float rightClip = right - mClipPadding;

        int page = mCurrentPage;
        float offsetPercent;
        if (mPageOffset <= 0.5) {
            offsetPercent = mPageOffset;
        } else {
            page += 1;
            offsetPercent = 1 - mPageOffset;
        }
        boolean currentSelected = offsetPercent <= SELECTION_FADE_PERCENTAGE;
        boolean currentBold = offsetPercent <= BOLD_FADE_PERCENTAGE;
        float selectedPercent = (SELECTION_FADE_PERCENTAGE - offsetPercent) / SELECTION_FADE_PERCENTAGE;

        // Verify if the current view must be clipped to the screen
        Rect curPageBound = bounds.get(mCurrentPage);
        float curPageWidth = curPageBound.right - curPageBound.left;
        if (curPageBound.left < leftClip) {
            // Try to clip to the screen (left side)
            clipViewOnTheLeft(curPageBound, curPageWidth, left);
        }
        if (curPageBound.right > rightClip) {
            // Try to clip to the screen (right side)
            clipViewOnTheRight(curPageBound, curPageWidth, right);
        }

        // left views starting from the current position
        if (mCurrentPage > 0) {
            for (int i = mCurrentPage - 1; i >= 0; i--) {
                Rect bound = bounds.get(i);
                // if left side is outside the screen
                if (bound.left < leftClip) {
                    int w = bound.right - bound.left;
                    // try to clip to the screen (left side)
                    clipViewOnTheLeft(bound, w, left);
                    // except if there's an intersection with the right view
                    Rect rightBound = bounds.get(i + 1);
                    // intersection
                    if (bound.right + mTitlePadding > rightBound.left) {
                        bound.left = (int) (rightBound.left - w - mTitlePadding);
                        bound.right = bound.left + w;
                    }
                }
            }
        }
        // right views starting from the current position
        if (mCurrentPage < count - 1) {
            for (int i = mCurrentPage + 1; i < count; i++) {
                Rect bound = bounds.get(i);
                // if right side is outside the screen
                if (bound.right > rightClip) {
                    int w = bound.right - bound.left;
                    // try to clip to the screen (right side)
                    clipViewOnTheRight(bound, w, right);
                    // except if there's an intersection with the left view
                    Rect leftBound = bounds.get(i - 1);
                    // intersection
                    if (bound.left - mTitlePadding < leftBound.right) {
                        bound.left = (int) (leftBound.right + mTitlePadding);
                        bound.right = bound.left + w;
                    }
                }
            }
        }

        // Now draw views
        int colorTextAlpha = mColorText >>> 24;
        for (int i = 0; i < count; i++) {
            // Get the title
            Rect bound = bounds.get(i);
            // Only if one side is visible
            if (bound.left > left && bound.left < right || bound.right > left && bound.right < right) {
                boolean currentPage = i == page;
                CharSequence pageTitle = getTitle(i);

                // Only set bold if we are within bounds
                mPaintText.setFakeBoldText(currentPage && currentBold && mBoldText);

                // Draw text as unselected
                mPaintText.setColor(mColorText);
                if (currentPage && currentSelected) {
                    // Fade out/in unselected text as the selected text fades in/out
                    mPaintText.setAlpha(colorTextAlpha - (int) (colorTextAlpha * selectedPercent));
                }

                // Except if there's an intersection with the right view
                if (i < boundsSize - 1) {
                    Rect rightBound = bounds.get(i + 1);
                    // Intersection
                    if (bound.right + mTitlePadding > rightBound.left) {
                        int w = bound.right - bound.left;
                        bound.left = (int) (rightBound.left - w - mTitlePadding);
                        bound.right = bound.left + w;
                    }
                }

                canvas.drawText(pageTitle, 0, pageTitle.length(), bound.left, bound.bottom + mTopPadding, mPaintText);

                // If we are within the selected bounds draw the selected text
                if (currentPage && currentSelected) {
                    mPaintText.setColor(mColorSelected);
                    mPaintText.setAlpha((int) ((mColorSelected >>> 24) * selectedPercent));
                    canvas.drawText(pageTitle, 0, pageTitle.length(), bound.left, bound.bottom + mTopPadding,
                            mPaintText);
                }
            }
        }

        // Draw the footer line
        if (mFooterLineHeight > 0) {
            mPath.reset();
            mPath.moveTo(0, height - mFooterLineHeight / 2f);
            mPath.lineTo(width, height - mFooterLineHeight / 2f);
            mPath.close();
            canvas.drawPath(mPath, mPaintFooterLine);
        }

        if (mFooterIndicatorHeight > 0) {
            if (!currentSelected || page >= boundsSize) {
                return;
            }

            Rect underlineBounds = bounds.get(page);
            float tempHeight = mFooterIndicatorHeight * selectedPercent
                    - ((1 - selectedPercent) * mFooterIndicatorHeight) * 0.5f;

            mPath.reset();
            mPath.moveTo(underlineBounds.left - mFooterIndicatorUnderlinePadding, height - mFooterLineHeight);
            mPath.lineTo(underlineBounds.right + mFooterIndicatorUnderlinePadding, height - mFooterLineHeight);
            mPath.lineTo(underlineBounds.right + mFooterIndicatorUnderlinePadding, height - mFooterLineHeight
                    - tempHeight);
            mPath.lineTo(underlineBounds.left - mFooterIndicatorUnderlinePadding, height - mFooterLineHeight
                    - tempHeight);
            mPath.close();

            // mPaintFooterIndicator.setAlpha((int) (0xFF * selectedPercent));
            canvas.drawPath(mPath, mPaintFooterIndicator);
            mPaintFooterIndicator.setAlpha(0xFF);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull android.view.MotionEvent ev) {
        if (super.onTouchEvent(ev)) {
            return true;
        }
        if (mViewPager == null || mViewPager.getAdapter() == null || mViewPager.getAdapter().getCount() == 0) {
            return false;
        }

        int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mLastMotionX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                float x = ev.getX(activePointerIndex);
                float deltaX = x - mLastMotionX;

                if (!mIsDragging) {
                    if (Math.abs(deltaX) > mTouchSlop) {
                        mIsDragging = true;
                    }
                }

                if (mIsDragging) {
                    mLastMotionX = x;
                    if (mViewPager.isFakeDragging() || mViewPager.beginFakeDrag()) {
                        mViewPager.fakeDragBy(deltaX);
                    }
                }

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!mIsDragging) {
                    int count = mViewPager.getAdapter().getCount();
                    int width = getWidth();
                    float halfWidth = width / 2f;
                    float sixthWidth = width / 6f;
                    float leftThird = halfWidth - sixthWidth;
                    float rightThird = halfWidth + sixthWidth;
                    float eventX = ev.getX();

                    if (eventX < leftThird) {
                        if (mCurrentPage > 0) {
                            mViewPager.setCurrentItem(mCurrentPage - 1);
                            return true;
                        }
                    } else if (eventX > rightThird) {
                        if (mCurrentPage < count - 1) {
                            mViewPager.setCurrentItem(mCurrentPage + 1);
                            return true;
                        }
                    } else {
                        // middle third
                        if (mCenterItemClickListener != null) {
                            mCenterItemClickListener.onCenterItemClick(mCurrentPage);
                        }
                    }
                }

                mIsDragging = false;
                mActivePointerId = INVALID_POINTER;
                if (mViewPager.isFakeDragging()) {
                    mViewPager.endFakeDrag();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                int index = ev.getActionIndex();
                mLastMotionX = ev.getX(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int pointerIndex = ev.getActionIndex();
                int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId));
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * Set bounds for the right textView including clip padding.
     *
     * @param curViewBound current bounds.
     * @param curViewWidth width of the view.
     */
    private void clipViewOnTheRight(Rect curViewBound, float curViewWidth, int right) {
        curViewBound.right = (int) (right - mClipPadding);
        curViewBound.left = (int) (curViewBound.right - curViewWidth);
    }

    /**
     * Set bounds for the left textView including clip padding.
     *
     * @param curViewBound current bounds.
     * @param curViewWidth width of the view.
     */
    private void clipViewOnTheLeft(Rect curViewBound, float curViewWidth, int left) {
        curViewBound.left = (int) (left + mClipPadding);
        curViewBound.right = (int) (mClipPadding + curViewWidth);
    }

    /**
     * Calculate views bounds and scroll them according to the current index
     */
    private ArrayList<Rect> calculateAllBounds(Paint paint) {
        ArrayList<Rect> list = new ArrayList<>();

        if (mViewPager.getAdapter() == null) {
            return list;
        }

        // for each views (if no values then add a fake one)
        int count = mViewPager.getAdapter().getCount();
        int width = getWidth();
        int height = getHeight();
        int halfWidth = width / 2;

        for (int i = 0; i < count; i++) {
            Rect bounds = calcBounds(i, paint);
            int w = bounds.right - bounds.left;
            int h = bounds.bottom - bounds.top;
            bounds.left = (int) (halfWidth - w / 2f + (i - mCurrentPage - mPageOffset) * width);
            bounds.right = bounds.left + w;
            bounds.top = (height - h) / 2;
            bounds.bottom = bounds.top + h;
            list.add(bounds);
        }

        return list;
    }

    /**
     * Calculate the bounds for a view's title
     */
    private Rect calcBounds(int index, Paint paint) {
        // calculate the text bounds
        Rect bounds = new Rect();
        CharSequence title = getTitle(index);
        bounds.right = (int) paint.measureText(title, 0, title.length());
        bounds.bottom = (int) paint.getTextSize(); // (paint.descent() - paint.ascent());
        return bounds;
    }

    @Override
    public void setViewPager(ViewPager viewPager) {
        if (mViewPager == viewPager) {
            Log.i("PAGE.INDICATOR", "This ViewPager is already set.");
            return;
        }
        if (mViewPager != null) {
            // clear us from the old pager
            mViewPager.removeOnPageChangeListener(this);
        }
        if (viewPager.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        mViewPager = viewPager;
        mViewPager.addOnPageChangeListener(this);

        mNotifications = new int[viewPager.getAdapter().getCount()];

        invalidate();
    }

    @Override
    public void setViewPager(ViewPager view, int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }

    @Override
    public void notifyDataSetChanged() {
        invalidate();
    }

    /**
     * Set a callback listener for the center item click.
     *
     * @param listener Callback instance.
     */
    public void setOnCenterItemClickListener(OnCenterItemClickListener listener) {
        mCenterItemClickListener = listener;
    }

    @Override
    public void setCurrentItem(int item) {
        if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        if (mViewPager.getCurrentItem() != item) {
            mViewPager.setCurrentItem(item);
        }
        mCurrentPage = item;
        invalidate();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrollState = state;

        if (mListener != null) {
            mListener.onPageScrollStateChanged(state);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mCurrentPage = position;
        mPageOffset = positionOffset;
        invalidate();

        if (mListener != null) {
            mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            mCurrentPage = position;
            invalidate();
        }

        if (mListener != null) {
            mListener.onPageSelected(position);
        }
    }

    @Override
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Measure our width in whatever mode specified
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        // Determine our height
        float height;
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightSpecMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            // Calculate the text bounds
            mBounds.setEmpty();
            mBounds.bottom = (int) (mPaintText.descent() - mPaintText.ascent());
            height = mBounds.bottom - mBounds.top + mFooterLineHeight + mFooterPadding + mTopPadding + mFooterIndicatorHeight;
        }
        int measuredHeight = (int) height;

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    public int getItemsCount() {
        if (mViewPager == null) {
            return 0;
        }

        PagerAdapter adapter = mViewPager.getAdapter();
        if (adapter == null) {
            return 0;
        }

        return adapter.getCount();
    }

    private CharSequence getTitle(int index) {
        if (mViewPager.getAdapter() == null) {
            return EMPTY_TITLE;
        }

        CharSequence title = mViewPager.getAdapter().getPageTitle(index);
        if (title == null) {
            title = EMPTY_TITLE;
        }

        if (mNotifications == null || index > mNotifications.length - 1) { // just to make sure
            return title;
        }

        return mNotifications[index] > 0 ? title + "  " + mNotifications[index] : title; // ■
    }

    public void showNotification(int index, int count) {
        if (mNotifications == null || index > mNotifications.length - 1) { // just to make sure
            return;
        }

        if (mNotifications[index] != count) {
            mNotifications[index] = count;
            notifyDataSetChanged();
        }
    }

    public int getNotificationCount(int index) {
        if (mNotifications == null || index > mNotifications.length - 1) { // just to make sure
            return 0;
        }

        return mNotifications[index];
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mCurrentPage = savedState.currentPage;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPage = mCurrentPage;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPage;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPage = in.readInt();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPage);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
