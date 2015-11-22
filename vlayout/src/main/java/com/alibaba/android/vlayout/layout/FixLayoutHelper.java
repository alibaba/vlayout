package com.alibaba.android.vlayout.layout;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;

import static android.support.v7.widget._ExposeLinearLayoutManagerEx.VERTICAL;

/**
 * Created by villadora on 15/8/18.
 */
public class FixLayoutHelper extends BaseLayoutHelper {

    private static final String TAG = "FixLayoutHelper";

    public static final int TOP_LEFT = 0;
    public static final int TOP_RIGHT = 1;
    public static final int BOTTOM_LEFT = 2;
    public static final int BOTTOM_RIGHT = 3;

    private int mZIndex = 1;

    private int mPos = -1;

    private int mAlignType = TOP_LEFT;

    protected int mX = 0;
    protected int mY = 0;

    protected View mFixView = null;

    protected boolean mDoNormalHandle = false;

    public FixLayoutHelper(int x, int y) {
        this(TOP_LEFT, x, y);
    }

    public FixLayoutHelper(int alignType, int x, int y) {
        this.mAlignType = alignType;
        this.mX = x;
        this.mY = y;
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
     * The margins in FixLayoutHelper are disabled
     */
    @Override
    public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
    }


    public void setAlignType(int alignType) {
        this.mAlignType = alignType;
    }


    /**
     * {@inheritDoc}
     * <p/>
     * Only start is used, use should not use this measured
     *
     * @param start position of items handled by this layoutHelper
     * @param end   will be ignored by {@link ScrollFixLayoutHelper}
     */
    @Override
    public void onRangeChange(int start, int end) {
        this.mPos = start;
        mFixView = null;
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                            VirtualLayoutManager.LayoutStateWrapper layoutState, LayoutChunkResult result,
                            final LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        // find view in currentPosition
        final View view = layoutState.next(recycler);
        if (view == null) {
            result.mFinished = true;
            return;
        }

        mDoNormalHandle = state.isPreLayout();

        if (mDoNormalHandle) {
            // in PreLayout do normal layout
            helper.addChildView(layoutState, view);
        }

        mFixView = view;

        doMeasureAndLayout(view, helper);


        result.mConsumed = 0;
        result.mIgnoreConsumed = true;

        handleStateOnResult(result, view);

    }

    @Override
    public boolean requireLayoutView() {
        return false;
    }

    @Override
    public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutManagerHelper helper) {
        super.beforeLayout(recycler, state, helper);

        if (mFixView != null) {
            // recycle view for later usage
            helper.removeChildView(mFixView);
            recycler.recycleView(mFixView);
            mFixView = null;
        }

        mDoNormalHandle = false;
    }

    @Override
    public void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                            int startPosition, int endPosition, int scrolled,
                            LayoutManagerHelper helper) {
        super.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);

        // disabled if mPos is negative number
        if (mPos < 0) return;

        if (mDoNormalHandle && state.isPreLayout()) {
            mFixView = null;
            return;
        }

        // Not in normal flow
        if (shouldBeDraw(startPosition, endPosition, scrolled)) {
            if (mFixView != null) {
                // already capture in layoutViews phase
                // if it's not shown on screen
                // TODO: nested scrollBy
                if (mFixView.getParent() == null) {
                    helper.addOffFlowView(mFixView, false);
                }
            } else {
                mFixView = recycler.getViewForPosition(mPos);
                doMeasureAndLayout(mFixView, helper);
                helper.addOffFlowView(mFixView, false);

            }
        }

    }

    protected boolean shouldBeDraw(int startPosition, int endPosition, int scrolled) {
        return true;
    }


    @Override
    public void clear(LayoutManagerHelper helper) {
        super.clear(helper);
        if (mFixView != null) {
            helper.removeChildView(mFixView);
            mFixView = null;
        }
    }

    private void doMeasureAndLayout(View view, LayoutManagerHelper helper) {
        if (view == null || helper == null) return;

        final VirtualLayoutManager.LayoutParams params = (VirtualLayoutManager.LayoutParams) view.getLayoutParams();
        params.positionType = VirtualLayoutManager.LayoutParams.PLACE_ABOVE;

        final OrientationHelper orientationHelper = helper.getMainOrientationHelper();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        final int widthSpec = helper.getChildMeasureSpec(
                helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight(), params.width, false);
        final int heightSpec = helper.getChildMeasureSpec(
                helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(), params.height, false);

        // do measurement
        helper.measureChild(view, widthSpec, heightSpec);

        int left, top, right, bottom;
        if (mAlignType == TOP_RIGHT) {
            top = helper.getPaddingTop() + mY;
            right = helper.getContentWidth() - helper.getPaddingRight() - mX;
            left = right - params.leftMargin - params.rightMargin - view.getMeasuredWidth();
            bottom = top + params.topMargin + params.bottomMargin + view.getMeasuredHeight();
        } else if (mAlignType == BOTTOM_LEFT) {
            left = helper.getPaddingLeft() + mX;
            bottom = helper.getContentHeight() - helper.getPaddingBottom() - mY;
            right = left + params.leftMargin + params.rightMargin + view.getMeasuredWidth();
            top = bottom - view.getMeasuredHeight() - params.topMargin - params.bottomMargin;
        } else if (mAlignType == BOTTOM_RIGHT) {
            right = helper.getContentWidth() - helper.getPaddingRight() - mX;
            bottom = helper.getContentHeight() - helper.getPaddingBottom() - mY;
            left = right - params.leftMargin - params.rightMargin - view.getMeasuredWidth();
            top = bottom - view.getMeasuredHeight() - params.topMargin - params.bottomMargin;
        } else {
            // TOP_LEFT
            left = helper.getPaddingLeft() + mX;
            top = helper.getPaddingTop() + mY;
            right = left + (layoutInVertical ? orientationHelper.getDecoratedMeasurementInOther(view) : orientationHelper.getDecoratedMeasurement(view));
            bottom = top + (layoutInVertical ? orientationHelper.getDecoratedMeasurement(view) : orientationHelper.getDecoratedMeasurementInOther(view));
        }


        layoutChild(view, left, top, right, bottom, helper);
    }


}
