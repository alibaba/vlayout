package com.alibaba.android.vlayout.layout;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;

import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;


/**
 * Created by villadora on 15/8/18.
 */
public class FixLayoutHelper extends FixAreaLayoutHelper {

    private static final String TAG = "FixLayoutHelper";

    public static final int TOP_LEFT = 0;
    public static final int TOP_RIGHT = 1;
    public static final int BOTTOM_LEFT = 2;
    public static final int BOTTOM_RIGHT = 3;

    private int mPos = -1;

    private int mAlignType = TOP_LEFT;

    protected int mX = 0;
    protected int mY = 0;

    private boolean mSketchMeasure = false;

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


    public void setX(int x) {
        this.mX = x;
    }

    public void setY(int y) {
        this.mY = y;
    }

    public void setAlignType(int alignType) {
        this.mAlignType = alignType;
    }


    public void setSketchMeasure(boolean sketchMeasure) {
        mSketchMeasure = sketchMeasure;
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
        View view = mFixView;
        if (view == null)
            view = layoutState.next(recycler);
        else {
            layoutState.skipCurrentPosition();
        }

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

        if (mFixView != null && helper.isViewHolderUpdated(mFixView)) {
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
        if (mPos < 0) {
            return;
        }

        if (mDoNormalHandle && state.isPreLayout()) {
            if (mFixView != null) {
                helper.removeChildView(mFixView);
                recycler.recycleView(mFixView);
            }

            mFixView = null;
            return;
        }

        // Not in normal flow
        if (shouldBeDraw(startPosition, endPosition, scrolled)) {
            if (mFixView != null) {
                // already capture in layoutViews phase
                // if it's not shown on screen
                if (mFixView.getParent() == null) {
                    helper.addFixedView(mFixView);
                } else {
                    helper.showView(mFixView);
                    // helper.removeChildView(mFixView);
                    helper.addFixedView(mFixView);
                }
            } else {
                mFixView = recycler.getViewForPosition(mPos);
                doMeasureAndLayout(mFixView, helper);
                helper.addFixedView(mFixView);
            }
        } else {
            if (mFixView != null) {
                helper.removeChildView(mFixView);
                recycler.recycleView(mFixView);
                mFixView = null;
            }
        }

    }

    /**
     * Decide whether the view should be shown
     *
     * @param startPosition the first visible position in RecyclerView
     * @param endPosition   the last visible position in RecyclerView
     * @param scrolled      how many pixels will be scrolled during this scrolling, 0 during layouting
     * @return Whether the view in current layoutHelper should be shown
     */
    protected boolean shouldBeDraw(int startPosition, int endPosition, int scrolled) {
        return true;
    }

    @Override
    public View getFixedView() {
        return mFixView;
    }

    @Override
    public void onClear(LayoutManagerHelper helper) {
        super.onClear(helper);
        if (mFixView != null) {
            helper.removeChildView(mFixView);
            helper.recycleView(mFixView);
            mFixView = null;
        }
    }

    private void doMeasureAndLayout(View view, LayoutManagerHelper helper) {
        if (view == null || helper == null) return;

        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        final OrientationHelper orientationHelper = helper.getMainOrientationHelper();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        final int widthSpec = helper.getChildMeasureSpec(
                helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight(),
                params.width >= 0 ? params.width : ((mSketchMeasure && layoutInVertical) ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT), false);
        final int heightSpec = helper.getChildMeasureSpec(
                helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                params.height >= 0 ? params.height : ((mSketchMeasure && !layoutInVertical) ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT), false);

        // do measurement
        helper.measureChild(view, widthSpec, heightSpec);

        int left, top, right, bottom;
        if (mAlignType == TOP_RIGHT) {
            top = helper.getPaddingTop() + mY + mAdjuster.top;
            right = helper.getContentWidth() - helper.getPaddingRight() - mX - mAdjuster.right;
            left = right - params.leftMargin - params.rightMargin - view.getMeasuredWidth();
            bottom = top + params.topMargin + params.bottomMargin + view.getMeasuredHeight();
        } else if (mAlignType == BOTTOM_LEFT) {
            left = helper.getPaddingLeft() + mX + mAdjuster.left;
            bottom = helper.getContentHeight() - helper.getPaddingBottom() - mY - mAdjuster.bottom;
            right = left + params.leftMargin + params.rightMargin + view.getMeasuredWidth();
            top = bottom - view.getMeasuredHeight() - params.topMargin - params.bottomMargin;
        } else if (mAlignType == BOTTOM_RIGHT) {
            right = helper.getContentWidth() - helper.getPaddingRight() - mX - mAdjuster.right;
            bottom = helper.getContentHeight() - helper.getPaddingBottom() - mY - mAdjuster.bottom;
            left = right - params.leftMargin - params.rightMargin - view.getMeasuredWidth();
            top = bottom - view.getMeasuredHeight() - params.topMargin - params.bottomMargin;
        } else {
            // TOP_LEFT
            left = helper.getPaddingLeft() + mX + mAdjuster.left;
            top = helper.getPaddingTop() + mY + mAdjuster.top;
            right = left + (layoutInVertical ? orientationHelper.getDecoratedMeasurementInOther(view) : orientationHelper.getDecoratedMeasurement(view));
            bottom = top + (layoutInVertical ? orientationHelper.getDecoratedMeasurement(view) : orientationHelper.getDecoratedMeasurementInOther(view));
        }


        layoutChild(view, left, top, right, bottom, helper);
    }


}
