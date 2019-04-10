/*
 * Copyright (C) 2011 Jake Wharton
 * Copyright (C) 2011 Patrik Akerfeldt
 * Copyright (C) 2011 Francisco Figueiredo Jr.
 * Copyright (C) 2019 Eduard Scarlat
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
package ro.edi.util.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.NonNull
import androidx.viewpager.widget.ViewPager
import ro.edi.novelty.R
import java.util.*
import timber.log.Timber.i as logi

/**
 * A TitlePageIndicator is a PageIndicator which displays the title of left view (if exist), the title of the current
 * select view (centered) and the title of the right view (if exist). When the user scrolls the ViewPager then titles
 * are also scrolled.
 */
open class TitlePageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle), PageIndicator {

    private var mViewPager: ViewPager? = null
    private var mListener: ViewPager.OnPageChangeListener? = null
    private var mCurrentPage = -1
    private var mPageOffset: Float = 0.toFloat()
    private var mScrollState: Int = 0
    private val mPaintText = Paint()
    private val mBoldText: Boolean
    private val mColorText: Int
    private val mColorSelected: Int
    private val mPath = Path()
    private val mBounds = Rect()
    private val mPaintFooterLine = Paint()
    private val mPaintFooterIndicator = Paint()
    private val mFooterIndicatorHeight: Float
    private val mFooterIndicatorUnderlinePadding: Float
    private val mFooterPadding: Float
    private val mTitlePadding: Float
    private val mTopPadding: Float
    /**
     * Left and right side padding for not active view titles.
     */
    private val mClipPadding: Float
    private val mFooterLineHeight: Float

    private val mTouchSlop: Int
    private var mLastMotionX = -1f
    private var mActivePointerId = INVALID_POINTER
    private var mIsDragging: Boolean = false

    private var mCenterItemClickListener: OnCenterItemClickListener? = null

    private var mNotifications: IntArray? = null

    val itemsCount: Int
        get() {
            if (mViewPager == null) {
                return 0
            }

            val adapter = mViewPager!!.adapter ?: return 0

            return adapter.count
        }

    /**
     * Interface for a callback when the center item has been clicked.
     */
    interface OnCenterItemClickListener {
        /**
         * Callback when the center item has been clicked.
         *
         * @param position Position of the current center item.
         */
        fun onCenterItemClick(position: Int)
    }

    init {
        // TODO clean this up
        // Load defaults from resources
        val res = resources
        val defaultFooterColor = res.getColor(R.color.default_title_indicator_footer_color)
        val defaultFooterLineHeight = res.getDimension(R.dimen.default_title_indicator_footer_line_height)
        val defaultFooterIndicatorHeight = res.getDimension(R.dimen.default_title_indicator_footer_indicator_height)
        val defaultFooterIndicatorUnderlinePadding = res
            .getDimension(R.dimen.default_title_indicator_footer_indicator_underline_padding)
        val defaultFooterPadding = res.getDimension(R.dimen.default_title_indicator_footer_padding)
        val defaultSelectedColor = res.getColor(R.color.default_title_indicator_selected_color)
        val defaultSelectedBold = res.getBoolean(R.bool.default_title_indicator_selected_bold)
        val defaultTextColor = res.getColor(R.color.default_title_indicator_text_color)
        val defaultTextSize = res.getDimension(R.dimen.default_title_indicator_text_size)
        val defaultTitlePadding = res.getDimension(R.dimen.default_title_indicator_title_padding)
        val defaultClipPadding = res.getDimension(R.dimen.default_title_indicator_clip_padding)
        val defaultTopPadding = res.getDimension(R.dimen.default_title_indicator_top_padding)

        // Retrieve styles attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.TitlePageIndicator, defStyle, 0)

        // Retrieve the colors to be used for this view and apply them.
        mFooterLineHeight = a.getDimension(R.styleable.TitlePageIndicator_footerLineHeight, defaultFooterLineHeight)
        mFooterIndicatorHeight = a.getDimension(
            R.styleable.TitlePageIndicator_footerIndicatorHeight,
            defaultFooterIndicatorHeight
        )
        mFooterIndicatorUnderlinePadding = a.getDimension(
            R.styleable.TitlePageIndicator_footerIndicatorUnderlinePadding, defaultFooterIndicatorUnderlinePadding
        )
        mFooterPadding = a.getDimension(R.styleable.TitlePageIndicator_footerPadding, defaultFooterPadding)
        mTopPadding = a.getDimension(R.styleable.TitlePageIndicator_topPadding, defaultTopPadding)
        mTitlePadding = a.getDimension(R.styleable.TitlePageIndicator_titlePadding, defaultTitlePadding)
        mClipPadding = a.getDimension(R.styleable.TitlePageIndicator_clipPadding, defaultClipPadding)
        mColorSelected = a.getColor(R.styleable.TitlePageIndicator_selectedColor, defaultSelectedColor)
        mColorText = a.getColor(R.styleable.TitlePageIndicator_textColor, defaultTextColor)
        mBoldText = a.getBoolean(R.styleable.TitlePageIndicator_selectedBold, defaultSelectedBold)

        val textSize = a.getDimension(R.styleable.TitlePageIndicator_textSize, defaultTextSize)
        val footerColor = a.getColor(R.styleable.TitlePageIndicator_footerColor, defaultFooterColor)
        mPaintText.textSize = textSize
        mPaintText.isAntiAlias = true
        mPaintText.isDither = true
        mPaintText.typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        mPaintFooterLine.style = Paint.Style.FILL_AND_STROKE

        mPaintFooterLine.strokeWidth = mFooterLineHeight
        mPaintFooterLine.color = footerColor
        mPaintFooterIndicator.isDither = true

        // http://code.google.com/p/android/issues/detail?id=24873
        // bug when enabling hardware acceleration

        // workaround #1: disable hw acceleration => the drawing will be rough
        // setLayerType(View.LAYER_TYPE_SOFTWARE, mPaintFooterIndicator);

        // workaround #2: avoid using 0 as stroke width => the drawing will be smooth as butter
        mPaintFooterIndicator.strokeWidth = 1f

        mPaintFooterIndicator.style = Paint.Style.FILL_AND_STROKE
        mPaintFooterIndicator.color = footerColor

        a.recycle()

        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledPagingTouchSlop
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mViewPager == null || mViewPager!!.adapter == null) {
            return
        }

        val count = mViewPager!!.adapter!!.count
        if (count == 0) {
            return
        }

        // mCurrentPage is -1 on first start and after orientation changed.
        // if so, retrieve the correct index from viewpager.
        if (mCurrentPage == -1 && mViewPager != null) {
            mCurrentPage = mViewPager!!.currentItem
        }

        // calculate views bounds
        val bounds = calculateAllBounds(mPaintText)
        val boundsSize = bounds.size

        // make sure we're on a page that still exists
        if (mCurrentPage >= boundsSize) {
            setCurrentItem(boundsSize - 1)
            return
        }

        val left = left
        val leftClip = left + mClipPadding
        val width = width
        val height = height
        // float halfWidth = width / 2f;
        val right = left + width
        val rightClip = right - mClipPadding

        var page = mCurrentPage
        val offsetPercent: Float
        if (mPageOffset <= 0.5) {
            offsetPercent = mPageOffset
        } else {
            page += 1
            offsetPercent = 1 - mPageOffset
        }
        val currentSelected = offsetPercent <= SELECTION_FADE_PERCENTAGE
        val currentBold = offsetPercent <= BOLD_FADE_PERCENTAGE
        val selectedPercent = (SELECTION_FADE_PERCENTAGE - offsetPercent) / SELECTION_FADE_PERCENTAGE

        // Verify if the current view must be clipped to the screen
        val curPageBound = bounds[mCurrentPage]
        val curPageWidth = (curPageBound.right - curPageBound.left).toFloat()
        if (curPageBound.left < leftClip) {
            // Try to clip to the screen (left side)
            clipViewOnTheLeft(curPageBound, curPageWidth, left)
        }
        if (curPageBound.right > rightClip) {
            // Try to clip to the screen (right side)
            clipViewOnTheRight(curPageBound, curPageWidth, right)
        }

        // left views starting from the current position
        if (mCurrentPage > 0) {
            for (i in mCurrentPage - 1 downTo 0) {
                val bound = bounds[i]
                // if left side is outside the screen
                if (bound.left < leftClip) {
                    val w = bound.right - bound.left
                    // try to clip to the screen (left side)
                    clipViewOnTheLeft(bound, w.toFloat(), left)
                    // except if there's an intersection with the right view
                    val rightBound = bounds[i + 1]
                    // intersection
                    if (bound.right + mTitlePadding > rightBound.left) {
                        bound.left = (rightBound.left.toFloat() - w.toFloat() - mTitlePadding).toInt()
                        bound.right = bound.left + w
                    }
                }
            }
        }
        // right views starting from the current position
        if (mCurrentPage < count - 1) {
            for (i in mCurrentPage + 1 until count) {
                val bound = bounds[i]
                // if right side is outside the screen
                if (bound.right > rightClip) {
                    val w = bound.right - bound.left
                    // try to clip to the screen (right side)
                    clipViewOnTheRight(bound, w.toFloat(), right)
                    // except if there's an intersection with the left view
                    val leftBound = bounds[i - 1]
                    // intersection
                    if (bound.left - mTitlePadding < leftBound.right) {
                        bound.left = (leftBound.right + mTitlePadding).toInt()
                        bound.right = bound.left + w
                    }
                }
            }
        }

        // Now draw views
        val colorTextAlpha = mColorText.ushr(24)
        for (i in 0 until count) {
            // Get the title
            val bound = bounds[i]
            // Only if one side is visible
            if (bound.left in (left + 1)..(right - 1) || bound.right in (left + 1)..(right - 1)) {
                val currentPage = i == page
                val pageTitle = getTitle(i)

                // Only set bold if we are within bounds
                mPaintText.isFakeBoldText = currentPage && currentBold && mBoldText

                // Draw text as unselected
                mPaintText.color = mColorText
                if (currentPage && currentSelected) {
                    // Fade out/in unselected text as the selected text fades in/out
                    mPaintText.alpha = colorTextAlpha - (colorTextAlpha * selectedPercent).toInt()
                }

                // Except if there's an intersection with the right view
                if (i < boundsSize - 1) {
                    val rightBound = bounds[i + 1]
                    // Intersection
                    if (bound.right + mTitlePadding > rightBound.left) {
                        val w = bound.right - bound.left
                        bound.left = (rightBound.left.toFloat() - w.toFloat() - mTitlePadding).toInt()
                        bound.right = bound.left + w
                    }
                }

                canvas.drawText(
                    pageTitle,
                    0,
                    pageTitle.length,
                    bound.left.toFloat(),
                    bound.bottom + mTopPadding,
                    mPaintText
                )

                // If we are within the selected bounds draw the selected text
                if (currentPage && currentSelected) {
                    mPaintText.color = mColorSelected
                    mPaintText.alpha = (mColorSelected.ushr(24) * selectedPercent).toInt()
                    canvas.drawText(
                        pageTitle, 0, pageTitle.length, bound.left.toFloat(), bound.bottom + mTopPadding,
                        mPaintText
                    )
                }
            }
        }

        // Draw the footer line
        if (mFooterLineHeight > 0) {
            mPath.reset()
            mPath.moveTo(0f, height - mFooterLineHeight / 2f)
            mPath.lineTo(width.toFloat(), height - mFooterLineHeight / 2f)
            mPath.close()
            canvas.drawPath(mPath, mPaintFooterLine)
        }

        if (mFooterIndicatorHeight > 0) {
            if (!currentSelected || page >= boundsSize) {
                return
            }

            val underlineBounds = bounds[page]
            val tempHeight =
                mFooterIndicatorHeight * selectedPercent - (1 - selectedPercent) * mFooterIndicatorHeight * 0.5f

            mPath.reset()
            mPath.moveTo(underlineBounds.left - mFooterIndicatorUnderlinePadding, height - mFooterLineHeight)
            mPath.lineTo(underlineBounds.right + mFooterIndicatorUnderlinePadding, height - mFooterLineHeight)
            mPath.lineTo(
                underlineBounds.right + mFooterIndicatorUnderlinePadding, height.toFloat() - mFooterLineHeight
                    - tempHeight
            )
            mPath.lineTo(
                underlineBounds.left - mFooterIndicatorUnderlinePadding, height.toFloat() - mFooterLineHeight
                    - tempHeight
            )
            mPath.close()

            // mPaintFooterIndicator.setAlpha((int) (0xFF * selectedPercent));
            canvas.drawPath(mPath, mPaintFooterIndicator)
            mPaintFooterIndicator.alpha = 0xFF
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(@NonNull ev: android.view.MotionEvent): Boolean {
        if (super.onTouchEvent(ev)) {
            return true
        }
        if (mViewPager == null || mViewPager!!.adapter == null || mViewPager!!.adapter!!.count == 0) {
            return false
        }

        val action = ev.action

        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = ev.getPointerId(0)
                mLastMotionX = ev.x
            }
            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = ev.findPointerIndex(mActivePointerId)
                val x = ev.getX(activePointerIndex)
                val deltaX = x - mLastMotionX

                if (!mIsDragging) {
                    if (Math.abs(deltaX) > mTouchSlop) {
                        mIsDragging = true
                    }
                }

                if (mIsDragging) {
                    mLastMotionX = x
                    if (mViewPager!!.isFakeDragging || mViewPager!!.beginFakeDrag()) {
                        mViewPager!!.fakeDragBy(deltaX)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (!mIsDragging) {
                    val count = mViewPager!!.adapter!!.count
                    val width = width
                    val halfWidth = width / 2f
                    val sixthWidth = width / 6f
                    val leftThird = halfWidth - sixthWidth
                    val rightThird = halfWidth + sixthWidth
                    val eventX = ev.x

                    if (eventX < leftThird) {
                        if (mCurrentPage > 0) {
                            mViewPager!!.currentItem = mCurrentPage - 1
                            return true
                        }
                    } else if (eventX > rightThird) {
                        if (mCurrentPage < count - 1) {
                            mViewPager!!.currentItem = mCurrentPage + 1
                            return true
                        }
                    } else {
                        // middle third
                        if (mCenterItemClickListener != null) {
                            mCenterItemClickListener!!.onCenterItemClick(mCurrentPage)
                        }
                    }
                }

                mIsDragging = false
                mActivePointerId = INVALID_POINTER
                if (mViewPager!!.isFakeDragging) {
                    mViewPager!!.endFakeDrag()
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                mLastMotionX = ev.getX(index)
                mActivePointerId = ev.getPointerId(index)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = ev.actionIndex
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mActivePointerId = ev.getPointerId(newPointerIndex)
                }
                mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId))
            }
            else -> {
            }
        }

        return true
    }

    /**
     * Set bounds for the right textView including clip padding.
     *
     * @param curViewBound current bounds.
     * @param curViewWidth width of the view.
     */
    private fun clipViewOnTheRight(curViewBound: Rect, curViewWidth: Float, right: Int) {
        curViewBound.right = (right - mClipPadding).toInt()
        curViewBound.left = (curViewBound.right - curViewWidth).toInt()
    }

    /**
     * Set bounds for the left textView including clip padding.
     *
     * @param curViewBound current bounds.
     * @param curViewWidth width of the view.
     */
    private fun clipViewOnTheLeft(curViewBound: Rect, curViewWidth: Float, left: Int) {
        curViewBound.left = (left + mClipPadding).toInt()
        curViewBound.right = (mClipPadding + curViewWidth).toInt()
    }

    /**
     * Calculate views bounds and scroll them according to the current index
     */
    private fun calculateAllBounds(paint: Paint): ArrayList<Rect> {
        val list = ArrayList<Rect>()

        if (mViewPager!!.adapter == null) {
            return list
        }

        // for each views (if no values then add a fake one)
        val count = mViewPager!!.adapter!!.count
        val width = width
        val height = height
        val halfWidth = width / 2

        for (i in 0 until count) {
            val bounds = calcBounds(i, paint)
            val w = bounds.right - bounds.left
            val h = bounds.bottom - bounds.top
            bounds.left = (halfWidth - w / 2f + (i.toFloat() - mCurrentPage.toFloat() - mPageOffset) * width).toInt()
            bounds.right = bounds.left + w
            bounds.top = (height - h) / 2
            bounds.bottom = bounds.top + h
            list.add(bounds)
        }

        return list
    }

    /**
     * Calculate the bounds for a view's title.
     */
    private fun calcBounds(index: Int, paint: Paint): Rect {
        // calculate the text bounds
        val bounds = Rect()
        val title = getTitle(index)
        bounds.right = paint.measureText(title, 0, title.length).toInt()
        bounds.bottom = paint.textSize.toInt() // (paint.descent() - paint.ascent());
        return bounds
    }

    override fun setViewPager(view: ViewPager) {
        if (mViewPager === view) {
            logi("This ViewPager is already set.")
            return
        }
        if (mViewPager != null) {
            // clear us from the old pager
            mViewPager!!.removeOnPageChangeListener(this)
        }
        if (view.adapter == null) {
            throw IllegalStateException("ViewPager does not have adapter instance.")
        }
        mViewPager = view
        mViewPager!!.addOnPageChangeListener(this)

        mNotifications = IntArray(view.adapter!!.count)

        invalidate()
    }

    override fun setViewPager(view: ViewPager, initialPosition: Int) {
        setViewPager(view)
        setCurrentItem(initialPosition)
    }

    override fun notifyDataSetChanged() {
        invalidate()
    }

    /**
     * Set a callback listener for the center item click.
     *
     * @param listener Callback instance.
     */
    fun setOnCenterItemClickListener(listener: OnCenterItemClickListener) {
        mCenterItemClickListener = listener
    }

    override fun setCurrentItem(item: Int) {
        if (mViewPager == null) {
            throw IllegalStateException("ViewPager has not been bound.")
        }
        if (mViewPager!!.currentItem != item) {
            mViewPager!!.currentItem = item
        }
        mCurrentPage = item
        invalidate()
    }

    override fun onPageScrollStateChanged(state: Int) {
        mScrollState = state

        if (mListener != null) {
            mListener!!.onPageScrollStateChanged(state)
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        mCurrentPage = position
        mPageOffset = positionOffset
        invalidate()

        if (mListener != null) {
            mListener!!.onPageScrolled(position, positionOffset, positionOffsetPixels)
        }
    }

    override fun onPageSelected(position: Int) {
        if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            mCurrentPage = position
            invalidate()
        }

        if (mListener != null) {
            mListener!!.onPageSelected(position)
        }
    }

    override fun setOnPageChangeListener(listener: ViewPager.OnPageChangeListener) {
        mListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Measure our width in whatever mode specified
        val measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec)

        // Determine our height
        val height: Float
        val heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec)
        if (heightSpecMode == View.MeasureSpec.EXACTLY) {
            // We were told how big to be
            height = View.MeasureSpec.getSize(heightMeasureSpec).toFloat()
        } else {
            // Calculate the text bounds
            mBounds.setEmpty()
            mBounds.bottom = (mPaintText.descent() - mPaintText.ascent()).toInt()
            height =
                (mBounds.bottom - mBounds.top).toFloat() + mFooterLineHeight + mFooterPadding + mTopPadding + mFooterIndicatorHeight
        }
        val measuredHeight = height.toInt()

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun getTitle(index: Int): CharSequence {
        if (mViewPager!!.adapter == null) {
            return EMPTY_TITLE
        }

        var title = mViewPager!!.adapter!!.getPageTitle(index)
        if (title == null) {
            title = EMPTY_TITLE
        }

        if (mNotifications == null || index > mNotifications!!.size - 1) { // just to make sure
            return title
        }

        return if (mNotifications!![index] > 0) title.toString() + "  " + mNotifications!![index] else title // ■
    }

    fun showNotification(index: Int, count: Int) {
        if (mNotifications == null || index > mNotifications!!.size - 1) { // just to make sure
            return
        }

        if (mNotifications!![index] != count) {
            mNotifications!![index] = count
            notifyDataSetChanged()
        }
    }

    fun getNotificationCount(index: Int): Int {
        return if (mNotifications == null || index > mNotifications!!.size - 1) { // just to make sure
            0
        } else mNotifications!![index]

    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        mCurrentPage = savedState.currentPage
        requestLayout()
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        superState?.let {
            val savedState = SavedState(superState)
            savedState.currentPage = mCurrentPage
            return savedState
        }
        return null
    }

    internal class SavedState : View.BaseSavedState {
        var currentPage: Int = 0

        constructor(superState: Parcelable) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            currentPage = `in`.readInt()
        }

        override fun writeToParcel(@NonNull dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(currentPage)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        /**
         * Percentage indicating what percentage of the screen width away from center should the underline be fully faded. A
         * value of 0.25 means that halfway between the center of the screen and an edge.
         */
        private const val SELECTION_FADE_PERCENTAGE = 1f

        /**
         * Percentage indicating what percentage of the screen width away from center should the selected text bold turn
         * off. A value of 0.05 means that 10% between the center and an edge.
         */
        private const val BOLD_FADE_PERCENTAGE = 0.5f

        /**
         * Title text used when no title is provided by the adapter.
         */
        private const val EMPTY_TITLE = ""

        private const val INVALID_POINTER = -1
    }
}
