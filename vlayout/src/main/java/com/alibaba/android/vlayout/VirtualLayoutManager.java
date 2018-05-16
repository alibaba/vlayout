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

package com.alibaba.android.vlayout;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.alibaba.android.vlayout.extend.PerformanceMonitor;
import com.alibaba.android.vlayout.layout.BaseLayoutHelper;
import com.alibaba.android.vlayout.layout.DefaultLayoutHelper;
import com.alibaba.android.vlayout.layout.FixAreaAdjuster;
import com.alibaba.android.vlayout.layout.FixAreaLayoutHelper;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Trace;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;


/**
 * A {@link android.support.v7.widget.RecyclerView.LayoutManager} implementation which provides
 * a virtual layout for actual views.
 * <p>
 * NOTE: it will change {@link android.support.v7.widget.RecyclerView.RecycledViewPool}
 * for RecyclerView.
 *
 * @author villadora
 * @since 1.0.0
 */

public class VirtualLayoutManager extends ExposeLinearLayoutManagerEx implements LayoutManagerHelper {
    protected static final String TAG = "VirtualLayoutManager";

    private static final String PHASE_MEASURE = "measure";
    private static final String PHASE_LAYOUT = "layout";
    private static final String TRACE_LAYOUT = "VLM onLayoutChildren";
    private static final String TRACE_SCROLL = "VLM scroll";

    private static boolean sDebuggable = false;

    public static void enableDebugging(boolean isDebug) {
        sDebuggable = isDebug;
    }

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    public static final int VERTICAL = OrientationHelper.VERTICAL;


    protected OrientationHelperEx mOrientationHelper;
    protected OrientationHelperEx mSecondaryOrientationHelper;

    private RecyclerView mRecyclerView;

    private boolean mNoScrolling = false;

    private boolean mNestedScrolling = false;

    private boolean mEnableMarginOverlapping = false;

    private int mMaxMeasureSize = -1;

    private PerformanceMonitor mPerformanceMonitor;

    public VirtualLayoutManager(@NonNull final Context context) {
        this(context, VERTICAL);
    }

    /**
     * @param context     Context
     * @param orientation Layout orientation. Should be {@link #HORIZONTAL} or {@link
     *                    #VERTICAL}.
     */
    public VirtualLayoutManager(@NonNull final Context context, int orientation) {
        this(context, orientation, false);
    }

    /**
     * @param context       Current context, will be used to access resources.
     * @param orientation   Layout orientation. Should be {@link #HORIZONTAL} or {@link
     *                      #VERTICAL}.
     * @param reverseLayout whether should reverse data
     */
    public VirtualLayoutManager(@NonNull final Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        this.mOrientationHelper = OrientationHelperEx.createOrientationHelper(this, orientation);
        this.mSecondaryOrientationHelper = OrientationHelperEx.createOrientationHelper(this, orientation == VERTICAL ? HORIZONTAL : VERTICAL);
        setHelperFinder(new RangeLayoutHelperFinder());
    }

    public void setPerformanceMonitor(PerformanceMonitor performanceMonitor) {
        mPerformanceMonitor = performanceMonitor;
    }

    public void setNoScrolling(boolean noScrolling) {
        this.mNoScrolling = noScrolling;
        mSpaceMeasured = false;
        mMeasuredFullSpace = 0;
        mSpaceMeasuring = false;
    }

    public void setNestedScrolling(boolean nestedScrolling) {
        setNestedScrolling(nestedScrolling, -1);
    }

    public void setNestedScrolling(boolean nestedScrolling, int maxMeasureSize) {
        this.mNestedScrolling = nestedScrolling;
        mSpaceMeasuring = mSpaceMeasured = false;
        mMeasuredFullSpace = 0;
    }

    private LayoutHelperFinder mHelperFinder;

    public void setHelperFinder(@NonNull final LayoutHelperFinder finder) {
        //noinspection ConstantConditions
        if (finder == null) {
            throw new IllegalArgumentException("finder is null");
        }

        List<LayoutHelper> helpers = new LinkedList<>();
        if (this.mHelperFinder != null) {
            List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
            for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
                LayoutHelper helper = layoutHelpers.get(i);
                helpers.add(helper);
            }
        }

        this.mHelperFinder = finder;
        if (helpers.size() > 0)
            this.mHelperFinder.setLayouts(helpers);

        mSpaceMeasured = false;
        requestLayout();
    }

    private FixAreaAdjuster mFixAreaAdjustor = FixAreaAdjuster.mDefaultAdjuster;

    public void setFixOffset(int left, int top, int right, int bottom) {
        mFixAreaAdjustor = new FixAreaAdjuster(left, top, right, bottom);
    }


    /*
     * Temp hashMap
     */
    private HashMap<Integer, LayoutHelper> newHelpersSet = new HashMap<>();
    private HashMap<Integer, LayoutHelper> oldHelpersSet = new HashMap<>();

    private BaseLayoutHelper.LayoutViewBindListener mLayoutViewBindListener;

    /**
     * Update layoutHelpers, data changes will cause layoutHelpers change
     *
     * @param helpers group of layoutHelpers
     */
    public void setLayoutHelpers(@Nullable List<LayoutHelper> helpers) {
        List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
            LayoutHelper helper = layoutHelpers.get(i);
            oldHelpersSet.put(System.identityHashCode(helper), helper);
        }

        // set ranges
        if (helpers != null) {
            int start = 0;
            for (int i = 0; i < helpers.size(); i++) {
                LayoutHelper helper = helpers.get(i);

                if (helper instanceof FixAreaLayoutHelper) {
                    ((FixAreaLayoutHelper) helper).setAdjuster(mFixAreaAdjustor);
                }

                if (helper instanceof BaseLayoutHelper && mLayoutViewBindListener != null) {
                    ((BaseLayoutHelper) helper).setLayoutViewBindListener(mLayoutViewBindListener);
                }


                if (helper.getItemCount() > 0) {
                    helper.setRange(start, start + helper.getItemCount() - 1);
                } else {
                    helper.setRange(-1, -1);
                }

                start += helper.getItemCount();
            }
        }

        this.mHelperFinder.setLayouts(helpers);

        layoutHelpers = mHelperFinder.getLayoutHelpers();
        for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
            LayoutHelper helper = layoutHelpers.get(i);
            newHelpersSet.put(System.identityHashCode(helper), helper);
        }


        for (Iterator<Map.Entry<Integer, LayoutHelper>> it = oldHelpersSet.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, LayoutHelper> entry = it.next();
            Integer key = entry.getKey();
            if (newHelpersSet.containsKey(key)) {
                newHelpersSet.remove(key);
                it.remove();
            }
        }


        for (LayoutHelper helper : oldHelpersSet.values()) {
            helper.clear(this);
        }

        if (!oldHelpersSet.isEmpty() || !newHelpersSet.isEmpty()) {
            mSpaceMeasured = false;
        }

        oldHelpersSet.clear();
        newHelpersSet.clear();
        requestLayout();
    }


    @NonNull
    public List<LayoutHelper> getLayoutHelpers() {
        return this.mHelperFinder.getLayoutHelpers();
    }

    public void setEnableMarginOverlapping(boolean enableMarginOverlapping) {
        mEnableMarginOverlapping = enableMarginOverlapping;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnableMarginOverLap() {
        return mEnableMarginOverlapping;
    }

    /**
     * Either be {@link #HORIZONTAL} or {@link #VERTICAL}
     *
     * @return orientation of this layout manager
     */
    @Override
    public int getOrientation() {
        return super.getOrientation();
    }

    @Override
    public void setOrientation(int orientation) {
        this.mOrientationHelper = OrientationHelperEx.createOrientationHelper(this, orientation);
        super.setOrientation(orientation);
    }

    /**
     * reverseLayout is not supported by VirtualLayoutManager. It's get disabled until all the LayoutHelpers support it.
     */
    @Override
    public void setReverseLayout(boolean reverseLayout) {
        if (reverseLayout) {
            throw new UnsupportedOperationException(
                    "VirtualLayoutManager does not support reverse layout in current version.");
        }

        super.setReverseLayout(false);
    }

    /**
     * stackFromEnd is not supported by VirtualLayoutManager. It's get disabled util all the layoutHelpers support it.
     * {@link #setReverseLayout(boolean)}.
     */
    @Override
    public void setStackFromEnd(boolean stackFromEnd) {
        if (stackFromEnd) {
            throw new UnsupportedOperationException(
                    "VirtualLayoutManager does not support stack from end.");
        }
        super.setStackFromEnd(false);
    }


    private AnchorInfoWrapper mTempAnchorInfoWrapper = new AnchorInfoWrapper();

    @Override
    public void onAnchorReady(RecyclerView.State state, ExposeLinearLayoutManagerEx.AnchorInfo anchorInfo) {
        super.onAnchorReady(state, anchorInfo);

        boolean changed = true;
        while (changed) {
            mTempAnchorInfoWrapper.position = anchorInfo.mPosition;
            mTempAnchorInfoWrapper.coordinate = anchorInfo.mCoordinate;
            mTempAnchorInfoWrapper.layoutFromEnd = anchorInfo.mLayoutFromEnd;
            LayoutHelper layoutHelper = mHelperFinder.getLayoutHelper(anchorInfo.mPosition);
            if (layoutHelper != null)
                layoutHelper.checkAnchorInfo(state, mTempAnchorInfoWrapper, this);

            if (mTempAnchorInfoWrapper.position == anchorInfo.mPosition) {
                changed = false;
            } else {
                anchorInfo.mPosition = mTempAnchorInfoWrapper.position;
            }

            anchorInfo.mCoordinate = mTempAnchorInfoWrapper.coordinate;

            mTempAnchorInfoWrapper.position = -1;
        }


        mTempAnchorInfoWrapper.position = anchorInfo.mPosition;
        mTempAnchorInfoWrapper.coordinate = anchorInfo.mCoordinate;
        List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
            LayoutHelper layoutHelper = layoutHelpers.get(i);
            layoutHelper.onRefreshLayout(state, mTempAnchorInfoWrapper, this);
        }
    }

    public LayoutHelper findNeighbourNonfixLayoutHelper(LayoutHelper layoutHelper, boolean isLayoutEnd) {
        if (layoutHelper == null) {
            return null;
        }
        List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        int index = layoutHelpers.indexOf(layoutHelper);
        if (index == -1) {
            return null;
        }
        int next = isLayoutEnd ? index - 1 : index + 1;
        if (next >= 0 && next < layoutHelpers.size()) {
            LayoutHelper helper = layoutHelpers.get(next);
            if (helper != null) {
                if (helper.isFixLayout()) {
                    return null;
                } else {
                    return helper;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected int computeAlignOffset(View child, boolean isLayoutEnd, boolean useAnchor) {
        return computeAlignOffset(getPosition(child), isLayoutEnd, useAnchor);
    }

    @Override
    protected int computeAlignOffset(int position, boolean isLayoutEnd, boolean useAnchor) {
        if (position != RecyclerView.NO_POSITION) {
            LayoutHelper helper = mHelperFinder.getLayoutHelper(position);

            if (helper != null) {
                return helper.computeAlignOffset(position - helper.getRange().getLower(),
                        isLayoutEnd, useAnchor, this);
            }
        }

        return 0;
    }

    public int obtainExtraMargin(View child, boolean isLayoutEnd) {
        return obtainExtraMargin(child, isLayoutEnd, true);
    }

    public int obtainExtraMargin(View child, boolean isLayoutEnd, boolean useAnchor) {
        if (child != null) {
            return computeAlignOffset(child, isLayoutEnd, useAnchor);
        }

        return 0;
    }

    private int mNested = 0;


    private void runPreLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {

        if (mNested == 0) {
            List<LayoutHelper> reverseLayoutHelpers = mHelperFinder.reverse();
            for (int i = 0, size = reverseLayoutHelpers.size(); i < size; i++) {
                LayoutHelper layoutHelper = reverseLayoutHelpers.get(i);
                layoutHelper.beforeLayout(recycler, state, this);
            }
        }

        mNested++;
    }

    private void runPostLayout(RecyclerView.Recycler recycler, RecyclerView.State state, int scrolled) {
        mNested--;
        if (mNested <= 0) {
            mNested = 0;
            final int startPosition = findFirstVisibleItemPosition();
            final int endPosition = findLastVisibleItemPosition();
            List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
            for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
                LayoutHelper layoutHelper = layoutHelpers.get(i);
                try {
                    layoutHelper.afterLayout(recycler, state, startPosition, endPosition, scrolled, this);
                } catch (Exception e) {
                    if (VirtualLayoutManager.sDebuggable) {
                        throw e;
                    }
                }
            }
        }
    }

    public void runAdjustLayout() {
        final int startPosition = findFirstVisibleItemPosition();
        final LayoutHelper firstLayoutHelper = mHelperFinder.getLayoutHelper(startPosition);
        final int endPosition = findLastVisibleItemPosition();
        final LayoutHelper lastLayoutHelper = mHelperFinder.getLayoutHelper(endPosition);
        List<LayoutHelper> totalLayoutHelpers = mHelperFinder.getLayoutHelpers();
        final int start = totalLayoutHelpers.indexOf(firstLayoutHelper);
        final int end = totalLayoutHelpers.indexOf(lastLayoutHelper);
        for (int i = start; i <= end; i++) {
            try {
                totalLayoutHelpers.get(i).adjustLayout(startPosition, endPosition, this);
            } catch (Exception e) {
                if (VirtualLayoutManager.sDebuggable) {
                    throw e;
                }
            }
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.beginSection(TRACE_LAYOUT);
        }

        if (mNoScrolling && state.didStructureChange()) {
            mSpaceMeasured = false;
            mSpaceMeasuring = true;
        }


        runPreLayout(recycler, state);

        try {
            super.onLayoutChildren(recycler, state);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            // MaX_VALUE means invalidate scrolling offset - no scroll
            runPostLayout(recycler, state, Integer.MAX_VALUE); // hack to indicate its an initial layout
        }


        if ((mNestedScrolling || mNoScrolling) && mSpaceMeasuring) {
            // measure required, so do measure
            mSpaceMeasured = true;
            // get last child
            int childCount = getChildCount();
            View lastChild = getChildAt(childCount - 1);
            if (lastChild != null) {
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) lastChild.getLayoutParams();
                // found the end of last child view
                mMeasuredFullSpace = getDecoratedBottom(lastChild) + params.bottomMargin + computeAlignOffset(lastChild, true, false);

                if (mRecyclerView != null && mNestedScrolling) {
                    ViewParent parent = mRecyclerView.getParent();
                    if (parent instanceof View) {
                        // make sure the fullspace be the min value of measured space and parent's height
                        mMeasuredFullSpace = Math.min(mMeasuredFullSpace, ((View) parent).getMeasuredHeight());
                    }
                }
            } else {
                mSpaceMeasuring = false;
            }
            mSpaceMeasuring = false;
            if (mRecyclerView != null && getItemCount() > 0) {
                // relayout
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        // post relayout
                        if (mRecyclerView != null)
                            mRecyclerView.requestLayout();
                    }
                });
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.endSection();
        }
    }

    /**
     * Entry method for scrolling
     * {@inheritDoc}
     */
    @Override
    protected int scrollInternalBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.beginSection(TRACE_SCROLL);
        }

        runPreLayout(recycler, state);

        int scrolled = 0;
        try {
            if (!mNoScrolling) {
                scrolled = super.scrollInternalBy(dy, recycler, state);
            } else {
                if (getChildCount() == 0 || dy == 0) {
                    return 0;
                }

                mLayoutState.mRecycle = true;
                ensureLayoutStateExpose();
                final int layoutDirection = dy > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
                final int absDy = Math.abs(dy);
                updateLayoutStateExpose(layoutDirection, absDy, true, state);
                final int freeScroll = mLayoutState.mScrollingOffset;

                final int consumed = freeScroll + fill(recycler, mLayoutState, state, false);
                if (consumed < 0) {
                    return 0;
                }
                scrolled = absDy > consumed ? layoutDirection * consumed : dy;
            }
        } catch (Exception e) {
            Log.w(TAG, Log.getStackTraceString(e), e);
            if (sDebuggable)
                throw e;

        } finally {
            runPostLayout(recycler, state, scrolled);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.endSection();
        }

        return scrolled;
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        int startPosition = findFirstVisibleItemPosition();
        int endPosition = findLastVisibleItemPosition();
        List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
            LayoutHelper layoutHelper = layoutHelpers.get(i);
            layoutHelper.onScrollStateChanged(state, startPosition, endPosition, this);
        }
    }

    @Override
    public void offsetChildrenHorizontal(int dx) {
        super.offsetChildrenHorizontal(dx);

        List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
            LayoutHelper layoutHelper = layoutHelpers.get(i);
            layoutHelper.onOffsetChildrenHorizontal(dx, this);
        }
    }

    @Override
    public void offsetChildrenVertical(int dy) {
        super.offsetChildrenVertical(dy);
        List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
            LayoutHelper layoutHelper = layoutHelpers.get(i);
            layoutHelper.onOffsetChildrenVertical(dy, this);
        }
    }

    private LayoutStateWrapper mTempLayoutStateWrapper = new LayoutStateWrapper();

    private List<Pair<Range<Integer>, Integer>> mRangeLengths = new LinkedList<>();

    @Nullable
    private int findRangeLength(@NonNull final Range<Integer> range) {
        final int count = mRangeLengths.size();
        if (count == 0) {
            return -1;
        }

        int s = 0, e = count - 1, m = -1;
        Pair<Range<Integer>, Integer> rs = null;

        // binary search range
        while (s <= e) {
            m = (s + e) / 2;
            rs = mRangeLengths.get(m);

            Range<Integer> r = rs.first;
            if (r == null) {
                rs = null;
                break;
            }

            if (r.contains(range.getLower()) || r.contains(range.getUpper()) || range.contains(r)) {
                break;
            } else if (r.getLower() > range.getUpper()) {
                e = m - 1;
            } else if (r.getUpper() < range.getLower()) {
                s = m + 1;
            }

            rs = null;
        }

        return rs == null ? -1 : m;
    }


    @Override
    protected void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutState layoutState, com.alibaba.android.vlayout.layout.LayoutChunkResult result) {
        final int position = layoutState.mCurrentPosition;
        mTempLayoutStateWrapper.mLayoutState = layoutState;
        LayoutHelper layoutHelper = mHelperFinder == null ? null : mHelperFinder.getLayoutHelper(position);
        if (layoutHelper == null)
            layoutHelper = mDefaultLayoutHelper;

        layoutHelper.doLayout(recycler, state, mTempLayoutStateWrapper, result, this);


        mTempLayoutStateWrapper.mLayoutState = null;


        // no item consumed
        if (layoutState.mCurrentPosition == position) {
            Log.w(TAG, "layoutHelper[" + layoutHelper.getClass().getSimpleName() + "@" + layoutHelper.toString() + "] consumes no item!");
            // break as no item consumed
            result.mFinished = true;
        } else {
            // Update height consumed in each layoutChunck pass
            final int positionAfterLayout = layoutState.mCurrentPosition - layoutState.mItemDirection;
            final int consumed = result.mIgnoreConsumed ? 0 : result.mConsumed;

            // TODO: change when supporting reverseLayout
            Range<Integer> range = new Range<>(Math.min(position, positionAfterLayout), Math.max(position, positionAfterLayout));

            final int idx = findRangeLength(range);
            if (idx >= 0) {
                Pair<Range<Integer>, Integer> pair = mRangeLengths.get(idx);
                if (pair != null && pair.first.equals(range) && pair.second == consumed)
                    return;

                mRangeLengths.remove(idx);
            }

            mRangeLengths.add(Pair.create(range, consumed));
            Collections.sort(mRangeLengths, new Comparator<Pair<Range<Integer>, Integer>>() {
                @Override
                public int compare(Pair<Range<Integer>, Integer> a, Pair<Range<Integer>, Integer> b) {
                    if (a == null && b == null) return 0;
                    if (a == null) return -1;
                    if (b == null) return 1;

                    Range<Integer> lr = a.first;
                    Range<Integer> rr = b.first;

                    return lr.getLower() - rr.getLower();
                }
            });
        }
    }


    /**
     * Return current position related to the top, only works when scrolling from the top
     *
     * @return offset from current position to original top of RecycledView
     */
    public int getOffsetToStart() {
        if (getChildCount() == 0) return -1;

        final View view = getChildAt(0);

        if (view == null) {
            //in some conditions, for exapmle, calling this method when outter activity destroy, may cause npe
            return -1;
        }

        int position = getPosition(view);
        final int idx = findRangeLength(Range.create(position, position));
        if (idx < 0 || idx >= mRangeLengths.size()) {
            return -1;
        }

        int offset = -mOrientationHelper.getDecoratedStart(view);
        for (int i = 0; i < idx; i++) {
            Pair<Range<Integer>, Integer> pair = mRangeLengths.get(i);
            if (pair != null) {
                offset += pair.second;
            }
        }

        return offset;
    }


    private static LayoutHelper DEFAULT_LAYOUT_HELPER = new DefaultLayoutHelper();

    private LayoutHelper mDefaultLayoutHelper = DEFAULT_LAYOUT_HELPER;

    /**
     * Change default LayoutHelper
     *
     * @param layoutHelper default layoutHelper apply to items without specified layoutHelper, it should not be null
     */
    private void setDefaultLayoutHelper(@NonNull final LayoutHelper layoutHelper) {
        //noinspection ConstantConditions
        if (layoutHelper == null)
            throw new IllegalArgumentException("layoutHelper should not be null");

        this.mDefaultLayoutHelper = layoutHelper;
    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position);
    }


    @Override
    public void scrollToPositionWithOffset(int position, int offset) {
        super.scrollToPositionWithOffset(position, offset);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        super.smoothScrollToPosition(recyclerView, state, position);
    }


    @Override
    public boolean supportsPredictiveItemAnimations() {
        return mCurrentPendingSavedState == null;
    }


    /**
     * Do updates when items change
     *
     * @param recyclerView  recyclerView that belong to
     * @param positionStart start position that items changed
     * @param itemCount     number of items that changed
     */
    @Override
    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
            LayoutHelper layoutHelper = layoutHelpers.get(i);
            layoutHelper.onItemsChanged(this);
        }

        // setLayoutHelpers(mHelperFinder.getLayoutHelpers());
    }


    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else if (lp instanceof RecyclerView.LayoutParams) {
            return new LayoutParams((RecyclerView.LayoutParams) lp);
        } else if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new InflateLayoutParams(c, attrs);
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);
    }


    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mRecyclerView = view;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);

        List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
            LayoutHelper layoutHelper = layoutHelpers.get(i);
            layoutHelper.clear(this);
        }

        mRecyclerView = null;
    }


    @SuppressWarnings("unused")
    public static class LayoutParams extends RecyclerView.LayoutParams {
        public static final int INVALIDE_SIZE = Integer.MIN_VALUE;


        public int zIndex = 0;

        public float mAspectRatio = Float.NaN;

        private int mOriginWidth = INVALIDE_SIZE;
        private int mOriginHeight = INVALIDE_SIZE;


        public void storeOriginWidth() {
            if (mOriginWidth == INVALIDE_SIZE) {
                mOriginWidth = width;
            }
        }

        public void storeOriginHeight() {
            if (mOriginHeight == INVALIDE_SIZE) {
                mOriginHeight = height;
            }
        }

        public void restoreOriginWidth() {
            if (mOriginWidth != INVALIDE_SIZE) {
                width = mOriginWidth;
            }
        }

        public void restoreOriginHeight() {
            if (mOriginHeight != INVALIDE_SIZE) {
                height = mOriginHeight;
            }
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }

    }

    public static class InflateLayoutParams extends LayoutParams {

        public InflateLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }
    }


    public static class AnchorInfoWrapper {

        public int position;

        public int coordinate;

        public boolean layoutFromEnd;

        AnchorInfoWrapper() {

        }

    }


    @SuppressWarnings({"JavaDoc", "unused"})
    public static class LayoutStateWrapper {
        public final static int LAYOUT_START = -1;

        public final static int LAYOUT_END = 1;

        final static int INVALID_LAYOUT = Integer.MIN_VALUE;

        public final static int ITEM_DIRECTION_HEAD = -1;

        public final static int ITEM_DIRECTION_TAIL = 1;

        final static int SCOLLING_OFFSET_NaN = Integer.MIN_VALUE;

        private LayoutState mLayoutState;

        LayoutStateWrapper() {

        }

        LayoutStateWrapper(LayoutState layoutState) {
            this.mLayoutState = layoutState;
        }


        public int getOffset() {
            return mLayoutState.mOffset;
        }

        public int getCurrentPosition() {
            return mLayoutState.mCurrentPosition;
        }

        public boolean hasScrapList() {
            return mLayoutState.mScrapList != null;
        }

        public void skipCurrentPosition() {
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
        }

        /**
         * We may not want to recycle children in some cases (e.g. layout)
         */
        public boolean isRecycle() {
            return mLayoutState.mRecycle;
        }


        /**
         * This {@link #layoutChunk(RecyclerView.Recycler, RecyclerView.State, LayoutState, com.alibaba.android.vlayout.layout.LayoutChunkResult)} pass is in layouting or scrolling
         */
        public boolean isRefreshLayout() {
            return mLayoutState.mOnRefresLayout;
        }

        /**
         * Number of pixels that we should fill, in the layout direction.
         */
        public int getAvailable() {
            return mLayoutState.mAvailable;
        }


        /**
         * Defines the direction in which the data adapter is traversed.
         * Should be {@link #ITEM_DIRECTION_HEAD} or {@link #ITEM_DIRECTION_TAIL}
         */
        public int getItemDirection() {
            return mLayoutState.mItemDirection;
        }

        /**
         * Defines the direction in which the layout is filled.
         * Should be {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        public int getLayoutDirection() {
            return mLayoutState.mLayoutDirection;
        }

        /**
         * Used when LayoutState is constructed in a scrolling state.
         * It should be set the amount of scrolling we can make without creating a new view.
         * Settings this is required for efficient view recycling.
         */
        public int getScrollingOffset() {
            return mLayoutState.mScrollingOffset;
        }

        /**
         * Used if you want to pre-layout items that are not yet visible.
         * The difference with {@link #getAvailable()} is that, when recycling, distance laid out for
         * {@link #getExtra()} is not considered to avoid recycling visible children.
         */
        public int getExtra() {
            return mLayoutState.mExtra;
        }

        /**
         * Equal to {@link RecyclerView.State#isPreLayout()}. When consuming scrap, if this value
         * is set to true, we skip removed views since they should not be laid out in post layout
         * step.
         */
        public boolean isPreLayout() {
            return mLayoutState.mIsPreLayout;
        }


        public boolean hasMore(RecyclerView.State state) {
            return mLayoutState.hasMore(state);
        }

        public View next(RecyclerView.Recycler recycler) {
            View next = mLayoutState.next(recycler);
            // set recycler
            return next;
        }

        public View retrieve(RecyclerView.Recycler recycler, int position) {
            int originPosition = mLayoutState.mCurrentPosition;
            mLayoutState.mCurrentPosition = position;
            View view = next(recycler);
            mLayoutState.mCurrentPosition = originPosition;
            return view;
        }
    }


    private static class LayoutViewHolder extends RecyclerView.ViewHolder {

        public LayoutViewHolder(View itemView) {
            super(itemView);
        }

    }


    public List<View> getFixedViews() {
        if (mRecyclerView == null) return Collections.emptyList();

        // TODO: support zIndex?
        List<View> views = new LinkedList<>();
        List<LayoutHelper> layoutHelpers = mHelperFinder.getLayoutHelpers();
        for (int i = 0, size = layoutHelpers.size(); i < size; i++) {
            LayoutHelper layoutHelper = layoutHelpers.get(i);
            View fixedView = layoutHelper.getFixedView();
            if (fixedView != null) {
                views.add(fixedView);
            }
        }

        return views;
    }


    private LayoutViewFactory mLayoutViewFatory = new LayoutViewFactory() {
        @Override
        public View generateLayoutView(@NonNull Context context) {
            return new LayoutView(context);
        }
    };

    /**
     * Set LayoutView Factory, so you can replace LayoutView for LayoutHelpers
     *
     * @param factory
     */
    public void setLayoutViewFactory(@NonNull final LayoutViewFactory factory) {
        if (factory == null)
            throw new IllegalArgumentException("factory should not be null");
        mLayoutViewFatory = factory;
    }

    @Override
    public final View generateLayoutView() {
        if (mRecyclerView == null) return null;

        View layoutView = mLayoutViewFatory.generateLayoutView(mRecyclerView.getContext());
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        attachViewHolder(params, new LayoutViewHolder(layoutView));

        layoutView.setLayoutParams(params);
        return layoutView;
    }


    @Override
    public void addChildView(View view, int index) {
        super.addView(view, index);
    }


    @Override
    public void moveView(int fromIndex, int toIndex) {
        super.moveView(fromIndex, toIndex);
    }

    @Override
    public void addChildView(LayoutStateWrapper layoutState, View view) {
        addChildView(layoutState, view, layoutState.getItemDirection() == LayoutStateWrapper.ITEM_DIRECTION_TAIL ? -1 : 0);
    }


    @Override
    public void addChildView(LayoutStateWrapper layoutState, View view, int index) {
        showView(view);

        if (!layoutState.hasScrapList()) {
            // can not find in scrapList
            addView(view, index);
        } else {
            addDisappearingView(view, index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOffFlowView(View view, boolean head) {
        showView(view);
        addHiddenView(view, head);

    }

    @Override
    public void addBackgroundView(View view, boolean head) {
        showView(view);
        int index = head ? 0 : -1;
        addView(view, index);
    }

    @Override
    public void addFixedView(View view) {
        //removeChildView(view);
        //mFixedContainer.addView(view);
        addOffFlowView(view, false);
    }

    @Override
    public void hideView(View view) {
        super.hideView(view);
    }

    @Override
    public void showView(View view) {
        super.showView(view);
    }

    @Override
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public RecyclerView.ViewHolder getChildViewHolder(View view) {
        if (mRecyclerView != null)
            return mRecyclerView.getChildViewHolder(view);
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isViewHolderUpdated(View view) {
        RecyclerView.ViewHolder holder = getChildViewHolder(view);
        return holder == null || isViewHolderUpdated(holder);

    }

    @Override
    public void removeChildView(View child) {
        removeView(child);
    }

    @Override
    public OrientationHelperEx getMainOrientationHelper() {
        return mOrientationHelper;
    }

    @Override
    public OrientationHelperEx getSecondaryOrientationHelper() {
        return mSecondaryOrientationHelper;
    }

    @Override
    public void measureChild(View child, int widthSpec, int heightSpec) {
        measureChildWithDecorations(child, widthSpec, heightSpec);
    }

    @Override
    public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
        measureChildWithDecorationsAndMargin(child, widthUsed, heightUsed);
    }

    @Override
    public int getChildMeasureSpec(int parentSize, int size, boolean canScroll) {
        return getChildMeasureSpec(parentSize, 0, size, canScroll);
    }


    @Override
    public boolean canScrollHorizontally() {
        return super.canScrollHorizontally() && !mNoScrolling;
    }

    @Override
    public boolean canScrollVertically() {
        return super.canScrollVertically() && !mNoScrolling;
    }

    @Override
    public void layoutChildWithMargins(View child, int left, int top, int right, int bottom) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordStart(PHASE_LAYOUT, child);
        }
        layoutDecorated(child, left + lp.leftMargin, top + lp.topMargin,
                right - lp.rightMargin, bottom - lp.bottomMargin);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordEnd(PHASE_LAYOUT, child);
        }
    }

    @Override
    public void layoutChild(View child, int left, int top, int right, int bottom) {
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordStart(PHASE_LAYOUT, child);
        }
        layoutDecorated(child, left, top,
                right, bottom);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordEnd(PHASE_LAYOUT, child);
        }
    }

    @Override
    protected void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            return;
        }

        if (sDebuggable) {
            Log.d(TAG, "Recycling " + Math.abs(startIndex - endIndex) + " items");
        }

        if (endIndex > startIndex) {

            View endView = getChildAt(endIndex - 1);
            View startView = getChildAt(startIndex);

            int startPos = getPosition(startView);
            int endPos = getPosition(endView);

            int idx = startIndex;

            for (int i = startIndex; i < endIndex; i++) {
                View v = getChildAt(idx);
                int pos = getPosition(v);
                if (pos != RecyclerView.NO_POSITION) {
                    LayoutHelper layoutHelper = mHelperFinder.getLayoutHelper(pos);
                    if (layoutHelper == null || layoutHelper.isRecyclable(pos, startPos, endPos, this, true)) {
                        removeAndRecycleViewAt(idx, recycler);
                    } else {
                        idx++;
                    }
                } else
                    removeAndRecycleViewAt(idx, recycler);
            }
        } else {

            View endView = getChildAt(startIndex);
            View startView = getChildAt(endIndex + 1);

            int startPos = getPosition(startView);
            int endPos = getPosition(endView);

            for (int i = startIndex; i > endIndex; i--) {
                View v = getChildAt(i);
                int pos = getPosition(v);
                if (pos != RecyclerView.NO_POSITION) {
                    LayoutHelper layoutHelper = mHelperFinder.getLayoutHelper(pos);
                    if (layoutHelper == null || layoutHelper.isRecyclable(pos, startPos, endPos, this, false)) {
                        removeAndRecycleViewAt(i, recycler);
                    }
                } else
                    removeAndRecycleViewAt(i, recycler);
            }
        }
    }


    @Override
    public void detachAndScrapAttachedViews(RecyclerView.Recycler recycler) {
        int childCount = this.getChildCount();

        for (int i = childCount - 1; i >= 0; --i) {
            View v = this.getChildAt(i);
            RecyclerView.ViewHolder holder = getChildViewHolder(v);
            if (holder instanceof CacheViewHolder && ((CacheViewHolder) holder).needCached()) {
                // mark not invalid, ignore DataSetChange(), make the ViewHolder itself to maitain the data
                ViewHolderWrapper.setFlags(holder, 0, FLAG_INVALID | FLAG_UPDATED);
            }
        }


        super.detachAndScrapAttachedViews(recycler);
    }

    @Override
    public void detachAndScrapViewAt(int index, RecyclerView.Recycler recycler) {
        View child = getChildAt(index);
        RecyclerView.ViewHolder holder = getChildViewHolder(child);
        if (holder instanceof CacheViewHolder && ((CacheViewHolder) holder).needCached()) {
            // mark not invalid
            ViewHolderWrapper.setFlags(holder, 0, FLAG_INVALID);
        }

        super.detachAndScrapViewAt(index, recycler);
    }

    @Override
    public void detachAndScrapView(View child, RecyclerView.Recycler recycler) {
        super.detachAndScrapView(child, recycler);
    }

    public interface CacheViewHolder {
        boolean needCached();
    }

    @Override
    public int getContentWidth() {
        return super.getWidth();
    }

    @Override
    public int getContentHeight() {
        return super.getHeight();
    }

    @Override
    public boolean isDoLayoutRTL() {
        return isLayoutRTL();
    }

    private Rect mDecorInsets = new Rect();

    private void measureChildWithDecorations(View child, int widthSpec, int heightSpec) {
        calculateItemDecorationsForChild(child, mDecorInsets);
        widthSpec = updateSpecWithExtra(widthSpec, mDecorInsets.left, mDecorInsets.right);
        heightSpec = updateSpecWithExtra(heightSpec, mDecorInsets.top, mDecorInsets.bottom);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordStart(PHASE_MEASURE, child);
        }
        child.measure(widthSpec, heightSpec);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordEnd(PHASE_MEASURE, child);
        }
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec) {
        calculateItemDecorationsForChild(child, mDecorInsets);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();

        if (getOrientation() == VERTICAL) {
            widthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + mDecorInsets.left,
                lp.rightMargin + mDecorInsets.right);
        }
        if (getOrientation() == HORIZONTAL) {
            heightSpec = updateSpecWithExtra(heightSpec, mDecorInsets.top,
                    mDecorInsets.bottom);
        }
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordStart(PHASE_MEASURE, child);
        }
        child.measure(widthSpec, heightSpec);
        if (mPerformanceMonitor != null) {
            mPerformanceMonitor.recordEnd(PHASE_MEASURE, child);
        }
    }

    /**
     * Update measure spec with insets
     *
     * @param spec
     * @param startInset
     * @param endInset
     * @return
     */
    private int updateSpecWithExtra(int spec, int startInset, int endInset) {
        if (startInset == 0 && endInset == 0) {
            return spec;
        }
        final int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            int size = View.MeasureSpec.getSize(spec);
            if (size - startInset - endInset < 0) {
                return View.MeasureSpec.makeMeasureSpec(0, mode);
            } else {
                return View.MeasureSpec.makeMeasureSpec(
                        View.MeasureSpec.getSize(spec) - startInset - endInset, mode);
            }
        }
        return spec;
    }


    @Override
    public View findViewByPosition(int position) {
        View view = super.findViewByPosition(position);
        if (view != null && getPosition(view) == position)
            return view;

        for (int i = 0; i < getChildCount(); i++) {
            view = getChildAt(i);
            if (view != null && getPosition(view) == position) {
                return view;
            }
        }

        return null;
    }


    @Override
    public void recycleView(View view) {
        if (mRecyclerView != null) {
            ViewParent parent = view.getParent();
            if (parent != null && parent == mRecyclerView) {
                RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
                mRecyclerView.getRecycledViewPool().putRecycledView(holder);
            }
        }
    }

    @Override
    public LayoutHelper findLayoutHelperByPosition(int position) {
        return mHelperFinder.getLayoutHelper(position);
    }




    /*
     * extend to full show view
     */


    // when set no scrolling, the max size should have limit
    private static final int MAX_NO_SCROLLING_SIZE = Integer.MAX_VALUE >> 4;

    private boolean mSpaceMeasured = false;

    private int mMeasuredFullSpace = 0;

    private boolean mSpaceMeasuring = false;


    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {
        if (!mNoScrolling && !mNestedScrolling) {

            super.onMeasure(recycler, state, widthSpec, heightSpec);
            return;
        }


        int initialSize = MAX_NO_SCROLLING_SIZE;

        if (mRecyclerView != null && mNestedScrolling) {
            if (mMaxMeasureSize > 0) {
                initialSize = mMaxMeasureSize;
            } else {
                ViewParent parent = mRecyclerView.getParent();
                if (parent instanceof View) {
                    initialSize = ((View) parent).getMeasuredHeight();
                }
            }
        }

        int measuredSize = mSpaceMeasured ? mMeasuredFullSpace : initialSize;

        if (mNoScrolling) {
            mSpaceMeasuring = !mSpaceMeasured;

            if (getChildCount() > 0 || getChildCount() != getItemCount()) {
                View lastChild = getChildAt(getChildCount() - 1);

                int bottom = mMeasuredFullSpace;
                if (lastChild != null) {
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) lastChild.getLayoutParams();
                    bottom = getDecoratedBottom(lastChild) + params.bottomMargin + computeAlignOffset(lastChild, true, false);
                }

                if (getChildCount() != getItemCount() || (lastChild != null && bottom != mMeasuredFullSpace)) {
                    measuredSize = MAX_NO_SCROLLING_SIZE;
                    mSpaceMeasured = false;
                    mSpaceMeasuring = true;
                }
            } else if (getItemCount() == 0) {
                measuredSize = 0;
                mSpaceMeasured = true;
                mSpaceMeasuring = false;
            }
        }


        if (getOrientation() == VERTICAL) {
            super.onMeasure(recycler, state, widthSpec, View.MeasureSpec.makeMeasureSpec(measuredSize, View.MeasureSpec.AT_MOST));
        } else {
            super.onMeasure(recycler, state, View.MeasureSpec.makeMeasureSpec(measuredSize, View.MeasureSpec.AT_MOST), heightSpec);
        }
    }
}
