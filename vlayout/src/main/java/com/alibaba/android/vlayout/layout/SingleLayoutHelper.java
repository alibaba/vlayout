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

import android.support.v7.widget.RecyclerView;
import android.view.View;

import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;

/**
 * LayoutHelper contains only one view
 */
public class SingleLayoutHelper extends ColumnLayoutHelper {

    private static final String TAG = "SingleLayoutHelper";

    private int mPos = -1;

    public SingleLayoutHelper() {
        setItemCount(1);
    }


    @Override
    public void setItemCount(int itemCount) {
        if (itemCount > 0)
            super.setItemCount(1);
        else
            super.setItemCount(0);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Only start is used, use should not use this measured
     *
     * @param start position of items handled by this layoutHelper
     * @param end   will be ignored by {@link SingleLayoutHelper}
     */
    @Override
    public void onRangeChange(int start, int end) {
        this.mPos = start;
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state, VirtualLayoutManager.LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        View view = layoutState.next(recycler);

        if (view == null) {
            result.mFinished = true;
            return;
        }


        helper.addChildView(layoutState, view);
        final VirtualLayoutManager.LayoutParams params = (VirtualLayoutManager.LayoutParams) view.getLayoutParams();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        int parentWidth = helper.getContentWidth() - helper.getPaddingLeft() - helper
                .getPaddingRight() - getHorizontalMargin() - getHorizontalPadding();
        int parentHeight = helper.getContentHeight() - helper.getPaddingTop() - helper
                .getPaddingBottom() - getVerticalMargin() - getVerticalPadding();

        if (!Float.isNaN(mAspectRatio)) {
            if (layoutInVertical) {
                parentHeight = (int) (parentWidth / mAspectRatio + 0.5f);
            } else {
                parentWidth = (int) (parentHeight * mAspectRatio + 0.5f);
            }
        }

        if (layoutInVertical) {
            final int widthSpec = helper.getChildMeasureSpec(parentWidth,
                     Float.isNaN(mAspectRatio) ? params.width : parentWidth, !layoutInVertical && Float.isNaN(mAspectRatio));
            final int heightSpec = helper.getChildMeasureSpec(parentHeight,
                    Float.isNaN(params.mAspectRatio) ? (Float.isNaN(mAspectRatio) ? params.height : parentHeight) : (int) (
                            parentWidth / params.mAspectRatio + 0.5f), layoutInVertical && Float.isNaN(mAspectRatio));

            // do measurement
            helper.measureChildWithMargins(view, widthSpec, heightSpec);
        } else {
            final int widthSpec = helper.getChildMeasureSpec(parentWidth,
                    Float.isNaN(params.mAspectRatio) ? (Float.isNaN(mAspectRatio) ? params.width : parentWidth) : (int) (
                            parentHeight * params.mAspectRatio + 0.5f), !layoutInVertical && Float.isNaN(mAspectRatio));
            final int heightSpec = helper.getChildMeasureSpec(parentHeight,
                     Float.isNaN(mAspectRatio) ? params.height : parentHeight, layoutInVertical && Float.isNaN(mAspectRatio));

            // do measurement
            helper.measureChildWithMargins(view, widthSpec, heightSpec);
        }

        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        result.mConsumed = orientationHelper.getDecoratedMeasurement(view);

        // do layout
        int left, top, right, bottom;
        if (layoutInVertical) {
            int viewWidth = orientationHelper.getDecoratedMeasurementInOther(view);
            int available = parentWidth - viewWidth;
            if (available < 0) {
                available = 0;
            }

            left = mMarginLeft + mPaddingLeft + helper.getPaddingLeft() + available / 2;
            right = helper.getContentWidth() - mMarginRight - mPaddingRight - helper.getPaddingRight() - available / 2;


            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                bottom = layoutState.getOffset() - mMarginBottom - mPaddingBottom;
                top = bottom - result.mConsumed;
            } else {
                top = layoutState.getOffset() + mMarginTop + mPaddingTop;
                bottom = top + result.mConsumed;
            }
        } else {
            int viewHeight = orientationHelper.getDecoratedMeasurementInOther(view);
            int available = parentHeight - viewHeight;
            if (available < 0) {
                available = 0;
            }

            top = helper.getPaddingTop() + mMarginTop + mPaddingTop + available / 2;
            bottom = helper.getContentHeight() - -mMarginBottom - mPaddingBottom - helper.getPaddingBottom() - available / 2;

            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                right = layoutState.getOffset() - mMarginRight - mPaddingRight;
                left = right - result.mConsumed;
            } else {
                left = layoutState.getOffset() + mMarginLeft + mPaddingLeft;
                right = left + result.mConsumed;
            }
        }

        if (layoutInVertical) {
            result.mConsumed += getVerticalMargin() + getVerticalPadding();
        } else {
            result.mConsumed += getHorizontalMargin() + getHorizontalPadding();
        }

        layoutChildWithMargin(view, left, top, right, bottom, helper);
    }

}
