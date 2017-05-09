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

import java.util.Arrays;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.Range;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutParams;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;
import com.alibaba.android.vlayout.layout.GridLayoutHelper.DefaultSpanSizeLookup;
import com.alibaba.android.vlayout.layout.GridLayoutHelper.SpanSizeLookup;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;

/**
 * LayoutHelper provides GridLayout. The difference with {@link ColumnLayoutHelper} is that this layoutHelper can layout and recycle child views one line by one line.
 *
 * @author villadora
 * @since 1.0.0
 */
public class RangeGridLayoutHelper extends BaseLayoutHelper {
    private static final String TAG = "RangeGridLayoutHelper";

    @SuppressWarnings("FieldCanBeLocal")
    private static boolean DEBUG = false;

    private RangeStyle mRangeStyle;

    private int mTotalSize = 0;

    /**
     * @param spanCount number of columns/rows in grid, must be greater than 0
     */
    public RangeGridLayoutHelper(int spanCount) {
        this(spanCount, -1, -1);
    }

    /**
     * @param spanCount number of columns/rows in grid, must be greater than 0
     * @param itemCount number of items in this layoutHelper
     */
    public RangeGridLayoutHelper(int spanCount, int itemCount) {
        this(spanCount, itemCount, 0);
    }

    public RangeGridLayoutHelper(int spanCount, int itemCount, int gap) {
        this(spanCount, itemCount, gap, gap);
    }

    /**
     * @param spanCount number of columns/rows in grid, must be greater than 0
     * @param itemCount number of items in this layoutHelper
     * @param vGap      vertical gap
     * @param hGap      horizontal gap
     */
    public RangeGridLayoutHelper(int spanCount, int itemCount, int vGap, int hGap) {
        mRangeStyle = new RangeStyle(this);
        mRangeStyle.setSpanCount(spanCount);
        mRangeStyle.setVGap(vGap);
        mRangeStyle.setHGap(hGap);
        setItemCount(itemCount);
    }

    public void addRangeStyle(Range range, RangeStyle rangeStyle) {
        mRangeStyle.addChildRangeStyle(range, rangeStyle);
    }

    public RangeStyle getRootRangeStyle() {
        return mRangeStyle;
    }

    @Override
    public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        mRangeStyle.setMargin(leftMargin, topMargin, rightMargin, bottomMargin);
    }

    @Override
    public void setPadding(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        mRangeStyle.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
    }

    public void setWeights(float[] weights) {
        mRangeStyle.setWeights(weights);
    }

    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        mRangeStyle.setSpanSizeLookup(spanSizeLookup);
    }

    public void setAutoExpand(boolean isAutoExpand) {
        mRangeStyle.setAutoExpand(isAutoExpand);
    }

    public void setIgnoreExtra(boolean ignoreExtra) {
        mRangeStyle.setIgnoreExtra(ignoreExtra);
    }


    /**
     * {@inheritDoc}
     * Set SpanCount for grid
     *
     * @param spanCount grid column number, must be greater than 0. {@link IllegalArgumentException}
     *                  will be thrown otherwise
     */
    public void setSpanCount(int spanCount) {
        mRangeStyle.setSpanCount(spanCount);
    }

    public int getSpanCount() {
        return mRangeStyle.getSpanCount();
    }

    /**
     * {@inheritDoc}
     *
     * @param start start position of items handled by this layoutHelper
     * @param end   end position of items handled by this layoutHelper, if end < start, it will throw {@link IllegalArgumentException}
     */
    @Override
    public void onRangeChange(int start, int end) {
        mRangeStyle.setRange(start, end);
    }

    public void setGap(int gap) {
        setVGap(gap);
        setHGap(gap);
    }

    public void setVGap(int vGap) {
        mRangeStyle.setVGap(vGap);
    }

    public void setHGap(int hGap) {
        mRangeStyle.setHGap(hGap);
    }

    @Override
    public void setAspectRatio(float aspectRatio) {
        mRangeStyle.setAspectRatio(aspectRatio);
    }

    @Override
    public float getAspectRatio() {
        return mRangeStyle.getAspectRatio();
    }

    @Override
    public void setBgColor(int bgColor) {
        mRangeStyle.setBgColor(bgColor);
    }

    @Override
    public void setLayoutViewBindListener(LayoutViewBindListener bindListener) {
        mRangeStyle.setLayoutViewBindListener(bindListener);
    }

    @Override
    public void setLayoutViewUnBindListener(LayoutViewUnBindListener layoutViewUnBindListener) {
        mRangeStyle.setLayoutViewUnBindListener(layoutViewUnBindListener);
    }

    @Override
    public boolean requireLayoutView() {
        return mRangeStyle.requireLayoutView();
    }

    @Override
    public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
        LayoutManagerHelper helper) {
        mRangeStyle.beforeLayout(recycler, state, helper);
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        boolean isStartLine = false, isEndLine = false;
        boolean isSecondStartLine = false, isSecondEndLine = false;
        final int currentPosition = layoutState.getCurrentPosition();
        RangeStyle rangeStyle = mRangeStyle.findRangeStyleByPosition(currentPosition);

        final int itemDirection = layoutState.getItemDirection();
        final boolean layingOutInPrimaryDirection =
                itemDirection == LayoutStateWrapper.ITEM_DIRECTION_TAIL;

        OrientationHelper orientationHelper = helper.getMainOrientationHelper();

        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        if (layoutInVertical) {
            mTotalSize = helper.getContentWidth() - helper.getPaddingRight() - helper.getPaddingLeft() - rangeStyle
                .getFamilyHorizontalMargin() - rangeStyle.getFamilyHorizontalPadding();
            rangeStyle.mSizePerSpan = (int) ((mTotalSize - (
                rangeStyle.mSpanCount - 1) * rangeStyle.mHGap) * 1.0f / rangeStyle.mSpanCount + 0.5f);
        } else {
            mTotalSize = helper.getContentHeight() - helper.getPaddingBottom() - helper.getPaddingTop() - rangeStyle
                .getFamilyVerticalMargin() - rangeStyle.getFamilyVerticalPadding();
            rangeStyle.mSizePerSpan = (int) ((mTotalSize - (
                rangeStyle.mSpanCount - 1) * rangeStyle.mVGap) * 1.0f / rangeStyle.mSpanCount + 0.5f);
        }


        int count = 0;
        int consumedSpanCount = 0;
        int remainingSpan = rangeStyle.mSpanCount;

        rangeStyle.ensureSpanCount();

        if (!layingOutInPrimaryDirection) {
            // fill the remaining spacing this row
            int itemSpanIndex = getSpanIndex(rangeStyle.mSpanSizeLookup, rangeStyle.mSpanCount, recycler, state, layoutState.getCurrentPosition());
            int itemSpanSize = getSpanSize(rangeStyle.mSpanSizeLookup, recycler, state, layoutState.getCurrentPosition());


            remainingSpan = itemSpanIndex + itemSpanSize;

            // should find the last element of this row
            if (itemSpanIndex != rangeStyle.mSpanCount - 1) {
                int index = layoutState.getCurrentPosition();
                int revRemainingSpan = rangeStyle.mSpanCount - remainingSpan;
                while (count < rangeStyle.mSpanCount && revRemainingSpan > 0) {
                    // go reverse direction to find views fill current row
                    index -= itemDirection;
                    if (isOutOfRange(index)) {
                        break;
                    }
                    final int spanSize = getSpanSize(rangeStyle.mSpanSizeLookup, recycler, state, index);
                    if (spanSize > rangeStyle.mSpanCount) {
                        throw new IllegalArgumentException("Item at position " + index + " requires " +
                                spanSize + " spans but RangeGridLayoutHelper has only " + rangeStyle.mSpanCount
                                + " spans.");
                    }

                    View view = layoutState.retrieve(recycler, index);
                    if (view == null) {
                        break;
                    }

                    if (!isStartLine) {
                        isStartLine = helper.getReverseLayout() ? index == mRangeStyle.getRange().getUpper().intValue()
                            : index == mRangeStyle.getRange().getLower().intValue();
                    }
                    if (!isSecondStartLine) {
                        if (rangeStyle != mRangeStyle) {
                            isSecondStartLine = helper.getReverseLayout() ? index == rangeStyle.getRange().getUpper()
                                .intValue() : index == rangeStyle.getRange().getLower().intValue();
                        }
                    }

                    if (!isEndLine) {
                        isEndLine = helper.getReverseLayout() ? index == mRangeStyle.getRange().getLower().intValue()
                            : index == mRangeStyle.getRange().getUpper().intValue();
                    }
                    if (!isSecondEndLine) {
                        if (rangeStyle != mRangeStyle) {
                            isSecondEndLine = helper.getReverseLayout() ? index == rangeStyle.getRange().getLower()
                                .intValue() : index == rangeStyle.getRange().getUpper().intValue();
                        }
                    }

                    revRemainingSpan -= spanSize;
                    if (revRemainingSpan < 0) {
                        break;
                    }


                    consumedSpanCount += spanSize;
                    rangeStyle.mSet[count] = view;
                    count++;
                }

                if (count > 0) {
                    // reverse array
                    int s = 0, e = count - 1;
                    while (s < e) {
                        View temp = rangeStyle.mSet[s];
                        rangeStyle.mSet[s] = rangeStyle.mSet[e];
                        rangeStyle.mSet[e] = temp;
                        s++;
                        e--;
                    }
                }
            }
        }

        while (count < rangeStyle.mSpanCount && layoutState.hasMore(state) && remainingSpan > 0) {
            int pos = layoutState.getCurrentPosition();
            if (isOutOfRange(pos)) {
                if (DEBUG) {
                    Log.d(TAG, "pos [" + pos + "] is out of range");
                }
                break;
            }

            final int spanSize = getSpanSize(rangeStyle.mSpanSizeLookup, recycler, state, pos);
            if (spanSize > rangeStyle.mSpanCount) {
                throw new IllegalArgumentException("Item at position " + pos + " requires " +
                        spanSize + " spans but GridLayoutManager has only " + rangeStyle.mSpanCount
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
                isStartLine = helper.getReverseLayout() ? pos == mRangeStyle.getRange().getUpper().intValue()
                    : pos == mRangeStyle.getRange().getLower().intValue();
            }
            if (!isSecondStartLine) {
                if (rangeStyle != mRangeStyle) {
                    isSecondStartLine = helper.getReverseLayout() ? pos == rangeStyle.getRange().getUpper()
                        .intValue() : pos == rangeStyle.getRange().getLower().intValue();
                }
            }

            if (!isEndLine) {
                isEndLine = helper.getReverseLayout() ? pos == mRangeStyle.getRange().getLower().intValue()
                    : pos == mRangeStyle.getRange().getUpper().intValue();
            }

            if (!isSecondEndLine) {
                if (rangeStyle != mRangeStyle) {
                    isSecondEndLine = helper.getReverseLayout() ? pos == rangeStyle.getRange().getLower()
                        .intValue() : pos == rangeStyle.getRange().getUpper().intValue();
                }
            }

            consumedSpanCount += spanSize;
            rangeStyle.mSet[count] = view;
            count++;
        }


        if (count == 0) {
            return;
        }

        int maxSize = 0;


        // we should assign spans before item decor offsets are calculated
        assignSpans(rangeStyle, recycler, state, count, consumedSpanCount, layingOutInPrimaryDirection, helper);

        if (remainingSpan > 0 && (count == consumedSpanCount) && rangeStyle.mIsAutoExpand) {
            //autoExpand only support when each cell occupy one span.
            if (layoutInVertical) {
                rangeStyle.mSizePerSpan = (mTotalSize - (count - 1) * rangeStyle.mHGap) / count;
            } else {
                rangeStyle.mSizePerSpan = (mTotalSize - (count - 1) * rangeStyle.mVGap) / count;
            }
        }


        boolean weighted = false;
        if (rangeStyle.mWeights != null && rangeStyle.mWeights.length > 0) {
            weighted = true;
            int totalSpace;
            if (layoutInVertical) {
                totalSpace = mTotalSize - (count - 1) * rangeStyle.mHGap;
            } else {
                totalSpace = mTotalSize - (count - 1) * rangeStyle.mVGap;
            }

            // calculate width with weight in percentage

            int eqCnt = 0, remainingSpace = totalSpace;
            int colCnt = (remainingSpan > 0 && rangeStyle.mIsAutoExpand) ? count : rangeStyle.mSpanCount;
            for (int i = 0; i < colCnt; i++) {
                if (i < rangeStyle.mWeights.length && !Float.isNaN(rangeStyle.mWeights[i]) && rangeStyle.mWeights[i] >= 0) {
                    float weight = rangeStyle.mWeights[i];
                    rangeStyle.mSpanCols[i] = (int) (weight * 1.0f / 100 * totalSpace + 0.5f);
                    remainingSpace -= rangeStyle.mSpanCols[i];
                } else {
                    eqCnt++;
                    rangeStyle.mSpanCols[i] = -1;
                }
            }

            if (eqCnt > 0) {
                int eqLength = remainingSpace / eqCnt;
                for (int i = 0; i < colCnt; i++) {
                    if (rangeStyle.mSpanCols[i] < 0) {
                        rangeStyle.mSpanCols[i] = eqLength;
                    }
                }
            }
        }


        for (int i = 0; i < count; i++) {
            View view = rangeStyle.mSet[i];
            helper.addChildView(layoutState, view, layingOutInPrimaryDirection ? -1 : 0);

            int spanSize = getSpanSize(rangeStyle.mSpanSizeLookup, recycler, state, helper.getPosition(view)), spec;
            if (weighted) {
                final int index = rangeStyle.mSpanIndices[i];
                int spanLength = 0;
                for (int j = 0; j < spanSize; j++) {
                    spanLength += rangeStyle.mSpanCols[j + index];
                }

                spec = View.MeasureSpec.makeMeasureSpec(Math.max(0, spanLength), View.MeasureSpec.EXACTLY);
            } else {
                spec = View.MeasureSpec.makeMeasureSpec(rangeStyle.mSizePerSpan * spanSize +
                                Math.max(0, spanSize - 1) * (layoutInVertical ? rangeStyle.mHGap : rangeStyle.mVGap),
                        View.MeasureSpec.EXACTLY);
            }
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();

            if (helper.getOrientation() == VERTICAL) {
                helper.measureChild(view, spec, getMainDirSpec(rangeStyle, lp.height, mTotalSize,
                        View.MeasureSpec.getSize(spec), lp.mAspectRatio));
            } else {
                helper.measureChild(view,
                        getMainDirSpec(rangeStyle, lp.width, mTotalSize, View.MeasureSpec.getSize(spec),
                                lp.mAspectRatio), View.MeasureSpec.getSize(spec));
            }
            final int size = orientationHelper.getDecoratedMeasurement(view);
            if (size > maxSize) {
                maxSize = size;
            }
        }

        // views that did not measure the maxSize has to be re-measured
        final int maxMeasureSpec = getMainDirSpec(rangeStyle, maxSize, mTotalSize, 0, Float.NaN);
        for (int i = 0; i < count; i++) {
            final View view = rangeStyle.mSet[i];
            if (orientationHelper.getDecoratedMeasurement(view) != maxSize) {
                int spanSize = getSpanSize(rangeStyle.mSpanSizeLookup, recycler, state, helper.getPosition(view)), spec;
                if (weighted) {
                    final int index = rangeStyle.mSpanIndices[i];
                    int spanLength = 0;
                    for (int j = 0; j < spanSize; j++) {
                        spanLength += rangeStyle.mSpanCols[j + index];
                    }

                    spec = View.MeasureSpec.makeMeasureSpec(Math.max(0, spanLength), View.MeasureSpec.EXACTLY);
                } else {
                    spec = View.MeasureSpec.makeMeasureSpec(rangeStyle.mSizePerSpan * spanSize +
                                    Math.max(0, spanSize - 1) * (layoutInVertical ? rangeStyle.mHGap : rangeStyle.mVGap),
                            View.MeasureSpec.EXACTLY);
                }

                if (helper.getOrientation() == VERTICAL) {
                    helper.measureChild(view, spec, maxMeasureSpec);
                } else {
                    helper.measureChild(view, maxMeasureSpec, spec);
                }
            }
        }

        int startSpace = 0, endSpace = 0;

        if (isStartLine) {
            startSpace = layoutInVertical ? mRangeStyle.getMarginTop() + mRangeStyle.getPaddingTop()
                : mRangeStyle.getMarginLeft() + mRangeStyle.getPaddingLeft();
        }
        if (isSecondStartLine) {
            startSpace += (layoutInVertical ? rangeStyle.getMarginTop() + rangeStyle.getPaddingTop()
                : rangeStyle.getMarginLeft() + rangeStyle.getPaddingLeft());
        }

        if (isEndLine) {
            endSpace = layoutInVertical ? mRangeStyle.getMarginBottom() + mRangeStyle.getPaddingBottom()
                : mRangeStyle.getMarginRight() + mRangeStyle.getPaddingRight();
        }
        if (isSecondEndLine) {
            endSpace += (layoutInVertical ? rangeStyle.getMarginBottom() + rangeStyle.getPaddingBottom()
                : rangeStyle.getMarginRight() + rangeStyle.getPaddingRight());
        }


        result.mConsumed = maxSize + startSpace + endSpace;

        final boolean layoutStart = layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START;
        int consumedGap = 0;
        if (!mLayoutWithAnchor) {
            if (!layoutStart) {
                if (!isStartLine) {
                    if (isSecondStartLine) {
                        consumedGap = (layoutInVertical ? rangeStyle.mParent.mVGap : rangeStyle.mParent.mHGap);
                        //Log.d(TAG, "1 --> " + currentPosition + " add gap " + consumedGap);
                    } else {
                        consumedGap = (layoutInVertical ? rangeStyle.mVGap : rangeStyle.mHGap);
                        //Log.d(TAG, "2 --> " + currentPosition + " add gap " + consumedGap);
                    }
                }
            } else {
                if (!isEndLine) {
                    if (isSecondEndLine) {
                        consumedGap = (layoutInVertical ? rangeStyle.mParent.mVGap : rangeStyle.mParent.mHGap);
                        //Log.d(TAG, "3 --> " + currentPosition + " add gap " + consumedGap);
                    } else {
                        consumedGap = (layoutInVertical ? rangeStyle.mVGap : rangeStyle.mHGap);
                        //Log.d(TAG, "4 --> " + currentPosition + " add gap " + consumedGap);
                    }
                }
            }
        }
        result.mConsumed += consumedGap;

        //if (!mLayoutWithAnchor && (!isEndLine || !layoutStart) && (!isStartLine || layoutStart)) {
        //    result.mConsumed += (layoutInVertical ? rangeStyle.mVGap : rangeStyle.mHGap);
        //    Log.d(TAG, "--> " + currentPosition + " add gap");
        //}

        Log.d(TAG, "--> " + currentPosition + " consumed " + result.mConsumed + " startSpace " + startSpace + " endSpace " + endSpace);


        int left = 0, right = 0, top = 0, bottom = 0;
        if (layoutInVertical) {
            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                bottom = layoutState.getOffset() - endSpace - (consumedGap);
                top = bottom - maxSize;
            } else {
                top = layoutState.getOffset() + startSpace + (consumedGap);
                bottom = top + maxSize;
            }
        } else {
            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                right = layoutState.getOffset() - endSpace - (consumedGap);
                left = right - maxSize;
            } else {
                left = layoutState.getOffset() + startSpace + (consumedGap);
                right = left + maxSize;
            }
        }

        for (int i = 0; i < count; i++) {
            View view = rangeStyle.mSet[i];
            final int index = rangeStyle.mSpanIndices[i];

            LayoutParams params = (LayoutParams) view.getLayoutParams();
            if (layoutInVertical) {
                if (weighted) {
                    left = helper.getPaddingLeft() + rangeStyle.getFamilyMarginLeft() + rangeStyle.getFamilyPaddingLeft();
                    for (int j = 0; j < index; j++) {
                        left += rangeStyle.mSpanCols[j] + rangeStyle.mHGap;
                    }
                } else {
                    left = helper.getPaddingLeft() + rangeStyle.getFamilyMarginLeft() + rangeStyle
                        .getFamilyPaddingLeft() + rangeStyle.mSizePerSpan * index + index * rangeStyle.mHGap;
                }

                right = left + orientationHelper.getDecoratedMeasurementInOther(view);
            } else {

                if (weighted) {
                    top = helper.getPaddingTop() + rangeStyle.getFamilyMarginTop() + rangeStyle.getFamilyPaddingTop();
                    for (int j = 0; j < index; j++) {
                        top += rangeStyle.mSpanCols[j] + rangeStyle.mVGap;
                    }
                } else {
                    top = helper.getPaddingTop() + rangeStyle.getFamilyMarginTop() + rangeStyle.getFamilyPaddingTop()
                            + rangeStyle.mSizePerSpan * index + index * rangeStyle.mVGap;
                }

                bottom = top + orientationHelper.getDecoratedMeasurementInOther(view);
            }

            if (DEBUG) {
                Log.d(TAG, "layout item in position: " + params.getViewPosition() + " with text with SpanIndex: " + index + " into (" +
                        left + ", " + top + ", " + right + ", " + bottom + " )");
            }

            // We calculate everything with View's bounding box (which includes decor and margins)
            // To calculate correct layout position, we subtract margins.
            rangeStyle.layoutChild(view, left, top, right, bottom, helper, false);

            // Consume the available space if the view is not removed OR changed
            if (params.isItemRemoved() || params.isItemChanged()) {
                result.mIgnoreConsumed = true;
            }

            result.mFocusable |= view.isFocusable();
        }


        mLayoutWithAnchor = false;
        Arrays.fill(rangeStyle.mSet, null);
        Arrays.fill(rangeStyle.mSpanIndices, 0);
        Arrays.fill(rangeStyle.mSpanCols, 0);
    }

    @Override
    public void afterLayout(Recycler recycler, State state, int startPosition, int endPosition, int scrolled,
        LayoutManagerHelper helper) {
        mRangeStyle.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);
    }

    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        if (isLayoutEnd) {
            if (offset == getItemCount() - 1) {
                return mRangeStyle.computeEndAlignOffset(layoutInVertical);
            }
        } else {
            if (offset == 0) {
                return mRangeStyle.computeStartAlignOffset(layoutInVertical);
            }
        }

        return super.computeAlignOffset(offset, isLayoutEnd, useAnchor, helper);
    }

    @Override
    public void onClear(LayoutManagerHelper helper) {
        super.onClear(helper);
        mRangeStyle.onClear();
    }

    @Override
    public void onItemsChanged(LayoutManagerHelper helper) {
        super.onItemsChanged(helper);
        mRangeStyle.onClear();
    }

    private static final int MAIN_DIR_SPEC =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

    private int getMainDirSpec(RangeStyle rangeStyle, int dim, int otherSize, int viewSize, float viewAspectRatio) {
        if (!Float.isNaN(viewAspectRatio) && viewAspectRatio > 0 && viewSize > 0) {
            return View.MeasureSpec.makeMeasureSpec((int) (viewSize / viewAspectRatio + 0.5f), View.MeasureSpec.EXACTLY);
        } else if (!Float.isNaN(rangeStyle.mAspectRatio) && rangeStyle.mAspectRatio > 0) {
            return View.MeasureSpec.makeMeasureSpec((int) (otherSize / rangeStyle.mAspectRatio + 0.5f), View.MeasureSpec.EXACTLY);
        } else if (dim < 0) {
            return MAIN_DIR_SPEC;
        } else {
            return View.MeasureSpec.makeMeasureSpec(dim, View.MeasureSpec.EXACTLY);
        }
    }

    private boolean mLayoutWithAnchor = false;

    @Override
    public void checkAnchorInfo(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {
        if (state.getItemCount() > 0 && !state.isPreLayout()) {
            RangeStyle rangeStyle = mRangeStyle.findRangeStyleByPosition(anchorInfo.position);
            int span = rangeStyle.mSpanSizeLookup.getCachedSpanIndex(anchorInfo.position, rangeStyle.mSpanCount);
            if (anchorInfo.layoutFromEnd) {
                while (span < rangeStyle.mSpanCount - 1 && anchorInfo.position < getRange().getUpper()) {
                    anchorInfo.position++;
                    span = rangeStyle.mSpanSizeLookup.getCachedSpanIndex(anchorInfo.position, rangeStyle.mSpanCount);
                }
            } else {
                while (span > 0 && anchorInfo.position > 0) {
                    anchorInfo.position--;
                    span = rangeStyle.mSpanSizeLookup.getCachedSpanIndex(anchorInfo.position, rangeStyle.mSpanCount);
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


    private int getSpanIndex(SpanSizeLookup spanSizeLookup, int spanCount, RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return spanSizeLookup.getCachedSpanIndex(pos, spanCount);
        }

        final int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            return 0;
        }
        return spanSizeLookup.getCachedSpanIndex(adapterPosition, spanCount);
    }


    private int getSpanSize(SpanSizeLookup spanSizeLookup, RecyclerView.Recycler recycler, RecyclerView.State state, int pos) {
        if (!state.isPreLayout()) {
            return spanSizeLookup.getSpanSize(pos);
        }

        final int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition == -1) {
            return 0;
        }

        return spanSizeLookup.getSpanSize(adapterPosition);
    }

    private void assignSpans(RangeStyle rangeStyle, RecyclerView.Recycler recycler, RecyclerView.State state, int count,
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
            View view = rangeStyle.mSet[i];
            int spanSize = getSpanSize(rangeStyle.mSpanSizeLookup, recycler, state, helper.getPosition(view));
            if (spanDiff == -1 && spanSize > 1) {
                rangeStyle.mSpanIndices[i] = span - (spanSize - 1);
            } else {
                rangeStyle.mSpanIndices[i] = span;
            }
            span += spanDiff * spanSize;
        }
    }

    public static class RangeStyle {

        private BaseLayoutHelper mLayoutHelper;

        private RangeStyle mParent;

        private Range<Integer> mRange;

        //TODO update data structure
        private ArrayMap<Range, RangeStyle> mChildren = new ArrayMap<>();

        protected int mPaddingLeft;

        protected int mPaddingRight;

        protected int mPaddingTop;

        protected int mPaddingBottom;

        protected int mMarginLeft;

        protected int mMarginRight;

        protected int mMarginTop;

        protected int mMarginBottom;

        private float mAspectRatio = Float.NaN;

        private int mSpanCount = 4;

        @SuppressWarnings("FieldCanBeLocal")
        private int mSizePerSpan = 0;

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

        protected Rect mLayoutRegion = new Rect();

        private View mLayoutView;

        private int mBgColor;

        private LayoutViewUnBindListener mLayoutViewUnBindListener;

        private LayoutViewBindListener mLayoutViewBindListener;

        public RangeStyle(RangeGridLayoutHelper layoutHelper) {
            mLayoutHelper = layoutHelper;
            mSpanSizeLookup.setSpanIndexCacheEnabled(true);
        }

        public RangeStyle() {
            mSpanSizeLookup.setSpanIndexCacheEnabled(true);
        }

        public void addChildRangeStyle(Range range, RangeStyle rangeStyle) {
            if (range != null && rangeStyle != null) {
                rangeStyle.setParent(this);
                mChildren.put(range, rangeStyle);
            }
        }

        public void setParent(RangeStyle rangeStyle) {
            this.mParent = rangeStyle;
        }

        //TODO find style itr
        public RangeStyle findRangeStyleByPosition(int position) {
            RangeStyle rangeStyle = this;
            for (int i = 0, size = mChildren.size(); i < size; i++) {
                Range range = mChildren.keyAt(i);
                if (range.contains(position)) {
                    rangeStyle = mChildren.valueAt(i);
                    break;
                }
            }
            return rangeStyle;
        }

        public void onClear() {
            mSpanSizeLookup.invalidateSpanIndexCache();
            for (int i = 0, size = mChildren.size(); i < size; i++) {
                RangeStyle rangeStyle = mChildren.valueAt(i);
                rangeStyle.mSpanSizeLookup.invalidateSpanIndexCache();
            }
        }

        //TODO compute align itr
        public int computeEndAlignOffset(boolean layoutInVertical) {
            int offset = layoutInVertical ? mMarginBottom + mPaddingBottom : mMarginRight + mPaddingRight;
            int endPosition = mRange.getUpper().intValue();
            for (int i = 0, size = mChildren.size(); i < size; i++) {
                RangeStyle rangeStyle = mChildren.valueAt(i);
                if (rangeStyle.mRange.getUpper().intValue() == endPosition) {
                    offset += (layoutInVertical ? rangeStyle.mMarginBottom + rangeStyle.mPaddingBottom
                        : rangeStyle.mMarginRight + rangeStyle.mPaddingRight);
                    break;
                }
            }
            return offset;
        }

        //TODO compute align itr
        public int computeStartAlignOffset(boolean layoutInVertical) {
            int offset = layoutInVertical ? -mMarginTop - mPaddingTop : -mMarginLeft - mPaddingLeft;
            int startPosition = mRange.getLower().intValue();
            for (int i = 0, size = mChildren.size(); i < size; i++) {
                RangeStyle rangeStyle = mChildren.valueAt(i);
                if (rangeStyle.mRange.getLower().intValue() == startPosition) {
                    offset += (layoutInVertical ? -rangeStyle.mMarginTop - rangeStyle.mPaddingTop
                        : -rangeStyle.mMarginLeft - rangeStyle.mPaddingLeft);
                    break;
                }
            }
            return offset;
        }

        public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
            LayoutManagerHelper helper) {
            if (!isChildrenEmpty()) {
                for (int i = 0, size = mChildren.size(); i < size; i++) {
                    RangeStyle rangeStyle = mChildren.valueAt(i);
                    rangeStyle.beforeLayout(recycler, state, helper);
                }
            }
            if (requireLayoutView()) {
                if (mLayoutView != null) {
                    // helper.detachChildView(mLayoutView);
                }
            } else {
                // if no layoutView is required, remove it
                if (mLayoutView != null) {
                    helper.removeChildView(mLayoutView);
                    mLayoutView = null;
                }
            }

        }

        private boolean isValidScrolled(int scrolled) {
            return scrolled != Integer.MAX_VALUE && scrolled != Integer.MIN_VALUE;
        }

        public void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
            int startPosition, int endPosition, int scrolled,
            LayoutManagerHelper helper) {

            if (!isChildrenEmpty()) {
                for (int i = 0, size = mChildren.size(); i < size; i++) {
                    RangeStyle rangeStyle = mChildren.valueAt(i);
                    rangeStyle.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);
                }
            }
            if (DEBUG) {
                Log.d(TAG, "call afterLayout() on " + this.getClass().getSimpleName());
            }


            if (requireLayoutView()) {
                if (isValidScrolled(scrolled) && mLayoutView != null) {
                    // initial layout do reset
                    mLayoutRegion.union(mLayoutView.getLeft(), mLayoutView.getTop(), mLayoutView.getRight(), mLayoutView.getBottom());
                }


                if (!mLayoutRegion.isEmpty()) {
                    if (isValidScrolled(scrolled)) {
                        if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
                            mLayoutRegion.offset(0, -scrolled);
                        }
                        else {
                            mLayoutRegion.offset(-scrolled, 0);
                        }
                    }
                    int contentWidth = helper.getContentWidth();
                    int contentHeight = helper.getContentHeight();
                    if (helper.getOrientation() == VirtualLayoutManager.VERTICAL ?
                        mLayoutRegion.intersects(0, -contentHeight / 4, contentWidth, contentHeight + contentHeight / 4) :
                        mLayoutRegion.intersects(-contentWidth / 4, 0, contentWidth + contentWidth / 4, contentHeight)) {

                        if (mLayoutView == null) {
                            mLayoutView = helper.generateLayoutView();
                            helper.addBackgroundView(mLayoutView, true);
                        }
                        //finally fix layoutRegion's height and with here to avoid visual blank
                        if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
                            mLayoutRegion.left = helper.getPaddingLeft() + getFamilyMarginLeft()
                                + getAncestorPaddingLeft();
                            mLayoutRegion.right = helper.getContentWidth() - helper.getPaddingRight()
                                - getFamilyMarginRight() - getAncestorPaddingRight();
                        } else {
                            mLayoutRegion.top = helper.getPaddingTop() + getFamilyMarginTop() + getAncestorPaddingTop();
                            mLayoutRegion.bottom = helper.getContentWidth() - helper.getPaddingBottom()
                                - getFamilyMarginBottom() - getAncestorPaddingBottom();
                        }

                        bindLayoutView(mLayoutView);
                        if (isRoot()) {
                            //TODO hide itr
                            helper.hideView(mLayoutView);
                            for (int i = 0, size = mChildren.size(); i < size; i++) {
                                RangeStyle rangeStyle = mChildren.valueAt(i);
                                helper.hideView(rangeStyle.mLayoutView);
                            }
                        }
                        return;
                    } else {
                        mLayoutRegion.set(0, 0, 0, 0);
                        if (mLayoutView != null) {
                            mLayoutView.layout(0, 0, 0, 0);
                        }
                    }
                }
            }

            if (mLayoutView != null) {
                if (mLayoutViewUnBindListener != null) {
                    mLayoutViewUnBindListener.onUnbind(mLayoutView, getLayoutHelper());
                }
                helper.removeChildView(mLayoutView);
                mLayoutView = null;
            }
        }

        public boolean requireLayoutView() {
            boolean self = mBgColor != 0 || mLayoutViewBindListener != null;
            if (!isChildrenEmpty()) {
                for (int i = 0, size = mChildren.size(); i < size; i++) {
                    RangeStyle rangeStyle = mChildren.valueAt(i);
                    self |= rangeStyle.requireLayoutView();
                }
            }
            return self;
        }

        public void bindLayoutView(@NonNull final View layoutView) {
            layoutView.measure(View.MeasureSpec.makeMeasureSpec(mLayoutRegion.width(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(mLayoutRegion.height(), View.MeasureSpec.EXACTLY));
            layoutView.layout(mLayoutRegion.left, mLayoutRegion.top, mLayoutRegion.right, mLayoutRegion.bottom);
            layoutView.setBackgroundColor(mBgColor);

            if (mLayoutViewBindListener != null) {
                mLayoutViewBindListener.onBind(layoutView, getLayoutHelper());
            }

            // reset region rectangle
            mLayoutRegion.set(0, 0, 0, 0);
        }

        public void setLayoutViewBindListener(LayoutViewBindListener bindListener) {
            mLayoutViewBindListener = bindListener;
        }

        public void setLayoutViewUnBindListener(
            LayoutViewUnBindListener layoutViewUnBindListener) {
            mLayoutViewUnBindListener = layoutViewUnBindListener;
        }

        public void setBgColor(int bgColor) {
            this.mBgColor = bgColor;
        }

        public void setAspectRatio(float aspectRatio) {
            this.mAspectRatio = aspectRatio;
        }

        public float getAspectRatio() {
            return mAspectRatio;
        }

        /**
         * set paddings for this layoutHelper
         * @param leftPadding left padding
         * @param topPadding top padding
         * @param rightPadding right padding
         * @param bottomPadding bottom padding
         */
        public void setPadding(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
            mPaddingLeft = leftPadding;
            mPaddingRight = rightPadding;
            mPaddingTop = topPadding;
            mPaddingBottom = bottomPadding;
        }

        /**
         * Set margins for this layoutHelper
         *
         * @param leftMargin left margin
         * @param topMargin top margin
         * @param rightMargin right margin
         * @param bottomMargin bottom margin
         */
        public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
            this.mMarginLeft = leftMargin;
            this.mMarginTop = topMargin;
            this.mMarginRight = rightMargin;
            this.mMarginBottom = bottomMargin;
        }

        /**
         * Get total margin in horizontal dimension
         *
         * @return
         */
        protected int getHorizontalMargin() {
            return mMarginLeft + mMarginRight;
        }

        /**
         * Get total margin in vertical dimension
         *
         * @return
         */
        protected int getVerticalMargin() {
            return mMarginTop + mMarginBottom;
        }

        /**
         * Get total padding in horizontal dimension
         * @return
         */
        protected int getHorizontalPadding() {
            return mPaddingLeft + mPaddingRight;
        }

        /**
         * Get total padding in vertical dimension
         * @return
         */
        protected int getVerticalPadding() {
            return mPaddingTop + mPaddingBottom;
        }

        public int getPaddingLeft() {
            return mPaddingLeft;
        }

        public int getPaddingRight() {
            return mPaddingRight;
        }

        public int getPaddingTop() {
            return mPaddingTop;
        }

        public int getPaddingBottom() {
            return mPaddingBottom;
        }

        public int getMarginLeft() {
            return mMarginLeft;
        }

        public int getMarginRight() {
            return mMarginRight;
        }

        public int getMarginTop() {
            return mMarginTop;
        }

        public int getMarginBottom() {
            return mMarginBottom;
        }

        public void setPaddingLeft(int paddingLeft) {
            mPaddingLeft = paddingLeft;
        }

        public void setPaddingRight(int paddingRight) {
            mPaddingRight = paddingRight;
        }

        public void setPaddingTop(int paddingTop) {
            mPaddingTop = paddingTop;
        }

        public void setPaddingBottom(int paddingBottom) {
            mPaddingBottom = paddingBottom;
        }

        public void setMarginLeft(int marginLeft) {
            mMarginLeft = marginLeft;
        }

        public void setMarginRight(int marginRight) {
            mMarginRight = marginRight;
        }

        public void setMarginTop(int marginTop) {
            mMarginTop = marginTop;
        }

        public void setMarginBottom(int marginBottom) {
            mMarginBottom = marginBottom;
        }

        public boolean isOutOfRange(int position) {
            return mRange != null ? !mRange.contains(position) : true;
        }

        public void setRange(int start, int end) {
            mRange = Range.create(start, end);
            mSpanSizeLookup.setStartPosition(start);
            mSpanSizeLookup.invalidateSpanIndexCache();
        }

        public Range<Integer> getRange() {
            return mRange;
        }

        public BaseLayoutHelper getLayoutHelper() {
            if (mLayoutHelper != null) {
                return mLayoutHelper;
            }
            if (mParent != null) {
                return mParent.getLayoutHelper();
            }
            return null;
        }

        public boolean isChildrenEmpty() {
            return mChildren.isEmpty();
        }

        public boolean isRoot() {
            return mParent == null;
        }

        public int getFamilyHorizontalMargin() {
            return (mParent != null ? mParent.getFamilyHorizontalMargin() : 0) + getHorizontalMargin();
        }

        public int getFamilyVerticalMargin() {
            return (mParent != null ? mParent.getFamilyVerticalMargin() : 0) + getVerticalMargin();
        }

        public int getFamilyHorizontalPadding() {
            return (mParent != null ? mParent.getFamilyHorizontalPadding() : 0) + getHorizontalPadding();
        }

        public int getFamilyVerticalPadding() {
            return (mParent != null ? mParent.getFamilyVerticalPadding() : 0) + getVerticalPadding();
        }

        public int getFamilyPaddingLeft() {
            return (mParent != null ? mParent.getFamilyPaddingLeft() : 0) + mPaddingLeft;
        }

        public int getFamilyPaddingRight() {
            return (mParent != null ? mParent.getFamilyPaddingRight() : 0) + mPaddingRight;
        }

        public int getFamilyPaddingTop() {
            return (mParent != null ? mParent.getFamilyPaddingTop() : 0) + mPaddingTop;
        }

        public int getFamilyPaddingBottom() {
            return (mParent != null ? mParent.getFamilyPaddingBottom() : 0) + mPaddingBottom;
        }

        public int getFamilyMarginLeft() {
            return (mParent != null ? mParent.getFamilyMarginLeft() : 0) + mMarginLeft;
        }

        public int getFamilyMarginRight() {
            return (mParent != null ? mParent.getFamilyMarginRight() : 0) + mMarginRight;
        }

        public int getFamilyMarginTop() {
            return (mParent != null ? mParent.getFamilyMarginTop() : 0) + mMarginTop;
        }

        public int getFamilyMarginBottom() {
            return (mParent != null ? mParent.getFamilyMarginBottom() : 0) + mMarginBottom;
        }

        public int getAncestorHorizontalMargin() {
            return (mParent != null ? mParent.getAncestorHorizontalMargin() + mParent.getHorizontalMargin() : 0);
        }

        public int getAncestorVerticalMargin() {
            return (mParent != null ? mParent.getAncestorVerticalMargin() + mParent.getVerticalMargin(): 0);
        }

        public int getAncestorHorizontalPadding() {
            return (mParent != null ? mParent.getAncestorHorizontalPadding() + mParent.getHorizontalPadding() : 0);
        }

        public int getAncestorVerticalPadding() {
            return (mParent != null ? mParent.getAncestorVerticalPadding() + mParent.getVerticalPadding() : 0);
        }

        public int getAncestorPaddingLeft() {
            return (mParent != null ? mParent.getAncestorPaddingLeft() + mParent.getPaddingLeft() : 0);
        }

        public int getAncestorPaddingRight() {
            return (mParent != null ? mParent.getAncestorPaddingRight() + mParent.getPaddingRight() : 0);
        }

        public int getAncestorPaddingTop() {
            return (mParent != null ? mParent.getAncestorPaddingTop() + mParent.getPaddingTop() : 0);
        }

        public int getAncestorPaddingBottom() {
            return (mParent != null ? mParent.getAncestorPaddingBottom() + mParent.getPaddingBottom() : 0);
        }

        public int getAncestorMarginLeft() {
            return (mParent != null ? mParent.getAncestorMarginLeft() + mParent.getMarginLeft() : 0);
        }

        public int getAncestorMarginRight() {
            return (mParent != null ? mParent.getAncestorMarginRight() + mParent.getMarginRight() : 0);
        }

        public int getAncestorMarginTop() {
            return (mParent != null ? mParent.getAncestorMarginTop() + mParent.getMarginTop() : 0);
        }

        public int getAncestorMarginBottom() {
            return (mParent != null ? mParent.getAncestorMarginBottom() + mParent.getMarginBottom() : 0);
        }

        public void setGap(int gap) {
            setVGap(gap);
            setHGap(gap);
        }

        public void setVGap(int vGap) {
            if (vGap < 0) {
                vGap = 0;
            }
            this.mVGap = vGap;
        }

        public void setHGap(int hGap) {
            if (hGap < 0) {
                hGap = 0;
            }
            this.mHGap = hGap;
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

        public int getSpanCount() {
            return mSpanCount;
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

        protected void layoutChild(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper, boolean addLayoutRegionWithMargin) {
            helper.layoutChild(child, left, top, right, bottom);
            if (requireLayoutView()) {
                if (addLayoutRegionWithMargin) {
                    mLayoutRegion
                        .union(left - mPaddingLeft - mMarginLeft, top - mPaddingTop - mMarginTop,
                            right + mPaddingRight + mMarginRight,
                            bottom + mPaddingBottom + mMarginBottom);
                } else {
                    mLayoutRegion.union(left - mPaddingLeft, top - mPaddingTop, right + mPaddingRight,
                        bottom + mPaddingBottom);
                }
            }

        }

    }

}
