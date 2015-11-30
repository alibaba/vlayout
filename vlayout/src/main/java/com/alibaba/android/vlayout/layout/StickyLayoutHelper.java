package com.alibaba.android.vlayout.layout;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

        final int remainingSpace = layoutState.getAvailable() - result.mConsumed + layoutState.getExtra();

        int left, top, right, bottom;
        if (helper.getOrientation() == VERTICAL) {
            // not support RTL now
            if (helper.isDoLayoutRTL()) {
                right = helper.getContentWidth() - helper.getPaddingRight() - mMarginRight;
                left = right - orientationHelper.getDecoratedMeasurementInOther(view);
            } else {
                left = helper.getPaddingLeft() + mMarginLeft;
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

                    bottom = orientationHelper.getEndAfterPadding();
                    top = bottom - result.mConsumed;
                }
            } else {
                // should not use 0
                if (remainingSpace < 0 && layoutState.getItemDirection() == LayoutStateWrapper.ITEM_DIRECTION_HEAD) {
                    mDoNormalHandle = false;
                    mFixView = view;
                    top = orientationHelper.getStartAfterPadding();
                    bottom = top + result.mConsumed;
                }
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

                    right = orientationHelper.getEndAfterPadding();
                    left = right - result.mConsumed;
                }
            } else {
                if (remainingSpace < 0) {
                    mDoNormalHandle = false;
                    mFixView = view;
                    left = orientationHelper.getStartAfterPadding();
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
            handleStateOnResult(result, view);
            mFixView = null;
        }


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
    public void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state, int startPosition, int endPosition, int scrolled,
                            LayoutManagerHelper helper) {
        super.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);

        // disabled if mPos is negative number
        if (mPos < 0) return;

        if (!mDoNormalHandle && mPos >= startPosition && mPos <= endPosition) {
            // Log.i("TEST", "abnormal pos: " + mPos + " start: " + startPosition + " end: " + endPosition);
        }

        if (mDoNormalHandle || state.isPreLayout()) {
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
            final View eView = helper.findViewByPosition(mPos);
            final OrientationHelper orientationHelper = helper.getMainOrientationHelper();
            boolean normalHandle = false;
            if ((mStickyStart && startPosition >= mPos) || (!mStickyStart && endPosition <= mPos)) {

                if (eView == null) {
                    mFixView = recycler.getViewForPosition(mPos);
                } else if (mStickyStart && orientationHelper.getDecoratedStart(eView) >= orientationHelper.getStartAfterPadding()) {
                    return;
                } else if (!mStickyStart && orientationHelper.getDecoratedEnd(eView) <= orientationHelper.getEndAfterPadding()) {
                    return;
                } else {
                    // TODO: reuse views
                    // mFixView = recycler.getViewForPosition(mPos);
                    mFixView = eView;
                }
            } else {
                if ((mStickyStart && endPosition >= mPos) || (!mStickyStart && startPosition <= mPos)) {
                    Log.i("STICKY", String.format("%s %d %d %d", mStickyStart, mPos, startPosition, endPosition));
                    if (eView == null) {
                        normalHandle = true;
                        mFixView = recycler.getViewForPosition(mPos);
                    }
                }
            }

            if (mFixView != null) {
                VirtualLayoutManager.LayoutParams params = (VirtualLayoutManager.LayoutParams) mFixView.getLayoutParams();

                if (params.isItemRemoved()) {
                    // item is removed
                    return;
                }

                // when do measure in after layout no need to consider scrolled
                doMeasure(mFixView, helper);

                // do layout

                int consumed = orientationHelper.getDecoratedMeasurement(mFixView);


                int left = 0, top = 0, right = 0, bottom = 0;
                int index = -1;
                if (helper.getOrientation() == VERTICAL) {
                    // not support RTL now
                    if (helper.isDoLayoutRTL()) {
                        right = helper.getContentWidth() - helper.getPaddingRight();
                        left = right - orientationHelper.getDecoratedMeasurementInOther(mFixView);
                    } else {
                        left = helper.getPaddingLeft();
                        right = left + orientationHelper.getDecoratedMeasurementInOther(mFixView);
                    }

                    if (normalHandle) {
                        View refer = null;
                        if (mStickyStart) {
                            for (int i = 0; i < helper.getChildCount(); i++) {
                                refer = helper.getChildAt(i);
                                int pos = helper.getPosition(refer);
                                if (pos == mPos - 1) { // TODO: when view size is larger than totalSpace!
                                    top = orientationHelper.getDecoratedEnd(refer);
                                    bottom = top + consumed;
                                    index = i + 1;
                                    break;
                                }
                            }
                        } else {
                            for (int i = helper.getChildCount() - 1; i >= 0; i--) {
                                refer = helper.getChildAt(i);
                                int pos = helper.getPosition(refer);
                                if (pos == mPos + 1) {
                                    bottom = orientationHelper.getDecoratedStart(refer);
                                    top = bottom - consumed;
                                    index = i;
                                    break;
                                }
                            }
                        }
                    } else if (helper.getReverseLayout() || !mStickyStart) {
                        bottom = helper.getContentHeight();
                        top = bottom - consumed;
                    } else {
                        top = helper.getPaddingTop();
                        bottom = top + consumed;
                    }

                } else {
                    top = helper.getPaddingTop();
                    bottom = top + orientationHelper.getDecoratedMeasurementInOther(mFixView);

                    if (normalHandle) {
                        View refer = null;
                        if (mStickyStart) {
                            for (int i = 0; i < helper.getChildCount(); i++) {
                                refer = helper.getChildAt(i);
                                int pos = helper.getPosition(refer);
                                if (pos == mPos - 1) { // TODO: when view size is larger than totalSpace!
                                    left = orientationHelper.getDecoratedEnd(refer);
                                    right = left + consumed;
                                    break;
                                }
                            }
                        } else {
                            for (int i = helper.getChildCount() - 1; i >= 0; i--) {
                                refer = helper.getChildAt(i);
                                int pos = helper.getPosition(refer);
                                if (pos == mPos + 1) {
                                    right = orientationHelper.getDecoratedStart(refer);
                                    left = right - consumed;
                                    break;
                                }
                            }
                        }
                    } else if (helper.getReverseLayout() || !mStickyStart) {
                        right = helper.getContentWidth();
                        left = right - consumed;
                    } else {
                        left = helper.getPaddingLeft();
                        right = left + consumed;
                    }

                }

                layoutChild(mFixView, left, top, right, bottom, helper);

                if (normalHandle) {
                    if (index >= 0) {
                        helper.addChildView(mFixView, index);
                        mFixView = null;
                    }
                } else
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
        final int widthSize = helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight() - getHorizontalMargin();

        final int widthSpec = helper.getChildMeasureSpec(widthSize, params.width, !layoutInVertical);
        int heightSpec;
        if (!Float.isNaN(mAspectRatio) && mAspectRatio > 0) {
            heightSpec = View.MeasureSpec.makeMeasureSpec((int) (widthSize / mAspectRatio + 0.5), View.MeasureSpec.EXACTLY);
        } else
            heightSpec = helper.getChildMeasureSpec(helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                    params.height, layoutInVertical);

        helper.measureChild(view, widthSpec, heightSpec);
    }
}

