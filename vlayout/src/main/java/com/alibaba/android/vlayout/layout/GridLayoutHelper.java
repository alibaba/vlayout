/*
 * MIT License
 *
 * Copyright (c) 2016 Alibaba Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.alibaba.android.vlayout.layout;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.OrientationHelperEx;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutParams;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import android.support.annotation.NonNull;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;


/**
 * LayoutHelper provides GridLayout. The difference with {@link ColumnLayoutHelper} is that this layoutHelper can layout and recycle child views one line by one line.
 *
 * @author villadora
 * @since 1.0.0
 */
public class GridLayoutHelper extends BaseLayoutHelper {
    private static final String TAG = "GridLayoutHelper";

    @SuppressWarnings("FieldCanBeLocal")
    private static boolean DEBUG = false;

    private int mSpanCount = 4;

    @SuppressWarnings("FieldCanBeLocal")
    private int mSizePerSpan = 0;


    private int mTotalSize = 0;

    private boolean mIsAutoExpand = true;

    private boolean mIgnoreExtra = false;

    @NonNull
    private SpanSizeLookup mSpanSizeLookup = new DefaultSpanSizeLookup();

    private int mVGap = 0;
    private int mHGap = 0;


    private float[] mWeights = new float[0];


    private View[] mSet;

    /**
     * store index of each span
     */
    private int[] mSpanIndices;

    /**
     * store size of each span when {@link #mWeights} is not empty
     */
    private int[] mSpanCols;

    /**
     * @param spanCount number of columns/rows in grid, must be greater than 0
     */
    public GridLayoutHelper(int spanCount) {
        this(spanCount, -1, -1);
    }

    /**
     * @param spanCount number of columns/rows in grid, must be greater than 0
     * @param itemCount number of items in this layoutHelper
     */
    public GridLayoutHelper(int spanCount, int itemCount) {
        this(spanCount, itemCount, 0);
    }

    public GridLayoutHelper(int spanCount, int itemCount, int gap) {
        this(spanCount, itemCount, gap, gap);
    }

    /**
     * @param spanCount number of columns/rows in grid, must be greater than 0
     * @param itemCount number of items in this layoutHelper
     * @param vGap      vertical gap
     * @param hGap      horizontal gap
     */
    public GridLayoutHelper(int spanCount, int itemCount, int vGap, int hGap) {
        setSpanCount(spanCount);
        mSpanSizeLookup.setSpanIndexCacheEnabled(true);

        setItemCount(itemCount);
        setVGap(vGap);
        setHGap(hGap);
    }


    public void setWeights(float[] weights) {
        if (weights != null) {
            this.mWeights = Arrays.copyOf(weights, weights.length);
        } else {
            this.mWeights = new float[0];
        }
    }

    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        if (spanSizeLookup != null) {
            // TODO: handle reverse layout?
            spanSizeLookup.setStartPosition(mSpanSizeLookup.getStartPosition());

            this.mSpanSizeLookup = spanSizeLookup;
        }
    }

    public void setAutoExpand(boolean isAutoExpand) {
        this.mIsAutoExpand = isAutoExpand;
    }

    public void setIgnoreExtra(boolean ignoreExtra) {
        this.mIgnoreExtra = ignoreExtra;
    }


    /**
     * {@inheritDoc}
     * Set SpanCount for grid
     *
     * @param spanCount grid column number, must be greater than 0. {@link IllegalArgumentException}
     *                  will be thrown otherwise
     */
    public void setSpanCount(int spanCount) {
        if (spanCount == mSpanCount) {
            return;
        }
        if (spanCount < 1) {
            throw new IllegalArgumentException("Span count should be at least 1. Provided "
                    + spanCount);
        }
        mSpanCount = spanCount;
        mSpanSizeLookup.invalidateSpanIndexCache();

        ensureSpanCount();
    }

    public int getVGap() {
        return mVGap;
    }

    public int getHGap() {
        return mHGap;
    }

    public int getSpanCount() {
        return mSpanCount;
    }

    /**
     * {@inheritDoc}
     *
     * @param start start position of items handled by this layoutHelper
     * @param end   end position of items handled by this layoutHelper, if end < start, it will throw {@link IllegalArgumentException}
     */
    @Override
    public void onRangeChange(int start, int end) {
        mSpanSizeLookup.setStartPosition(start);
        mSpanSizeLookup.invalidateSpanIndexCache();
    }


    public void setGap(int gap) {
        setVGap(gap);
        setHGap(gap);
    }

    public void setVGap(int vGap) {
        if (vGap < 0) vGap = 0;
        this.mVGap = vGap;
    }

    public void setHGap(int hGap) {
        if (hGap < 0) hGap = 0;
        this.mHGap = hGap;
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        boolean isStartLine = false, isEndLine = false;
        final int currentPosition = layoutState.getCurrentPosition();
        final boolean isOverLapMargin = helper.isEnableMarginOverLap();

        final int itemDirection = layoutState.getItemDirection();
        final boolean layingOutInPrimaryDirection =
            itemDirection == LayoutStateWrapper.ITEM_DIRECTION_TAIL;

        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        if (layoutInVertical) {
            mTotalSize = helper.getContentWidth() - helper.getPaddingRight() - helper.getPaddingLeft() - getHorizontalMargin() - getHorizontalPadding();
            mSizePerSpan = (int) ((mTotalSize - (mSpanCount - 1) * mHGap) * 1.0f / mSpanCount + 0.5f);
        } else {
            mTotalSize = helper.getContentHeight() - helper.getPaddingBottom() - helper.getPaddingTop() - getVerticalMargin() - getVerticalPadding();
            mSizePerSpan = (int) ((mTotalSize - (mSpanCount - 1) * mVGap) * 1.0f / mSpanCount + 0.5f);
        }


        int count = 0;
        int consumedSpanCount = 0;
        int remainingSpan = mSpanCount;


        ensureSpanCount();


        if (!layingOutInPrimaryDirection) {
            // fill the remaining spacing this row
            int itemSpanIndex = getSpanIndex(recycler, state, layoutState.getCurrentPosition());
            int itemSpanSize = getSpanSize(recycler, state, layoutState.getCurrentPosition());


            remainingSpan = itemSpanIndex + itemSpanSize;

            // should find the last element of this row
            if (itemSpanIndex != mSpanCount - 1) {
                int index = layoutState.getCurrentPosition();
                int revRemainingSpan = mSpanCount - remainingSpan;
                while (count < mSpanCount && revRemainingSpan > 0) {
                    // go reverse direction to find views fill current row
                    index -= itemDirection;
                    if (isOutOfRange(index)) {
                        break;
                    }
                    final int spanSize = getSpanSize(recycler, state, index);
                    if (spanSize > mSpanCount) {
                        throw new IllegalArgumentException("Item at position " + index + " requires " +
                            spanSize + " spans but GridLayoutManager has only " + mSpanCount
                            + " spans.");
                    }

                    View view = layoutState.retrieve(recycler, index);
                    if (view == null)
                        break;

                    if (!isStartLine) {
                        isStartLine = helper.getReverseLayout() ? index == getRange().getUpper() : index == getRange().getLower();
                    }

                    if (!isEndLine) {
                        isEndLine = helper.getReverseLayout() ? index == getRange().getLower() : index == getRange().getUpper();
                    }

                    revRemainingSpan -= spanSize;
                    if (revRemainingSpan < 0)
                        break;


                    consumedSpanCount += spanSize;
                    mSet[count] = view;
                    count++;
                }

                if (count > 0) {
                    // reverse array
                    int s = 0, e = count - 1;
                    while (s < e) {
                        View temp = mSet[s];
                        mSet[s] = mSet[e];
                        mSet[e] = temp;
                        s++;
                        e--;
                    }
                }
            }
        }

        while (count < mSpanCount && layoutState.hasMore(state) && remainingSpan > 0) {
            int pos = layoutState.getCurrentPosition();
            if (isOutOfRange(pos)) {
                if (DEBUG)
                    Log.d(TAG, "pos [" + pos + "] is out of range");
                break;
            }

            final int spanSize = getSpanSize(recycler, state, pos);
            if (spanSize > mSpanCount) {
                throw new IllegalArgumentException("Item at position " + pos + " requires " +
                    spanSize + " spans but GridLayoutManager has only " + mSpanCount
                    + " spans.");
            }
            remainingSpan -= spanSize;
            if (remainingSpan < 0) {
                break; // item did not fit into this row or column
            }

            View view = layoutState.next(recycler);
            if (view == null) {
                break;
            }

            if (!isStartLine) {
                isStartLine = helper.getReverseLayout() ? pos == getRange().getUpper().intValue() : pos == getRange().getLower().intValue();
            }

            if (!isEndLine) {
                isEndLine = helper.getReverseLayout() ? pos == getRange().getLower().intValue() : pos == getRange().getUpper().intValue();
            }

            consumedSpanCount += spanSize;
            mSet[count] = view;
            count++;
        }


        if (count == 0) {
            return;
        }

        int maxSize = 0;


        // we should assign spans before item decor offsets are calculated
        assignSpans(recycler, state, count, consumedSpanCount, layingOutInPrimaryDirection, helper);

        if (remainingSpan > 0 && (count == consumedSpanCount) && mIsAutoExpand) {
            //autoExpand only support when each cell occupy one span.
            if (layoutInVertical) {
                mSizePerSpan = (mTotalSize - (count - 1) * mHGap) / count;
            } else {
                mSizePerSpan = (mTotalSize - (count - 1) * mVGap) / count;
            }
        } else if (!layingOutInPrimaryDirection && remainingSpan == 0 && (count == consumedSpanCount) && mIsAutoExpand) {
            //autoExpand only support when each cell occupy one span.
            if (layoutInVertical) {
                mSizePerSpan = (mTotalSize - (count - 1) * mHGap) / count;
            } else {
                mSizePerSpan = (mTotalSize - (count - 1) * mVGap) / count;
            }
        }


        boolean weighted = false;
        if (mWeights != null && mWeights.length > 0) {
            weighted = true;
            int totalSpace;
            if (layoutInVertical) {
                totalSpace = mTotalSize - (count - 1) * mHGap;
            } else {
                totalSpace = mTotalSize - (count - 1) * mVGap;
            }

            // calculate width with weight in percentage

            int eqCnt = 0, remainingSpace = totalSpace;
            int colCnt = (remainingSpan > 0 && mIsAutoExpand) ? count : mSpanCount;
            for (int i = 0; i < colCnt; i++) {
                if (i < mWeights.length && !Float.isNaN(mWeights[i]) && mWeights[i] >= 0) {
                    float weight = mWeights[i];
                    mSpanCols[i] = (int) (weight * 1.0f / 100 * totalSpace + 0.5f);
                    remainingSpace -= mSpanCols[i];
                } else {
                    eqCnt++;
                    mSpanCols[i] = -1;
                }
            }

            if (eqCnt > 0) {
                int eqLength = remainingSpace / eqCnt;
                for (int i = 0; i < colCnt; i++) {
                    if (mSpanCols[i] < 0) {
                        mSpanCols[i] = eqLength;
                    }
                }
            }
        }


        for (int i = 0; i < count; i++) {
            View view = mSet[i];
            helper.addChildView(layoutState, view, layingOutInPrimaryDirection ? -1 : 0);

            int spanSize = getSpanSize(recycler, state, helper.getPosition(view)), spec;
            if (weighted) {
                final int index = mSpanIndices[i];
                int spanLength = 0;
                for (int j = 0; j < spanSize; j++) {
                    spanLength += mSpanCols[j + index];
                }

                spec = View.MeasureSpec.makeMeasureSpec(Math.max(0, spanLength), View.MeasureSpec.EXACTLY);
            } else {
                spec = View.MeasureSpec.makeMeasureSpec(mSizePerSpan * spanSize +
                        Math.max(0, spanSize - 1) * (layoutInVertical ? mHGap : mVGap),
                    View.MeasureSpec.EXACTLY);
            }
            final VirtualLayoutManager.LayoutParams lp = (VirtualLayoutManager.LayoutParams) view.getLayoutParams();

            if (helper.getOrientation() == VERTICAL) {
                helper.measureChildWithMargins(view, spec, getMainDirSpec(lp.height, mTotalSize,
                    View.MeasureSpec.getSize(spec), lp.mAspectRatio));
            } else {
                helper.measureChildWithMargins(view,
                    getMainDirSpec(lp.width, mTotalSize, View.MeasureSpec.getSize(spec),
                        lp.mAspectRatio), View.MeasureSpec.getSize(spec));
            }
            final int size = orientationHelper.getDecoratedMeasurement(view);
            if (size > maxSize) {
                maxSize = size;
            }
        }

        // views that did not measure the maxSize has to be re-measured
        final int maxMeasureSpec = getMainDirSpec(maxSize, mTotalSize, 0, Float.NaN);
        for (int i = 0; i < count; i++) {
            final View view = mSet[i];
            if (orientationHelper.getDecoratedMeasurement(view) != maxSize) {
                int spanSize = getSpanSize(recycler, state, helper.getPosition(view)), spec;
                if (weighted) {
                    final int index = mSpanIndices[i];
                    int spanLength = 0;
                    for (int j = 0; j < spanSize; j++) {
                        spanLength += mSpanCols[j + index];
                    }

                    spec = View.MeasureSpec.makeMeasureSpec(Math.max(0, spanLength), View.MeasureSpec.EXACTLY);
                } else {
                    spec = View.MeasureSpec.makeMeasureSpec(mSizePerSpan * spanSize +
                            Math.max(0, spanSize - 1) * (layoutInVertical ? mHGap : mVGap),
                        View.MeasureSpec.EXACTLY);
                }

                if (helper.getOrientation() == VERTICAL) {
                    helper.measureChildWithMargins(view, spec, maxMeasureSpec);
                } else {
                    helper.measureChildWithMargins(view, maxMeasureSpec, spec);
                }
            }
        }

        int startSpace = 0, endSpace = 0;

        if (isStartLine) {
            startSpace = computeStartSpace(helper, layoutInVertical, !helper.getReverseLayout(), isOverLapMargin);
        }

        if (isEndLine) {
            endSpace = computeEndSpace(helper, layoutInVertical, !helper.getReverseLayout(), isOverLapMargin);
        }


        result.mConsumed = maxSize + startSpace + endSpace;

        final boolean layoutStart = layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START;
        if (!mLayoutWithAnchor && (!isEndLine || !layoutStart) && (!isStartLine || layoutStart)) {
            result.mConsumed += (layoutInVertical ? mVGap : mHGap);
        }


        int left = 0, right = 0, top = 0, bottom = 0;
        if (layoutInVertical) {
            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                bottom = layoutState.getOffset() - endSpace - ((mLayoutWithAnchor || isEndLine) ? 0 : mVGap);
                top = bottom - maxSize;
            } else {
                top = layoutState.getOffset() + startSpace + ((mLayoutWithAnchor || isStartLine) ? 0 : mVGap);
                bottom = top + maxSize;
            }
        } else {
            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                right = layoutState.getOffset() - endSpace - (mLayoutWithAnchor || isEndLine ? 0 : mHGap);
                left = right - maxSize;
            } else {
                left = layoutState.getOffset() + startSpace + (mLayoutWithAnchor || isStartLine ? 0 : mHGap);
                right = left + maxSize;
            }
        }

        for (int i = 0; i < count; i++) {
            View view = mSet[i];
            final int index = mSpanIndices[i];

            LayoutParams params = (LayoutParams) view.getLayoutParams();
            if (layoutInVertical) {
                if (weighted) {
                    left = helper.getPaddingLeft() + mMarginLeft + mPaddingLeft;
                    for (int j = 0; j < index; j++) {
                        left += mSpanCols[j] + mHGap;
                    }
                } else {
                    left = helper.getPaddingLeft() + mMarginLeft + mPaddingLeft + mSizePerSpan * index + index * mHGap;
                }

                right = left + orientationHelper.getDecoratedMeasurementInOther(view);
            } else {

                if (weighted) {
                    top = helper.getPaddingTop() + mMarginTop + mPaddingTop;
                    for (int j = 0; j < index; j++) {
                        top += mSpanCols[j] + mVGap;
                    }
                } else {
                    top = helper.getPaddingTop() + mMarginTop + mPaddingTop
                        + mSizePerSpan * index + index * mVGap;
                }

                bottom = top + orientationHelper.getDecoratedMeasurementInOther(view);
            }

            if (DEBUG) {
                Log.d(TAG, "layout item in position: " + params.getViewPosition() + " with text " + ((TextView) view).getText() + " with SpanIndex: " + index + " into (" +
                    left + ", " + top + ", " + right + ", " + bottom + " )");
            }

            // We calculate everything with View's bounding box (which includes decor and margins)
            // To calculate correct layout position, we subtract margins.
            // modified by huifeng at 20160907, margins are already subtracted
            layoutChildWithMargin(view, left, top, right, bottom, helper);

            // Consume the available space if the view is not removed OR changed
            if (params.isItemRemoved() || params.isItemChanged()) {
                result.mIgnoreConsumed = true;
            }

            result.mFocusable |= view.isFocusable();
        }


        mLayoutWithAnchor = false;
        Arrays.fill(mSet, null);
        Arrays.fill(mSpanIndices, 0);
        Arrays.fill(mSpanCols, 0);
    }


    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        if (isLayoutEnd) {
            if (offset == getItemCount() - 1) {
                return layoutInVertical ? mMarginBottom + mPaddingBottom : mMarginRight + mPaddingRight;
            }
        } else {
            if (offset == 0) {
                return layoutInVertical ? -mMarginTop - mPaddingTop : -mMarginLeft - mPaddingLeft;
            }
        }

        return super.computeAlignOffset(offset, isLayoutEnd, useAnchor, helper);
    }

    @Override
    public void onClear(LayoutManagerHelper helper) {
        super.onClear(helper);
        mSpanSizeLookup.invalidateSpanIndexCache();
    }

    @Override
    public void onItemsChanged(LayoutManagerHelper helper) {
        super.onItemsChanged(helper);
        mSpanSizeLookup.invalidateSpanIndexCache();
    }

    private static final int MAIN_DIR_SPEC =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

    private int getMainDirSpec(int dim, int otherSize, int viewSize, float viewAspectRatio) {
        if (!Float.isNaN(viewAspectRatio) && viewAspectRatio > 0 && viewSize > 0) {
            return View.MeasureSpec.makeMeasureSpec((int) (viewSize / viewAspectRatio + 0.5f), View.MeasureSpec.EXACTLY);
        } else if (!Float.isNaN(mAspectRatio) && mAspectRatio > 0) {
            return View.MeasureSpec.makeMeasureSpec((int) (otherSize / mAspectRatio + 0.5f), View.MeasureSpec.EXACTLY);
        } else if (dim < 0) {
            return MAIN_DIR_SPEC;
        } else {
            return View.MeasureSpec.makeMeasureSpec(dim, View.MeasureSpec.EXACTLY);
        }
    }


    private void ensureSpanCount() {

        if (mSet == null || mSet.length != mSpanCount) {
            mSet = new View[mSpanCount];
        }

        if (mSpanIndices == null || mSpanIndices.length != mSpanCount) {
            mSpanIndices = new int[mSpanCount];
        }

        if (mSpanCols == null || mSpanCols.length != mSpanCount) {
            mSpanCols = new int[mSpanCount];
        }
    }


    private boolean mLayoutWithAnchor = false;

    @Override
    public void checkAnchorInfo(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {
        if (state.getItemCount() > 0 && !state.isPreLayout()) {
            int span = mSpanSizeLookup.getCachedSpanIndex(anchorInfo.position, mSpanCount);
            if (anchorInfo.layoutFromEnd) {
                while (span < mSpanCount - 1 && anchorInfo.position < getRange().getUpper()) {
                    anchorInfo.position++;
                    span = mSpanSizeLookup.getCachedSpanIndex(anchorInfo.position, mSpanCount);
                }
            } else {
                while (span > 0 && anchorInfo.position > 0) {
                    anchorInfo.position--;
                    span = mSpanSizeLookup.getCachedSpanIndex(anchorInfo.position, mSpanCount);
                }
            }

            mLayoutWithAnchor = true;

/*
            if (anchorInfo.position == getRange().getLower() || anchorInfo.position == getRange().getUpper()) {
                return;
            }

            boolean layoutInVertical = helper.getOrientation() == VERTICAL;
            if (anchorInfo.layoutFromEnd) {
                anchorInfo.coordinate += layoutInVertical ? mVGap : mHGap;
            } else {
                anchorInfo.coordinate -= layoutInVertical ? mVGap : mHGap;
            }
 */

        }
    }


    private int getSpanIndex(RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return mSpanSizeLookup.getCachedSpanIndex(pos, mSpanCount);
        }

        final int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            return 0;
        }
        return mSpanSizeLookup.getCachedSpanIndex(adapterPosition, mSpanCount);
    }


    private int getSpanSize(RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return mSpanSizeLookup.getSpanSize(pos);
        }

        final int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            return 0;
        }

        return mSpanSizeLookup.getSpanSize(adapterPosition);
    }

    private void assignSpans(RecyclerView.Recycler recycler, RecyclerView.State state, int count,
                             int consumedSpanCount, boolean layingOutInPrimaryDirection, LayoutManagerHelper helper) {
        int span, spanDiff, start, end, diff;
        // make sure we traverse from min position to max position
        if (layingOutInPrimaryDirection) {
            start = 0;
            end = count;
            diff = 1;
        } else {
            start = count - 1;
            end = -1;
            diff = -1;
        }

        if (helper.getOrientation() == VERTICAL && helper.isDoLayoutRTL()) { // start from last span
            span = consumedSpanCount - 1;
            spanDiff = -1;
        } else {
            span = 0;
            spanDiff = 1;
        }

        for (int i = start; i != end; i += diff) {
            View view = mSet[i];
            int spanSize = getSpanSize(recycler, state, helper.getPosition(view));
            if (spanDiff == -1 && spanSize > 1) {
                mSpanIndices[i] = span - (spanSize - 1);
            } else {
                mSpanIndices[i] = span;
            }
            span += spanDiff * spanSize;
        }
    }


    static final class DefaultSpanSizeLookup extends SpanSizeLookup {

        @Override
        public int getSpanSize(int position) {
            return 1;
        }

        @Override
        public int getSpanIndex(int span, int spanCount) {
            return (span - mStartPosition) % spanCount;
        }
    }


    public static abstract class SpanSizeLookup {

        final SparseIntArray mSpanIndexCache = new SparseIntArray();

        private boolean mCacheSpanIndices = false;

        int mStartPosition = 0;

        /**
         * Returns the number of span occupied by the item at <code>position</code>.
         *
         * @param position The adapter position of the item
         * @return The number of spans occupied by the item at the provided position
         */
        abstract public int getSpanSize(int position);

        /**
         * Sets whether the results of {@link #getSpanIndex(int, int)} method should be cached or
         * not. By default these values are not cached. If you are not overriding
         * {@link #getSpanIndex(int, int)}, you should set this to true for better performance.
         *
         * @param cacheSpanIndices Whether results of getSpanIndex should be cached or not.
         */
        public void setSpanIndexCacheEnabled(boolean cacheSpanIndices) {
            mCacheSpanIndices = cacheSpanIndices;
        }

        public void setStartPosition(int startPosition) {
            this.mStartPosition = startPosition;
        }

        public int getStartPosition() {
            return this.mStartPosition;
        }

        /**
         * Clears the span index cache. GridLayoutManager automatically calls this method when
         * adapter changes occur.
         */
        public void invalidateSpanIndexCache() {
            mSpanIndexCache.clear();
        }

        /**
         * Returns whether results of {@link #getSpanIndex(int, int)} method are cached or not.
         *
         * @return True if results of {@link #getSpanIndex(int, int)} are cached.
         */
        public boolean isSpanIndexCacheEnabled() {
            return mCacheSpanIndices;
        }

        int getCachedSpanIndex(int position, int spanCount) {
            if (!mCacheSpanIndices) {
                return getSpanIndex(position, spanCount);
            }
            final int existing = mSpanIndexCache.get(position, -1);
            if (existing != -1) {
                return existing;
            }
            final int value = getSpanIndex(position, spanCount);
            mSpanIndexCache.put(position, value);
            return value;
        }

        /**
         * Returns the final span index of the provided position.
         * <p/>
         * If you have a faster way to calculate span index for your items, you should override
         * this method. Otherwise, you should enable span index cache
         * ({@link #setSpanIndexCacheEnabled(boolean)}) for better performance. When caching is
         * disabled, default implementation traverses all items from 0 to
         * <code>position</code>. When caching is enabled, it calculates from the closest cached
         * value before the <code>position</code>.
         * <p/>
         * If you override this method, you need to make sure it is consistent with
         * {@link #getSpanSize(int)}. GridLayoutManager does not call this method for
         * each item. It is called only for the reference item and rest of the items
         * are assigned to spans based on the reference item. For example, you cannot assign a
         * position to span 2 while span 1 is empty.
         * <p/>
         * Note that span offsets always start with 0 and are not affected by RTL.
         *
         * @param position  The position of the item
         * @param spanCount The total number of spans in the grid
         * @return The final span position of the item. Should be between 0 (inclusive) and
         * <code>spanCount</code>(exclusive)
         */
        public int getSpanIndex(int position, int spanCount) {
            int positionSpanSize = getSpanSize(position);
            if (positionSpanSize == spanCount) {
                return 0; // quick return for full-span items
            }
            int span = 0;
            int startPos = mStartPosition;
            // If caching is enabled, try to jump
            if (mCacheSpanIndices && mSpanIndexCache.size() > 0) {
                int prevKey = findReferenceIndexFromCache(position);
                if (prevKey >= 0) {
                    span = mSpanIndexCache.get(prevKey) + getSpanSize(prevKey);
                    startPos = prevKey + 1;
                }
            }
            for (int i = startPos; i < position; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                } else if (span > spanCount) {
                    // did not fit, moving to next row / column
                    span = size;
                }
            }
            if (span + positionSpanSize <= spanCount) {
                return span;
            }
            return 0;
        }

        int findReferenceIndexFromCache(int position) {
            int lo = 0;
            int hi = mSpanIndexCache.size() - 1;

            while (lo <= hi) {
                final int mid = (lo + hi) >>> 1;
                final int midVal = mSpanIndexCache.keyAt(mid);
                if (midVal < position) {
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            int index = lo - 1;
            if (index >= 0 && index < mSpanIndexCache.size()) {
                return mSpanIndexCache.keyAt(index);
            }
            return -1;
        }

        /**
         * Returns the index of the group this position belongs.
         * <p/>
         * For example, if grid has 3 columns and each item occupies 1 span, span group index
         * for item 1 will be 0, item 5 will be 1.
         *
         * @param adapterPosition The position in adapter
         * @param spanCount       The total number of spans in the grid
         * @return The index of the span group including the item at the given adapter position
         */
        public int getSpanGroupIndex(int adapterPosition, int spanCount) {
            int span = 0;
            int group = 0;
            int positionSpanSize = getSpanSize(adapterPosition);
            for (int i = 0; i < adapterPosition; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                    group++;
                } else if (span > spanCount) {
                    // did not fit, moving to next row / column
                    span = size;
                    group++;
                }
            }
            if (span + positionSpanSize > spanCount) {
                group++;
            }
            return group;
        }
    }
}
