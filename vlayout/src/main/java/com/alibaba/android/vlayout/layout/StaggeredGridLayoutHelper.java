package com.alibaba.android.vlayout.layout;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.Range;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import static com.alibaba.android.vlayout.VirtualLayoutManager.HORIZONTAL;
import static com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper.LAYOUT_END;
import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;

/**
 * Created by villadora on 15/10/28.
 */
public class StaggeredGridLayoutHelper extends BaseLayoutHelper {

    private int mNumLanes = 0;

    private Lane[] mLanes;

    private int mHorizontalGap = 0;
    private int mVerticalGap = 0;


    // length specs
    private int mColLength = 0;
    private int mEachGap = 0;
    private int mLastGap = 0;


    private BitSet mRemainingSpans = null;

    public StaggeredGridLayoutHelper(int lanes) {
        this(lanes, 0);
    }

    public StaggeredGridLayoutHelper(int lanes, int gap) {
        this(lanes, gap, gap);
    }

    public StaggeredGridLayoutHelper(int lanes, int horizontalGap, int verticalGap) {
        this.mNumLanes = lanes;
        ensureLanes();
        mHorizontalGap = horizontalGap;
        mVerticalGap = verticalGap;
    }


    private void ensureLanes() {
        if (mLanes == null || mLanes.length != mNumLanes || mRemainingSpans == null) {
            mLanes = new Lane[mNumLanes];
            mRemainingSpans = new BitSet(mNumLanes);
            for (int i = 0; i < mLanes.length; i++) {
                mLanes[i] = new Lane();
            }
        }
    }

    @Override
    public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutManagerHelper helper) {
        super.beforeLayout(recycler, state, helper);

        if (helper.getOrientation() == VERTICAL) {
            int availableWidth = helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight();
            mColLength = (int) ((availableWidth - mHorizontalGap * (mNumLanes - 1)) / mNumLanes + 0.5);
            int totalGaps = availableWidth - mColLength * mNumLanes;
            mEachGap = (int) (totalGaps / (mNumLanes - 1) + 0.5);
            mLastGap = totalGaps - (mNumLanes - 2) * mEachGap;
        }
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                            VirtualLayoutManager.LayoutStateWrapper layoutState, LayoutChunkResult result,
                            LayoutManagerHelper helper) {
        int position = layoutState.getCurrentPosition();
        if (isOutOfRange(position)) {
            return;
        }


        ensureLanes();

        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        final OrientationHelper orientationHelper = helper.getMainOrientationHelper();

        final int targetLine;
        final int recycleLine;

        // Line of the furthest row.
        if (layoutState.getLayoutDirection() == LAYOUT_END) {
            // ignore padding for recycler
            recycleLine = orientationHelper.getEndAfterPadding() + layoutState.getAvailable();
            targetLine = recycleLine + layoutState.getExtra() + orientationHelper.getEndPadding();
        } else { // LAYOUT_START
            // ignore padding for recycler
            recycleLine = orientationHelper.getStartAfterPadding() - layoutState.getAvailable();
            targetLine = recycleLine - layoutState.getExtra() - orientationHelper.getStartAfterPadding();
        }

        mRemainingSpans.set(0, mNumLanes, true);


        while (layoutState.hasMore(state) && !mRemainingSpans.isEmpty()) {
            View view = layoutState.next(recycler);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

            if (layoutInVertical) {
                int widthSpec = helper.getChildMeasureSpec(mColLength, params.width, false);
                int heightSpec = helper.getChildMeasureSpec(helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                        params.height, true);

                helper.measureChild(view, widthSpec, heightSpec);

                final int laneNum = findLane(layoutState.getLayoutDirection() == LAYOUT_END);
                if (laneNum < 0) {
                    throw new IllegalArgumentException("Can not find suitable lane: " + Arrays.deepToString(mLanes));
                }

                Lane lane = mLanes[laneNum];
                final Range<Integer> range = getRange();
                if (layoutState.getLayoutDirection() == LAYOUT_END) {
                    // offset always be the longest child with space
                    int offset = layoutState.getOffset();

                    int top = lane.end + mVerticalGap;
                    int left = (mEachGap + mColLength) * laneNum;
                    if (laneNum == mNumLanes - 1) {
                        // is last lane
                        left = left - mEachGap + mLastGap;
                    }

                    int bottom = top + orientationHelper.getDecoratedMeasurement(view);
                    int right = left + orientationHelper.getDecoratedMeasurementInOther(view);

                    layoutChild(view, left, top, right, bottom, helper);

                    lane.end = bottom;


                    int highLane = -1;
                    if (position == range.getUpper()) {
                        // if last item,  update consumed based on offset
                        highLane = findLane(true, false);

                    } else {
                        // have more items
                        highLane = findLane(true, true);
                    }

                    if (highLane < 0) {
                        throw new IllegalArgumentException("Can not find suitable lane: " + Arrays.deepToString(mLanes));
                    }


                    int highEnd = mLanes[highLane].end;
                    result.mConsumed = (highEnd <= offset) ? 0 : (highEnd - offset);
                } else {
                    int offset = layoutState.getOffset();

                    int bottom = lane.begin - mVerticalGap;
                    int left = (mEachGap + mColLength) * laneNum;
                    if (laneNum == mNumLanes - 1) {
                        // is last lane
                        left = left - mEachGap + mLastGap;
                    }

                    int top = bottom - orientationHelper.getDecoratedMeasurement(view);
                    int right = left + orientationHelper.getDecoratedMeasurementInOther(view);

                    layoutChild(view, left, top, right, bottom, helper);

                    lane.begin = top;

                    int lowLane = -1;
                    if (position == range.getLower()) {
                        // if last item,  update consumed based on offset
                        lowLane = findLane(false, true);
                    } else {
                        // have more items
                        lowLane = findLane(false, false);
                    }

                    if (lowLane < 0) {
                        throw new IllegalArgumentException("Can not find suitable lane: " + Arrays.deepToString(mLanes));
                    }


                    int lowBegin = mLanes[lowLane].begin;
                    result.mConsumed = (lowBegin >= offset) ? 0 : (offset - lowBegin);
                }
                handleStateOnResult(result, view);
            }


        }


    private int findLane(boolean byEnd) {
        return findLane(byEnd, byEnd);
    }

    private int findLane(boolean byEnd, boolean byMin) {
        int rs = -1;

        int limit = byMin ? Integer.MAX_VALUE : Integer.MIN_VALUE;

        for (int i = 0; i < mLanes.length; i++) {
            Lane lane = mLanes[i];
            int lvl = byEnd ? lane.end : lane.begin;

            if (byMin) {
                if (lvl < limit) {
                    limit = lvl;
                    rs = i;
                }
            } else {
                if (lvl > limit) {
                    limit = lvl;
                    rs = i;
                }
            }
        }

        return rs;
    }

    @Override
    public void onScrollStateChanged(int state, LayoutManagerHelper helper) {

    }


    @Override
    public int getExtraMargin(int offset, boolean isLayoutEnd, boolean layoutInVertical) {
        if (layoutInVertical) {
            // in middle nothing need to do
            if (isLayoutEnd) {
                int lane = findLane(true, offset != getItemCount() - 1);
                return mLanes[lane].end;
            } else {
                int lane = findLane(false, offset != 0);
                return mLanes[lane].begin;
            }
        }

        return 0;
    }

    @Override
    public void clear(LayoutManagerHelper helper) {
        super.clear(helper);
        mLanes = null;
    }


    @Override
    public boolean isRecyclable(int childPos, int startIndex, int endIndex, LayoutManagerHelper helper) {
        boolean recycled = super.isRecyclable(childPos, startIndex, endIndex, helper);
        if (recycled) {
            View child = helper.getChildAt(childPos);
            if (child != null) {
                boolean isVertical = helper.getOrientation() == VERTICAL;
                OrientationHelper orientationHelper = helper.getMainOrientationHelper();

                if (isVertical) {
                    int availableWidth = helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight();
                    int top = child.getTop();
                    int bottom = child.getBottom();
                    int left = child.getLeft();

                    final int colWidth = (int) ((availableWidth - mHorizontalGap * (mNumLanes - 1)) / mNumLanes + 0.5);

                    int numLane = (int) ((left / (colWidth + mHorizontalGap)) + 0.5);
                    if (numLane < 0 || numLane >= mLanes.length) {
                        throw new IllegalArgumentException("Can not find correct lane with left position: " + left);
                    }

                    Lane lane = mLanes[numLane];

                    if (lane.begin <= top) {
                        lane.begin = bottom + mVerticalGap;
                    } else if (lane.end >= bottom) {
                        lane.end = top - mVerticalGap;
                    }
                }
            }
        }

        return recycled;
    }

    @Override
    public void checkAnchorInfo(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo) {
        super.checkAnchorInfo(state, anchorInfo);
        // refresh layout

        // if position is visible
        // find first view


        // else
        // TODO: clear span cache and views in span

        mLanes = null;
        ensureLanes();

    }

    @Override
    public void onSaveState(Bundle bundle) {
        super.onSaveState(bundle);
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
    }


    @Override
    public void offsetChildrenVertical(int dy, LayoutManagerHelper helper) {
        super.offsetChildrenVertical(dy, helper);
        if (helper.getOrientation() == VERTICAL) {
            for (Lane lane : mLanes) {
                lane.offset(dy);
            }
        }
    }

    @Override
    public void offsetChildrenHorizontal(int dx, LayoutManagerHelper helper) {
        super.offsetChildrenHorizontal(dx, helper);
        if (helper.getOrientation() == HORIZONTAL) {
            for (Lane lane : mLanes) {
                lane.offset(dx);
            }
        }
    }

    static class Lane {
        int begin;
        int end;

        void offset(int dx) {
            begin += dx;
            end += dx;
        }
    }


    // Package scoped to access from tests.
    static class Span {

        static final int INVALID_LINE = Integer.MIN_VALUE;
        static final int INVALID_OFFSET = Integer.MIN_VALUE;
        private ArrayList<View> mViews = new ArrayList<View>();
        int mCachedStart = INVALID_LINE;
        int mCachedEnd = INVALID_LINE;
        int mDeletedSize = 0;
        final int mIndex;

        private Span(int index) {
            mIndex = index;
        }

        void calculateCachedStart(@NonNull OrientationHelper helper) {
            if (mViews.size() == 0) {
                mCachedStart = INVALID_LINE;
            } else {
                final View startView = mViews.get(0);
                mCachedStart = helper.getDecoratedStart(startView);
            }
        }

        // Use this one when default value does not make sense and not having a value means a bug.
        int getStartLine(OrientationHelper helper) {
            if (mCachedStart != INVALID_LINE) {
                return mCachedStart;
            }
            calculateCachedStart(helper);
            return mCachedStart;
        }

        void calculateCachedEnd(OrientationHelper helper) {
            if (mViews.size() == 0) {
                mCachedEnd = INVALID_LINE;
            } else {
                final View endView = mViews.get(mViews.size() - 1);
                mCachedEnd = helper.getDecoratedEnd(endView);
            }
        }

        // Use this one when default value does not make sense and not having a value means a bug.
        int getEndLine(OrientationHelper helper) {
            if (mCachedEnd != INVALID_LINE) {
                return mCachedEnd;
            }
            calculateCachedEnd(helper);
            return mCachedEnd;
        }

        void prependToSpan(View view, OrientationHelper helper) {
            LayoutParams lp = getLayoutParams(view);
            mViews.add(0, view);
            mCachedStart = INVALID_LINE;
            if (mViews.size() == 1) {
                mCachedEnd = INVALID_LINE;
            }
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                mDeletedSize += helper.getDecoratedMeasurement(view);
            }
        }

        void appendToSpan(View view, OrientationHelper helper) {
            LayoutParams lp = getLayoutParams(view);
            mViews.add(view);
            mCachedEnd = INVALID_LINE;
            if (mViews.size() == 1) {
                mCachedStart = INVALID_LINE;
            }
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                mDeletedSize += helper.getDecoratedMeasurement(view);
            }
        }

        // Useful method to preserve positions on a re-layout.
        void cacheReferenceLineAndClear(boolean reverseLayout, int offset, OrientationHelper helper) {
            int reference;
            if (reverseLayout) {
                reference = getEndLine(helper);
            } else {
                reference = getStartLine(helper);
            }
            clear();
            if (reference == INVALID_LINE) {
                return;
            }
            if ((reverseLayout && reference < helper.getEndAfterPadding()) ||
                    (!reverseLayout && reference > helper.getStartAfterPadding())) {
                return;
            }
            if (offset != INVALID_OFFSET) {
                reference += offset;
            }
            mCachedStart = mCachedEnd = reference;
        }

        void clear() {
            mViews.clear();
            invalidateCache();
            mDeletedSize = 0;
        }

        void invalidateCache() {
            mCachedStart = INVALID_LINE;
            mCachedEnd = INVALID_LINE;
        }

        void setLine(int line) {
            mCachedEnd = mCachedStart = line;
        }

        void popEnd(OrientationHelper helper) {
            final int size = mViews.size();
            View end = mViews.remove(size - 1);
            final LayoutParams lp = getLayoutParams(end);
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                mDeletedSize -= helper.getDecoratedMeasurement(end);
            }
            if (size == 1) {
                mCachedStart = INVALID_LINE;
            }
            mCachedEnd = INVALID_LINE;
        }

        void popStart(OrientationHelper helper) {
            View start = mViews.remove(0);
            final LayoutParams lp = getLayoutParams(start);
            if (mViews.size() == 0) {
                mCachedEnd = INVALID_LINE;
            }
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                mDeletedSize -= helper.getDecoratedMeasurement(start);
            }
            mCachedStart = INVALID_LINE;
        }

        public int getDeletedSize() {
            return mDeletedSize;
        }

        LayoutParams getLayoutParams(View view) {
            return (LayoutParams) view.getLayoutParams();
        }

        void onOffset(int dt) {
            if (mCachedStart != INVALID_LINE) {
                mCachedStart += dt;
            }
            if (mCachedEnd != INVALID_LINE) {
                mCachedEnd += dt;
            }
        }

        // normalized offset is how much this span can scroll
        int getNormalizedOffset(int dt, int targetStart, int targetEnd, OrientationHelper helper) {
            if (mViews.size() == 0) {
                return 0;
            }
            if (dt < 0) {
                final int endSpace = getEndLine(helper) - targetEnd;
                if (endSpace <= 0) {
                    return 0;
                }
                return -dt > endSpace ? -endSpace : dt;
            } else {
                final int startSpace = targetStart - getStartLine(helper);
                if (startSpace <= 0) {
                    return 0;
                }
                return startSpace < dt ? startSpace : dt;
            }
        }

        /**
         * Returns if there is no child between start-end lines
         *
         * @param start The start line
         * @param end   The end line
         * @return true if a new child can be added between start and end
         */
        boolean isEmpty(int start, int end, OrientationHelper helper) {
            final int count = mViews.size();
            for (int i = 0; i < count; i++) {
                final View view = mViews.get(i);
                if (helper.getDecoratedStart(view) < end &&
                        helper.getDecoratedEnd(view) > start) {
                    return false;
                }
            }
            return true;
        }
    }

}
