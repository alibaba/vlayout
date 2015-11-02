package com.alibaba.android.vlayout.layout;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;

import static android.support.v7.widget._ExposeLinearLayoutManagerEx.VERTICAL;

/**
 * Created by villadora on 15/8/31.
 */
public class SingleLayoutHelper extends BaseLayoutHelper {

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
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        int parentWidth = helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight() - mMarginLeft - mMarginRight;
        int parentHeight = helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom() - mMarginTop - mMarginBottom;

        if (!Float.isNaN(mAspectRatio)) {
            if (layoutInVertical) {
                parentHeight = (int) (parentWidth * mAspectRatio);
            } else {
                parentWidth = (int) (parentHeight * mAspectRatio);
            }
        }

        final int widthSpec = helper.getChildMeasureSpec(parentWidth,
                Float.isNaN(mAspectRatio) ? params.width : parentWidth, !layoutInVertical && Float.isNaN(mAspectRatio));
        final int heightSpec = helper.getChildMeasureSpec(parentHeight,
                Float.isNaN(mAspectRatio) ? params.height : parentHeight, layoutInVertical && Float.isNaN(mAspectRatio));

        // do measurement
        helper.measureChild(view, widthSpec, heightSpec);

        OrientationHelper orientationHelper = helper.getMainOrientationHelper();

        result.mConsumed = orientationHelper.getDecoratedMeasurement(view);

        // do layout
        int left, top, right, bottom;
        if (layoutInVertical) {
            int viewWidth = orientationHelper.getDecoratedMeasurementInOther(view);
            int available = helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight() - viewWidth;
            if (available < 0) {
                available = 0;
            }

            left = mMarginLeft + helper.getPaddingLeft() + available / 2;
            right = helper.getContentWidth() - mMarginRight - helper.getPaddingRight() - available / 2;


            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                bottom = layoutState.getOffset() - mMarginBottom;
                top = bottom - result.mConsumed;
            } else {
                top = layoutState.getOffset() + mMarginTop;
                bottom = top + result.mConsumed;
            }
        } else {
            int viewHeight = orientationHelper.getDecoratedMeasurementInOther(view);
            int available = helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom() - viewHeight;
            if (available < 0) {
                available = 0;
            }

            top = helper.getPaddingTop() + mMarginTop + available / 2;
            bottom = helper.getContentHeight() - -mMarginBottom - helper.getPaddingBottom() - available / 2;

            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                right = layoutState.getOffset() - mMarginRight;
                left = right - result.mConsumed;
            } else {
                left = layoutState.getOffset() + mMarginLeft;
                right = left + result.mConsumed;
            }
        }

        result.mConsumed += mMarginTop + mMarginBottom;

        layoutChild(view, left, top, right, bottom, helper, true);
    }

    @Override
    public int getExtraMargin(int offset, boolean isLayoutEnd, boolean layoutInVertical) {
        if (layoutInVertical) {
            if (isLayoutEnd) {
                return mMarginBottom;
            } else {
                return -mMarginTop;
            }
        } else {
            if (isLayoutEnd) {
                return mMarginRight;
            } else {
                return -mMarginLeft;
            }
        }
    }
}
