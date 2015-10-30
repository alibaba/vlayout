package com.alibaba.android.vlayout.layout;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import static android.support.v7.widget._ExposeLinearLayoutManagerEx.VERTICAL;


/**
 * Layout which allow item sticky to start/end
 *
 * @author villadora
 * @since 1.0.0
 */
// TODO: make stack animation
public class StickyLayoutHelper extends BaseLayoutHelper {

    private static final String TAG = "StickyStartLayoutHelper";

    private int mPos = -1;

    private boolean mStickyStart = true;

    private View mFixView = null;
    private boolean mDoNormalHandle = false;


    public StickyLayoutHelper() {
        this(true);
    }

    public StickyLayoutHelper(boolean stickyStart) {
        this.mStickyStart = stickyStart;
        setItemCount(1);
    }

    public void setStickyStart(boolean stickyStart) {
        this.mStickyStart = stickyStart;
    }


    @Override
    public void setItemCount(int itemCount) {
        if (itemCount > 0)
            super.setItemCount(1);
        else
            super.setItemCount(0);
    }

    @Override
    public void onRangeChange(int start, int end) {
        mPos = start;
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
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


        doMeasure(view, helper);


        // do layout
        final OrientationHelper orientationHelper = helper.getMainOrientationHelper();
        result.mConsumed = orientationHelper.getDecoratedMeasurement(view);

        VirtualLayoutManager.LayoutParams params = (VirtualLayoutManager.LayoutParams) view.getLayoutParams();
        // Do normal measure&layout phase by default
        mDoNormalHandle = true;

        final int remainingSpace = layoutState.getAvailable() - result.mConsumed;

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
            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                // fill start, from bottom to top
                bottom = layoutState.getOffset();
                top = layoutState.getOffset() - result.mConsumed;
            } else {
                // fill end, from top to bottom
                top = layoutState.getOffset();
                bottom = layoutState.getOffset() + result.mConsumed;
            }


            if (helper.getReverseLayout() || !mStickyStart) {
                if (remainingSpace < 0 && layoutState.getItemDirection() == LayoutStateWrapper.ITEM_DIRECTION_TAIL) {
                    mDoNormalHandle = false;
                    mFixView = view;

                    bottom = helper.getContentHeight();
                    top = bottom - result.mConsumed;
                }
            } else {
                // should not use 0
                if (remainingSpace < 0 && layoutState.getItemDirection() == LayoutStateWrapper.ITEM_DIRECTION_HEAD) {
                    mDoNormalHandle = false;
                    mFixView = view;
                    top = 0;
                    bottom = result.mConsumed;
                }

                // Log.i("TEST", "fastScroll: " + layoutState.getScrollingOffset());
            }

        } else {
            top = helper.getPaddingTop();
            bottom = top + orientationHelper.getDecoratedMeasurementInOther(view);

            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                right = layoutState.getOffset();
                left = layoutState.getOffset() - result.mConsumed;
            } else {
                left = layoutState.getOffset();
                right = layoutState.getOffset() + result.mConsumed;
            }
            if (helper.getReverseLayout() || !mStickyStart) {
                if (remainingSpace < 0) {
                    mDoNormalHandle = false;
                    mFixView = view;

                    right = helper.getContentWidth();
                    left = right - result.mConsumed;
                }
            } else {
                if (remainingSpace < 0) {
                    mDoNormalHandle = false;
                    mFixView = view;
                    left = 0;
                    right = result.mConsumed;
                }
            }

        }


        layoutChild(view, left, top, right, bottom, helper);

        if (state.isPreLayout()) {
            mDoNormalHandle = true;
        }

        if (mDoNormalHandle) {
            helper.addChildView(layoutState, view);
            mFixView = null;
        }

        handleStateOnResult(result, view);
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
    public boolean requireLayoutView() {
        return false;
    }

    @Override
    public void afterFinishLayout(RecyclerView.Recycler recycler, RecyclerView.State state, int startPosition, int endPosition, int scrolled,
                                  LayoutManagerHelper helper) {
        super.afterFinishLayout(recycler, state, startPosition, endPosition, scrolled, helper);

        // disabled if mPos is negative number
        if (mPos < 0) return;

        if (!mDoNormalHandle && mPos >= startPosition && mPos <= endPosition) {
            // Log.i("TEST", "abnormal pos: " + mPos + " start: " + startPosition + " end: " + endPosition);
        }

        if (mDoNormalHandle) {
            mFixView = null;
            return;
        }

        // Not in normal flow
        if (mFixView != null) {
            // already capture in layoutViews phase
            // if it's not shown on screen
            if (mFixView.getParent() == null) {
                helper.addOffFlowView(mFixView, false);
            }
        } else {
            if ((mStickyStart && startPosition >= mPos) || (!mStickyStart && endPosition <= mPos)) {
                mFixView = recycler.getViewForPosition(mPos);

                VirtualLayoutManager.LayoutParams params = (VirtualLayoutManager.LayoutParams) mFixView.getLayoutParams();

                if (params.isItemRemoved()) {
                    // item is removed
                    return;
                }

                // when do measure in after layout no need to consider scrolled
                doMeasure(mFixView, helper);

                // do layout
                final OrientationHelper orientationHelper = helper.getMainOrientationHelper();
                int consumed = orientationHelper.getDecoratedMeasurement(mFixView);


                int left, top, right, bottom;
                if (helper.getOrientation() == VERTICAL) {
                    // not support RTL now
                    if (helper.isDoLayoutRTL()) {
                        right = helper.getContentWidth() - helper.getPaddingRight();
                        left = right - orientationHelper.getDecoratedMeasurementInOther(mFixView);
                    } else {
                        left = helper.getPaddingLeft();
                        right = left + orientationHelper.getDecoratedMeasurementInOther(mFixView);
                    }

                    if (helper.getReverseLayout() || !mStickyStart) {
                        bottom = helper.getContentHeight();
                        top = bottom - consumed;
                    } else {
                        top = 0;
                        bottom = consumed;
                    }

                } else {
                    top = helper.getPaddingTop();
                    bottom = top + orientationHelper.getDecoratedMeasurementInOther(mFixView);

                    if (helper.getReverseLayout() || !mStickyStart) {
                        right = helper.getContentWidth();
                        left = right - consumed;
                    } else {
                        left = 0;
                        right = consumed;
                    }

                }

                layoutChild(mFixView, left, top, right, bottom, helper);

                helper.addOffFlowView(mFixView, false);
            }
        }


    }


    @Override
    public void clear(LayoutManagerHelper helper) {
        super.clear(helper);
        if (mFixView != null)
            helper.removeChildView(mFixView);
    }

    private void doMeasure(View view, LayoutManagerHelper helper) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        final int widthSpec = helper.getChildMeasureSpec(helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight(),
                params.width, !layoutInVertical);
        final int heightSpec = helper.getChildMeasureSpec(helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                params.height, layoutInVertical);

        helper.measureChild(view, widthSpec, heightSpec);
    }
}

