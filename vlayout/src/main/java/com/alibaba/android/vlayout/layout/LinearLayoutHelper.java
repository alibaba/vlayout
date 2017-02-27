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

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;


/**
 * LayoutHelper layouts views as linear/ListView
 */
public class LinearLayoutHelper extends BaseLayoutHelper {

    private static final String TAG = "LinearLayoutHelper";

    private static final boolean DEBUG = false;

    private int mDividerHeight = 0;

    public LinearLayoutHelper() {
        this(0);
    }

    public LinearLayoutHelper(int dividerHeight) {
        // empty range
        this(dividerHeight, 0);
    }


    public LinearLayoutHelper(int dividerHeight, int itemCount) {
        setItemCount(itemCount);
        setDividerHeight(dividerHeight);
    }


    public void setDividerHeight(int dividerHeight) {
        if (dividerHeight < 0) dividerHeight = 0;
        this.mDividerHeight = dividerHeight;
    }

    /**
     * In {@link LinearLayoutHelper}, each iteration only consume one item,
     * so it can let parent LayoutManager to decide whether the next item is in the range of this helper
     */
    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                            VirtualLayoutManager.LayoutStateWrapper layoutState, LayoutChunkResult result,
                            LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }
        int currentPosition = layoutState.getCurrentPosition();

        // find corresponding layout container
        View view = nextView(recycler, layoutState, helper, result);
        if (view == null) {
            return;
        }

        VirtualLayoutManager.LayoutParams params = (VirtualLayoutManager.LayoutParams) view.getLayoutParams();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        int startSpace = 0, endSpace = 0, gap = 0;
        boolean isLayoutEnd = layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_END;
        boolean isStartLine = isLayoutEnd
                ? currentPosition == getRange().getLower().intValue()
                : currentPosition == getRange().getUpper().intValue();
        boolean isEndLine = isLayoutEnd
                ? currentPosition == getRange().getUpper().intValue()
                : currentPosition == getRange().getLower().intValue();

        if (isStartLine) {
            startSpace = layoutInVertical
                    ? (isLayoutEnd ? mMarginTop + mPaddingTop : mMarginBottom + mPaddingBottom)
                    : (isLayoutEnd ? mMarginLeft + mPaddingLeft : mMarginRight + mPaddingRight);
        }

        if (isEndLine) {
            endSpace = layoutInVertical
                    ? (isLayoutEnd ? mMarginBottom + mPaddingBottom : mMarginTop + mPaddingTop)
                    : (isLayoutEnd ? mMarginRight + mPaddingRight : mMarginLeft + mPaddingLeft);
        }

        if (!isStartLine) {
            gap = mDividerHeight;
        }

        final int widthSize = helper.getContentWidth() - helper.getPaddingLeft() - helper
                .getPaddingRight() - getHorizontalMargin() - getHorizontalPadding();
        int widthSpec = helper.getChildMeasureSpec(widthSize, params.width, !layoutInVertical);
        int heightSpec;
        float viewAspectRatio = params.mAspectRatio;
        if (!Float.isNaN(viewAspectRatio) && viewAspectRatio > 0) {
            heightSpec = View.MeasureSpec.makeMeasureSpec((int) (widthSize / viewAspectRatio + 0.5f),
                    View.MeasureSpec.EXACTLY);
        } else if (!Float.isNaN(mAspectRatio) && mAspectRatio > 0) {
            heightSpec = View.MeasureSpec.makeMeasureSpec((int) (widthSize / mAspectRatio + 0.5),
                    View.MeasureSpec.EXACTLY);
        } else {
            heightSpec = helper.getChildMeasureSpec(
                    helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom()
                            - getVerticalMargin() - getVerticalPadding(), params.height,
                    layoutInVertical);
        }

        helper.measureChild(view, widthSpec, heightSpec);

        OrientationHelper orientationHelper = helper.getMainOrientationHelper();
        result.mConsumed = orientationHelper.getDecoratedMeasurement(view) + startSpace + endSpace + gap;
        int left, top, right, bottom;
        if (helper.getOrientation() == VERTICAL) {
            // not support RTL now
            if (helper.isDoLayoutRTL()) {
                right = helper.getContentWidth() - helper.getPaddingRight() - mMarginRight - mPaddingRight;
                left = right - orientationHelper.getDecoratedMeasurementInOther(view);
            } else {
                left = helper.getPaddingLeft() + mMarginLeft + mPaddingLeft;
                right = left + orientationHelper.getDecoratedMeasurementInOther(view);
            }

            // whether this layout pass is layout to start or to end
            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                // fill start, from bottom to top
                bottom = layoutState.getOffset() - startSpace - (isStartLine ? 0 : mDividerHeight);
                top = bottom - orientationHelper.getDecoratedMeasurement(view);
            } else {
                // fill end, from top to bottom
                top = layoutState.getOffset() + startSpace + (isStartLine ? 0 : mDividerHeight);
                bottom = top + orientationHelper.getDecoratedMeasurement(view);
            }
        } else {
            top = helper.getPaddingTop() + mMarginTop + mPaddingTop;
            bottom = top + orientationHelper.getDecoratedMeasurementInOther(view);

            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                // fill left, from right to left
                right = layoutState.getOffset() - startSpace - (isStartLine ? 0 : mDividerHeight);
                left = right - orientationHelper.getDecoratedMeasurement(view);
            } else {
                // fill right, from left to right
                left = layoutState.getOffset() + startSpace + (isStartLine ? 0 : mDividerHeight);
                right = left + orientationHelper.getDecoratedMeasurement(view);
            }
        }
        // We calculate everything with View's bounding box (which includes decor and margins)
        // To calculate correct layout position, we subtract margins.
        layoutChild(view, left, top, right, bottom, helper);

        if (DEBUG) {
            Log.d(TAG, "laid out child at position " + helper.getPosition(view) + ", with l:"
                    + (left + params.leftMargin) + ", t:" + (top + params.topMargin) + ", r:"
                    + (right - params.rightMargin) + ", b:" + (bottom - params.bottomMargin));
        }

        handleStateOnResult(result, view);
    }

    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        if (isLayoutEnd) {
            if (offset == getItemCount() - 1) {
                return layoutInVertical ? mMarginBottom + mPaddingBottom : mMarginRight + mPaddingRight;
            }
        } else {
            if (offset == 0)
                return layoutInVertical ? -mMarginTop - mPaddingTop : -mMarginLeft - mPaddingLeft;
        }

        return super.computeAlignOffset(offset, isLayoutEnd, useAnchor, helper);
    }


}
