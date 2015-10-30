package com.alibaba.android.vlayout.layout;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;

import static android.support.v7.widget._ExposeLinearLayoutManagerEx.VERTICAL;

/**
 * Created by villadora on 15/8/10.
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

        // find corresponding layout container
        View view = nextView(recycler, layoutState, helper, result);
        if (view == null) {
            return;
        }

        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        int widthSpec = helper.getChildMeasureSpec(helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight(), params.width, !layoutInVertical);
        int heightSpec = helper.getChildMeasureSpec(helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(), params.height, layoutInVertical);

        helper.measureChild(view, widthSpec, heightSpec);

        OrientationHelper orientationHelper = helper.getMainOrientationHelper();
        result.mConsumed = orientationHelper.getDecoratedMeasurement(view) + mDividerHeight;

        int left, top, right, bottom;
        if (helper.getOrientation() == VERTICAL) {
            // not support RTL now
            if (helper.isDoLayoutRTL()) {
                right = helper.getContentWidth() - helper.getPaddingRight();
                left = right - orientationHelper.getDecoratedMeasurementInOther(view);
            } else {
                left = helper.getPaddingLeft();
                right = left + orientationHelper.getDecoratedMeasurementInOther(view);
            }

            // whether this layout pass is layout to start or to end
            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                // fill start, from bottom to top
                bottom = layoutState.getOffset() - mDividerHeight;
                top = layoutState.getOffset() - result.mConsumed;
            } else {
                // fill end, from top to bottom
                top = layoutState.getOffset() + mDividerHeight;
                bottom = layoutState.getOffset() + result.mConsumed;
            }
        } else {
            top = helper.getPaddingTop();
            bottom = top + orientationHelper.getDecoratedMeasurementInOther(view);

            if (layoutState.getLayoutDirection() == VirtualLayoutManager.LayoutStateWrapper.LAYOUT_START) {
                right = layoutState.getOffset() - mDividerHeight;
                left = layoutState.getOffset() - result.mConsumed;
            } else {
                left = layoutState.getOffset() + mDividerHeight;
                right = layoutState.getOffset() + result.mConsumed;
            }
        }
        // We calculate everything with View's bounding box (which includes decor and margins)
        // To calculate correct layout position, we subtract margins.
        helper.layoutChild(view, left, top, right, bottom);

        mLayoutRegion.union(left, top, right, bottom);

        if (DEBUG) {
            Log.d(TAG, "laid out child at position " + helper.getPosition(view) + ", with l:"
                    + (left + params.leftMargin) + ", t:" + (top + params.topMargin) + ", r:"
                    + (right - params.rightMargin) + ", b:" + (bottom - params.bottomMargin));
        }

        handleStateOnResult(result, view);
    }

}
