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
import com.alibaba.android.vlayout.OrientationHelperEx;
import com.alibaba.android.vlayout.Range;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutParams;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;
import com.alibaba.android.vlayout.layout.GridLayoutHelper.DefaultSpanSizeLookup;
import com.alibaba.android.vlayout.layout.GridLayoutHelper.SpanSizeLookup;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.Log;
import android.view.View;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;

/**
 * LayoutHelper provides RangeGridLayoutHelper. The difference with {@link GridLayoutHelper} is that this layoutHelper could has child group logically but implemented as flat.
 *
 * @author longerian
 * @since 1.0.0
 */
public class RangeGridLayoutHelper extends BaseLayoutHelper {
    private static final String TAG = "RGLayoutHelper";

    @SuppressWarnings("FieldCanBeLocal")
    private static boolean DEBUG = false;

    private GridRangeStyle mRangeStyle;

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
        mRangeStyle = new GridRangeStyle(this);
        mRangeStyle.setSpanCount(spanCount);
        mRangeStyle.setVGap(vGap);
        mRangeStyle.setHGap(hGap);
        setItemCount(itemCount);
    }

    /**
     *
     * @param start offset relative to its parent
     * @param end offset relative to its parent
     * @param rangeStyle new range style
     */
    public void addRangeStyle(int start, int end, GridRangeStyle rangeStyle) {
        mRangeStyle.addChildRangeStyle(start, end, rangeStyle);
    }

    public GridRangeStyle getRootRangeStyle() {
        return mRangeStyle;
    }

    @Override
    public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        super.setMargin(leftMargin, topMargin, rightMargin, bottomMargin);
        mRangeStyle.setMargin(leftMargin, topMargin, rightMargin, bottomMargin);
    }

    @Override
    public void setPadding(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        super.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
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
    public void setLayoutViewHelper(DefaultLayoutViewHelper layoutViewHelper) {
        mRangeStyle.setLayoutViewHelper(layoutViewHelper);
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

    //TODO optimize this method
    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        boolean isStartLine = false, isEndLine = false;
        boolean isSecondStartLine = false, isSecondEndLine = false;
        final int currentPosition = layoutState.getCurrentPosition();
        GridRangeStyle rangeStyle = mRangeStyle.findRangeStyleByPosition(currentPosition);

        final int itemDirection = layoutState.getItemDirection();
        final boolean layingOutInPrimaryDirection =
            itemDirection == LayoutStateWrapper.ITEM_DIRECTION_TAIL;

        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

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
                    if (rangeStyle.isOutOfRange(index)) {
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


                    if (!isEndLine) {
                        isEndLine = helper.getReverseLayout() ? index == mRangeStyle.getRange().getLower().intValue()
                            : index == mRangeStyle.getRange().getUpper().intValue();
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
            if (rangeStyle.isOutOfRange(pos)) {
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
                if (!rangeStyle.equals(mRangeStyle)) {
                    if (mLayoutWithAnchor) {
                        pos = layoutState.getCurrentPosition();
                    }
                    isSecondStartLine = helper.getReverseLayout() ? pos == rangeStyle.getRange().getUpper()
                        .intValue() : pos == rangeStyle.getRange().getLower().intValue();
                }
            }

            if (!isEndLine) {
                isEndLine = helper.getReverseLayout() ? pos == mRangeStyle.getRange().getLower().intValue()
                    : pos == mRangeStyle.getRange().getUpper().intValue();
            }

            if (!isSecondEndLine) {
                if (!rangeStyle.equals(mRangeStyle)) {
                    if (mLayoutWithAnchor) {
                        pos = layoutState.getCurrentPosition();
                    }
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
        } else if (!layingOutInPrimaryDirection && remainingSpan == 0 && (count == consumedSpanCount) && rangeStyle.mIsAutoExpand) {
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
                helper.measureChildWithMargins(view, spec, getMainDirSpec(rangeStyle, lp.height, mTotalSize,
                    View.MeasureSpec.getSize(spec), lp.mAspectRatio));
            } else {
                helper.measureChildWithMargins(view,
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
                    helper.measureChildWithMargins(view, spec, maxMeasureSpec);
                } else {
                    helper.measureChildWithMargins(view, maxMeasureSpec, spec);
                }
            }
        }

        int startSpace = 0, endSpace = 0;

        int secondStartSpace = 0, secondEndSpace = 0;
        boolean isLayoutEnd = layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_END;
        final boolean isOverLapMargin = helper.isEnableMarginOverLap();

        if (isStartLine) {
            startSpace = computeStartSpace(helper, layoutInVertical, isLayoutEnd, isOverLapMargin);
        }
        if (isSecondStartLine) {
            secondStartSpace = (layoutInVertical ? rangeStyle.getMarginTop() + rangeStyle.getPaddingTop()
                : rangeStyle.getMarginLeft() + rangeStyle.getPaddingLeft());
        }

        if (isEndLine) {
            endSpace = layoutInVertical ? mRangeStyle.getMarginBottom() + mRangeStyle.getPaddingBottom()
                : mRangeStyle.getMarginRight() + mRangeStyle.getPaddingRight();
        }
        if (isSecondEndLine) {
            secondEndSpace = (layoutInVertical ? rangeStyle.getMarginBottom() + rangeStyle.getPaddingBottom()
                : rangeStyle.getMarginRight() + rangeStyle.getPaddingRight());
        }


        result.mConsumed = maxSize + startSpace + endSpace + secondStartSpace + secondEndSpace;

        final boolean layoutStart = layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START;
        int consumedGap = 0;
        if (!mLayoutWithAnchor) {
            if (!layoutStart) {
                if (!isStartLine) {
                    if (isSecondStartLine) {
                        consumedGap = (layoutInVertical ? rangeStyle.mParent.mVGap : rangeStyle.mParent.mHGap);
                        if (DEBUG) {
                            Log.d(TAG, "⬇ " + currentPosition + " 1 " + consumedGap + " gap");
                        }
                    } else {
                        consumedGap = (layoutInVertical ? rangeStyle.mVGap : rangeStyle.mHGap);
                        if (DEBUG) {
                            Log.d(TAG, "⬇ " + currentPosition + " 2 " + consumedGap + " gap");
                        }
                    }
                }
            } else {
                if (!isEndLine) {
                    if (isSecondEndLine) {
                        consumedGap = (layoutInVertical ? rangeStyle.mParent.mVGap : rangeStyle.mParent.mHGap);
                        if (DEBUG) {
                            Log.d(TAG, "⬆ " + currentPosition + " 3 " + consumedGap + " gap");
                        }
                    } else {
                        consumedGap = (layoutInVertical ? rangeStyle.mVGap : rangeStyle.mHGap);
                        if (DEBUG) {
                            Log.d(TAG, "⬆ " + currentPosition + " 4 " + consumedGap + " gap");
                        }
                    }
                }
            }
        }
        result.mConsumed += consumedGap;

        if (result.mConsumed <= 0) {
            result.mConsumed = 0;
        }

        int lastUnconsumedSpace = 0;
        /** layoutView() may be triggered by layoutManager's scrollInternalBy() or onFocusSearchFailed() or onLayoutChildren()
         *
         * In case of scrollInternalBy() or onFocusSearchFailed(), layoutState.isRefreshLayout() == false, and layoutState.mOffset = ChildClosestToExpose + alignOffset,
         * see {@link com.alibaba.android.vlayout.ExposeLinearLayoutManagerEx#updateLayoutStateExpose(int, int, boolean, State)},
         * this means last line's layout padding or margin is not really consumed, so considering it before layout new line.
         *
         * In case of onLayoutChildren(), layoutState.isRefreshLayout() == true, and layoutState.mOffset = anchorInfo.mCoordinate = anchorChild.start + alignOffset,
         * see {@link com.alibaba.android.vlayout.ExposeLinearLayoutManagerEx#updateAnchorInfoForLayoutExpose(State, AnchorInfo)},
         * this means last line's layout padding or margin is consumed.
         **/
        if (!layoutState.isRefreshLayout()) {
            if (layoutStart) {
                int lastLinePosition = currentPosition + 1;
                if (!isOutOfRange(lastLinePosition)) {
                    RangeStyle<GridRangeStyle> neighbourRange = mRangeStyle.findRangeStyleByPosition(lastLinePosition);
                    if (neighbourRange.isFirstPosition(lastLinePosition)) {
                        lastUnconsumedSpace = layoutInVertical ? neighbourRange.getMarginTop() + neighbourRange.getPaddingTop()
                            : neighbourRange.getMarginLeft() + neighbourRange.getPaddingLeft();
                        if (DEBUG) {
                            Log.d(TAG, "⬆ " + currentPosition + " 1 " + lastUnconsumedSpace + " last");
                        }
                    }
                }
            } else {
                int lastLinePosition = currentPosition - 1;
                if (!isOutOfRange(lastLinePosition)) {
                    RangeStyle<GridRangeStyle> neighbourRange = mRangeStyle.findRangeStyleByPosition(lastLinePosition);
                    if (neighbourRange.isLastPosition(lastLinePosition)) {
                        lastUnconsumedSpace = layoutInVertical ? neighbourRange.getMarginBottom() + neighbourRange.getPaddingBottom()
                            : neighbourRange.getMarginRight() + neighbourRange.getPaddingRight();
                        if (DEBUG) {
                            Log.d(TAG, "⬇ " + currentPosition + " 2 " + lastUnconsumedSpace + " last");
                        }
                    }
                }
            }
        }

        if (DEBUG) {
            Log.d(TAG,
                (layoutStart ? "⬆ " : "⬇ ") + currentPosition + " consumed " + result.mConsumed + " startSpace " + startSpace + " endSpace "
                    + endSpace + " secondStartSpace " + secondStartSpace + " secondEndSpace " + secondEndSpace + " lastUnconsumedSpace " + lastUnconsumedSpace);
        }

        int left = 0, right = 0, top = 0, bottom = 0;
        if (layoutInVertical) {
            if (layoutStart) {
                bottom = layoutState.getOffset() - endSpace - secondEndSpace - (consumedGap) - lastUnconsumedSpace;
                top = bottom - maxSize;
            } else {
                top = layoutState.getOffset() + startSpace + secondStartSpace + (consumedGap) + lastUnconsumedSpace;
                bottom = top + maxSize;
            }
        } else {
            if (layoutStart) {
                right = layoutState.getOffset() - endSpace - (consumedGap) - lastUnconsumedSpace;
                left = right - maxSize;
            } else {
                left = layoutState.getOffset() + startSpace + (consumedGap) + lastUnconsumedSpace;
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
    public void adjustLayout(int startPosition, int endPosition, LayoutManagerHelper helper) {
        mRangeStyle.adjustLayout(startPosition, endPosition, helper);
    }
    
    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (isLayoutEnd) {
            if (offset == getItemCount() - 1) {
                return GridRangeStyle.computeEndAlignOffset(mRangeStyle, layoutInVertical);
            }
        } else {
            if (offset == 0) {
                return GridRangeStyle.computeStartAlignOffset(mRangeStyle, layoutInVertical);
            }
        }

        return super.computeAlignOffset(offset, isLayoutEnd, useAnchor, helper);
    }

    @Override
    public void onClear(LayoutManagerHelper helper) {
        super.onClear(helper);
        mRangeStyle.onClear(helper);
        mRangeStyle.onInvalidateSpanIndexCache();
    }

    @Override
    public void onItemsChanged(LayoutManagerHelper helper) {
        super.onItemsChanged(helper);
        mRangeStyle.onInvalidateSpanIndexCache();
    }

    private static final int MAIN_DIR_SPEC =
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

    private int getMainDirSpec(GridRangeStyle rangeStyle, int dim, int otherSize, int viewSize, float viewAspectRatio) {
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
        if (state.getItemCount() > 0 ) {
            GridRangeStyle rangeStyle = mRangeStyle.findRangeStyleByPosition(anchorInfo.position);
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

    private void assignSpans(GridRangeStyle rangeStyle, RecyclerView.Recycler recycler, RecyclerView.State state, int count,
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

    public int getBorderStartSpace(LayoutManagerHelper helper) {
        int start = getRange().getLower().intValue();
        RangeStyle rangeStyle = mRangeStyle.findRangeStyleByPosition(start);
        if (helper.getOrientation() == VERTICAL) {
            return rangeStyle.getFamilyMarginTop() + rangeStyle.getFamilyPaddingTop();
        } else {
            return rangeStyle.getFamilyMarginLeft() + rangeStyle.getFamilyPaddingLeft();
        }
    }

    public int getBorderEndSpace(LayoutManagerHelper helper) {
        int end = getRange().getUpper().intValue();
        RangeStyle rangeStyle = mRangeStyle.findRangeStyleByPosition(end);
        if (helper.getOrientation() == VERTICAL) {
            return rangeStyle.getFamilyMarginBottom() + rangeStyle.getFamilyPaddingBottom();
        } else {
            return rangeStyle.getFamilyMarginRight() + rangeStyle.getFamilyPaddingRight();
        }
    }

    public static class GridRangeStyle extends RangeStyle<GridRangeStyle> {

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



        public GridRangeStyle(RangeGridLayoutHelper layoutHelper) {
            super(layoutHelper);
            mSpanSizeLookup.setSpanIndexCacheEnabled(true);
        }

        public GridRangeStyle() {
            mSpanSizeLookup.setSpanIndexCacheEnabled(true);
        }


        //TODO find style itr
        public GridRangeStyle findRangeStyleByPosition(int position) {
            return findRangeStyle(this, position);
        }

        private GridRangeStyle findRangeStyle(GridRangeStyle rangeStyle, int position){
            for (int i = 0, size = rangeStyle.mChildren.size(); i < size; i++) {
                GridRangeStyle childRangeStyle = rangeStyle.mChildren.valueAt(i);
                Range range = rangeStyle.mChildren.keyAt(i);
                if (!childRangeStyle.isChildrenEmpty()){
                    return findRangeStyle(childRangeStyle, position);
                } else if (range.contains(position)) {
                    return rangeStyle.mChildren.valueAt(i);
                }
            }
            return rangeStyle;
        }

        public GridRangeStyle findSiblingStyleByPosition(int position) {
            GridRangeStyle rangeStyle = null;
            if (mParent != null) {
                ArrayMap<Range<Integer>, GridRangeStyle> siblings = mParent.mChildren;
                for (int i = 0, size = siblings.size(); i < size; i++) {
                    Range range = siblings.keyAt(i);
                    if (range.contains(position)) {
                        GridRangeStyle childRangeStyle = siblings.valueAt(i);
                        if (!childRangeStyle.equals(this)) {
                            rangeStyle = childRangeStyle;
                        }
                        break;
                    }
                }
            }
            return rangeStyle;
        }

        public void onInvalidateSpanIndexCache() {
            mSpanSizeLookup.invalidateSpanIndexCache();
            for (int i = 0, size = mChildren.size(); i < size; i++) {
                GridRangeStyle rangeStyle = mChildren.valueAt(i);
                rangeStyle.onInvalidateSpanIndexCache();
            }
        }

        public static int computeEndAlignOffset(GridRangeStyle rangeStyle, boolean layoutInVertical) {
            int offset = layoutInVertical ? rangeStyle.mMarginBottom + rangeStyle.mPaddingBottom : rangeStyle.mMarginRight + rangeStyle.mPaddingRight;
            int endPosition = rangeStyle.getRange().getUpper().intValue();
            for (int i = 0, size = rangeStyle.mChildren.size(); i < size; i++) {
                GridRangeStyle childRangeStyle = rangeStyle.mChildren.valueAt(i);
                if (!childRangeStyle.isChildrenEmpty()){
                    offset += computeEndAlignOffset(childRangeStyle, layoutInVertical);
                }else if (childRangeStyle.mRange.getUpper().intValue() == endPosition) {
                    offset += (layoutInVertical ? childRangeStyle.mMarginBottom + childRangeStyle.mPaddingBottom
                        : childRangeStyle.mMarginRight + childRangeStyle.mPaddingRight);
                    break;
                }
            }
            return offset;
        }

        public static int computeStartAlignOffset(GridRangeStyle rangeStyle, boolean layoutInVertical) {
            int offset = layoutInVertical ? -rangeStyle.mMarginTop - rangeStyle.mPaddingTop : -rangeStyle.mMarginLeft - rangeStyle.mPaddingLeft;
            int startPosition = rangeStyle.getRange().getLower().intValue();
            for (int i = 0, size = rangeStyle.mChildren.size(); i < size; i++) {
                GridRangeStyle childRangeStyle = rangeStyle.mChildren.valueAt(i);
                if (!childRangeStyle.isChildrenEmpty()){
                    //FIXME may compute the wrong start space here
                    offset += computeStartAlignOffset(childRangeStyle, layoutInVertical);
                }else if (childRangeStyle.mRange.getLower().intValue() == startPosition) {
                    offset += (layoutInVertical ? -childRangeStyle.mMarginTop - childRangeStyle.mPaddingTop
                        : -childRangeStyle.mMarginLeft - childRangeStyle.mPaddingLeft);
                    break;
                }
            }
            return offset;
        }

        public void setAspectRatio(float aspectRatio) {
            this.mAspectRatio = aspectRatio;
        }

        public float getAspectRatio() {
            return mAspectRatio;
        }


        @Override
        public void setRange(int start, int end) {
            super.setRange(start, end);
            mSpanSizeLookup.setStartPosition(start);
            mSpanSizeLookup.invalidateSpanIndexCache();
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

    }

}
