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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutParams;

/**
 * This class is used to expose layoutChunk method, should not be used in anywhere else
 * It's only a valid class technically and with no features/functions in it
 *
 * @author villadora
 * @since 1.0.0
 */


/**
 * A {@link RecyclerView.LayoutManager} implementation which provides
 * similar functionality to {@link android.widget.ListView}.
 */
class ExposeLinearLayoutManagerEx extends LinearLayoutManager {

    private static final String TAG = "ExposeLLManagerEx";

    private static final boolean DEBUG = false;

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    public static final int VERTICAL = OrientationHelper.VERTICAL;

    public static final int INVALID_OFFSET = Integer.MIN_VALUE;


    /**
     * While trying to find next view to focus, LayoutManager will not try to scroll more
     * than this factor times the total space of the list. If layout is vertical, total space is the
     * height minus padding, if layout is horizontal, total space is the width minus padding.
     */
    private static final float MAX_SCROLL_FACTOR = 0.33f;

    /**
     * Helper class that keeps temporary layout state.
     * It does not keep state after layout is complete but we still keep a reference to re-use
     * the same object.
     */
    protected LayoutState mLayoutState;

    /**
     * Many calculations are made depending on orientation. To keep it clean, this interface
     * helps {@link LinearLayoutManager} make those decisions.
     * Based on {@link #mOrientation}, an implementation is lazily created in
     * {@link #ensureLayoutStateExpose} method.
     */
    private OrientationHelperEx mOrientationHelper;

    /**
     * We need to track this so that we can ignore current position when it changes.
     */
    private boolean mLastStackFromEnd;


    /**
     * This keeps the final value for how LayoutManager should start laying out views.
     * It is calculated by checking {@link #getReverseLayout()} and View's layout direction.
     * {@link #onLayoutChildren(RecyclerView.Recycler, RecyclerView.State)} is run.
     */
    private boolean mShouldReverseLayoutExpose = false;

    /**
     * When LayoutManager needs to scroll to a position, it sets this variable and requests a
     * layout which will check this variable and re-layout accordingly.
     */
    private int mCurrentPendingScrollPosition = RecyclerView.NO_POSITION;

    /**
     * Used to keep the offset value when {@link #scrollToPositionWithOffset(int, int)} is
     * called.
     */
    private int mPendingScrollPositionOffset = INVALID_OFFSET;


    protected Bundle mCurrentPendingSavedState = null;

    /**
     * Re-used variable to keep anchor information on re-layout.
     * Anchor position and coordinate defines the reference point for LLM while doing a layout.
     */
    private final AnchorInfo mAnchorInfo;

    private final ChildHelperWrapper mChildHelperWrapper;

    private final Method mEnsureLayoutStateMethod;

    private int recycleOffset;

    /**
     * Creates a vertical LinearLayoutManager
     *
     * @param context Current context, will be used to access resources.
     */
    public ExposeLinearLayoutManagerEx(Context context) {
        this(context, VERTICAL, false);
    }


    /**
     * @param context       Current context, will be used to access resources.
     * @param orientation   Layout orientation. Should be {@link #HORIZONTAL} or {@link
     *                      #VERTICAL}.
     * @param reverseLayout When set to true, layouts from end to start.
     */
    public ExposeLinearLayoutManagerEx(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        mAnchorInfo = new AnchorInfo();
        setOrientation(orientation);
        setReverseLayout(reverseLayout);
        mChildHelperWrapper = new ChildHelperWrapper(this);


        try {
            mEnsureLayoutStateMethod = LinearLayoutManager.class.getDeclaredMethod("ensureLayoutState");
            mEnsureLayoutStateMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try {
            // FIXME in the future
            Method setItemPrefetchEnabledMethod = RecyclerView.LayoutManager.class
                    .getDeclaredMethod("setItemPrefetchEnabled", boolean.class);
            if (setItemPrefetchEnabledMethod != null) {
                setItemPrefetchEnabledMethod.invoke(this, false);
            }
        } catch (NoSuchMethodException e) {
            /* this method is added in 25.1.0, official release still has bug, see
             * https://code.google.com/p/android/issues/detail?can=2&start=0&num=100&q=&colspec=ID%20Status%20Priority%20Owner%20Summary%20Stars%20Reporter%20Opened&groupby=&sort=&id=230295
             **/
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }
//        setItemPrefetchEnabled(false);
    }


    @Override
    public Parcelable onSaveInstanceState() {
        if (mCurrentPendingSavedState != null) {
            return new Bundle(mCurrentPendingSavedState);
        }
        Bundle state = new Bundle();
        if (getChildCount() > 0) {
            boolean didLayoutFromEnd = mLastStackFromEnd ^ mShouldReverseLayoutExpose;
            state.putBoolean("AnchorLayoutFromEnd", didLayoutFromEnd);
            if (didLayoutFromEnd) {
                final View refChild = getChildClosestToEndExpose();
                state.putInt("AnchorOffset", mOrientationHelper.getEndAfterPadding() -
                        mOrientationHelper.getDecoratedEnd(refChild));
                state.putInt("AnchorPosition", getPosition(refChild));
            } else {
                final View refChild = getChildClosestToStartExpose();
                state.putInt("AnchorPosition", getPosition(refChild));
                state.putInt("AnchorOffset", mOrientationHelper.getDecoratedStart(refChild) -
                        mOrientationHelper.getStartAfterPadding());
            }
        } else {
            state.putInt("AnchorPosition", RecyclerView.NO_POSITION);
        }
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            mCurrentPendingSavedState = (Bundle) state;
            requestLayout();
            if (DEBUG) {
                Log.d(TAG, "loaded saved state");
            }
        } else if (DEBUG) {
            Log.d(TAG, "invalid saved state class");
        }
    }

    /**
     * Sets the orientation of the layout. {@link LinearLayoutManager}
     * will do its best to keep scroll position.
     *
     * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
        mOrientationHelper = null;
    }

    public void setRecycleOffset(int recycleOffset) {
        this.recycleOffset = recycleOffset;
    }

    /**
     * Calculates the view layout order. (e.g. from end to start or start to end)
     * RTL layout support is applied automatically. So if layout is RTL and
     * {@link #getReverseLayout()} is {@code true}, elements will be laid out starting from left.
     */
    private void myResolveShouldLayoutReverse() {
        // A == B is the same result, but we rather keep it readable
        if (getOrientation() == VERTICAL || !isLayoutRTL()) {
            mShouldReverseLayoutExpose = getReverseLayout();
        } else {
            mShouldReverseLayoutExpose = !getReverseLayout();
        }
    }


    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        final int firstChildPos = getPosition(getChildAt(0));
        final int direction = targetPosition < firstChildPos != mShouldReverseLayoutExpose ? -1 : 1;
        if (getOrientation() == HORIZONTAL) {
            return new PointF(direction, 0);
        } else {
            return new PointF(0, direction);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        // layout algorithm:
        // 1) by checking children and other variables, find an anchor coordinate and an anchor
        //  item position.
        // 2) fill towards start, stacking from bottom
        // 3) fill towards end, stacking from top
        // 4) scroll to fulfill requirements like stack from bottom.
        // create layout state
        if (DEBUG) {
            Log.d(TAG, "is pre layout:" + state.isPreLayout());
        }
        if (mCurrentPendingSavedState != null && mCurrentPendingSavedState.getInt("AnchorPosition") >= 0) {
            mCurrentPendingScrollPosition = mCurrentPendingSavedState.getInt("AnchorPosition");
        }

        ensureLayoutStateExpose();
        mLayoutState.mRecycle = false;
        // resolve layout direction
        myResolveShouldLayoutReverse();

        mAnchorInfo.reset();
        mAnchorInfo.mLayoutFromEnd = mShouldReverseLayoutExpose ^ getStackFromEnd();
        // calculate anchor position and coordinate
        updateAnchorInfoForLayoutExpose(state, mAnchorInfo);


        if (DEBUG) {
            Log.d(TAG, "Anchor info:" + mAnchorInfo);
        }

        // LLM may decide to layout items for "extra" pixels to account for scrolling target,
        // caching or predictive animations.
        int extraForStart;
        int extraForEnd;
        final int extra = getExtraLayoutSpace(state);
        boolean before = state.getTargetScrollPosition() < mAnchorInfo.mPosition;
        if (before == mShouldReverseLayoutExpose) {
            extraForEnd = extra;
            extraForStart = 0;
        } else {
            extraForStart = extra;
            extraForEnd = 0;
        }
        extraForStart += mOrientationHelper.getStartAfterPadding();
        extraForEnd += mOrientationHelper.getEndPadding();
        if (state.isPreLayout() && mCurrentPendingScrollPosition != RecyclerView.NO_POSITION &&
                mPendingScrollPositionOffset != INVALID_OFFSET) {
            // if the child is visible and we are going to move it around, we should layout
            // extra items in the opposite direction to make sure new items animate nicely
            // instead of just fading in
            final View existing = findViewByPosition(mCurrentPendingScrollPosition);
            if (existing != null) {
                final int current;
                final int upcomingOffset;
                if (mShouldReverseLayoutExpose) {
                    current = mOrientationHelper.getEndAfterPadding() -
                            mOrientationHelper.getDecoratedEnd(existing);
                    upcomingOffset = current - mPendingScrollPositionOffset;
                } else {
                    current = mOrientationHelper.getDecoratedStart(existing)
                            - mOrientationHelper.getStartAfterPadding();
                    upcomingOffset = mPendingScrollPositionOffset - current;
                }
                if (upcomingOffset > 0) {
                    extraForStart += upcomingOffset;
                } else {
                    extraForEnd -= upcomingOffset;
                }
            }
        }
        int startOffset;
        int endOffset;
        onAnchorReady(state, mAnchorInfo);
        detachAndScrapAttachedViews(recycler);
        mLayoutState.mIsPreLayout = state.isPreLayout();
        mLayoutState.mOnRefresLayout = true;
        if (mAnchorInfo.mLayoutFromEnd) {
            // fill towards start
            updateLayoutStateToFillStartExpose(mAnchorInfo);
            mLayoutState.mExtra = extraForStart;
            fill(recycler, mLayoutState, state, false);
            startOffset = mLayoutState.mOffset;
            if (mLayoutState.mAvailable > 0) {
                extraForEnd += mLayoutState.mAvailable;
            }
            // fill towards end
            updateLayoutStateToFillEndExpose(mAnchorInfo);
            mLayoutState.mExtra = extraForEnd;
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
            fill(recycler, mLayoutState, state, false);
            endOffset = mLayoutState.mOffset;
        } else {
            // fill towards end
            updateLayoutStateToFillEndExpose(mAnchorInfo);
            mLayoutState.mExtra = extraForEnd;
            fill(recycler, mLayoutState, state, false);
            endOffset = mLayoutState.mOffset;
            if (mLayoutState.mAvailable > 0) {
                extraForStart += mLayoutState.mAvailable;
            }
            // fill towards start
            updateLayoutStateToFillStartExpose(mAnchorInfo);
            mLayoutState.mExtra = extraForStart;
            mLayoutState.mCurrentPosition += mLayoutState.mItemDirection;
            fill(recycler, mLayoutState, state, false);
            startOffset = mLayoutState.mOffset;
        }

        // changes may cause gaps on the UI, try to fix them.
        // TODO we can probably avoid this if neither stackFromEnd/reverseLayout/RTL values have
        // changed
        if (getChildCount() > 0) {
            // because layout from end may be changed by scroll to position
            // we re-calculate it.
            // find which side we should check for gaps.
            if (mShouldReverseLayoutExpose ^ getStackFromEnd()) {
                int fixOffset = fixLayoutEndGapExpose(endOffset, recycler, state, true);
                startOffset += fixOffset;
                endOffset += fixOffset;
                fixOffset = fixLayoutStartGapExpose(startOffset, recycler, state, false);
                startOffset += fixOffset;
                endOffset += fixOffset;
            } else {
                int fixOffset = fixLayoutStartGapExpose(startOffset, recycler, state, true);
                startOffset += fixOffset;
                endOffset += fixOffset;
                fixOffset = fixLayoutEndGapExpose(endOffset, recycler, state, false);
                startOffset += fixOffset;
                endOffset += fixOffset;
            }
        }
        layoutForPredictiveAnimationsExpose(recycler, state, startOffset, endOffset);
        if (!state.isPreLayout()) {
            mCurrentPendingScrollPosition = RecyclerView.NO_POSITION;
            mPendingScrollPositionOffset = INVALID_OFFSET;
            mOrientationHelper.onLayoutComplete();
        }
        mLastStackFromEnd = getStackFromEnd();
        mCurrentPendingSavedState = null; // we don't need this anymore
        if (DEBUG) {
            validateChildOrderExpose();
        }
    }

    /**
     * Method called when Anchor position is decided. Extending class can setup accordingly or
     * even update anchor info if necessary.
     *
     * @param state
     * @param anchorInfo Simple data structure to keep anchor point information for the next layout
     */
    public void onAnchorReady(RecyclerView.State state, AnchorInfo anchorInfo) {
    }


    private RecyclerView mRecyclerView;

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mRecyclerView = view;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        mRecyclerView = null;
    }

    @Override
    public int findFirstVisibleItemPosition() {
        ensureLayoutStateExpose();
        return super.findFirstVisibleItemPosition();
    }


    @Override
    public int findLastVisibleItemPosition() {
        ensureLayoutStateExpose();
        try {
            return super.findLastVisibleItemPosition();
        } catch (Exception e) {
            Log.d("LastItem", "itemCount: " + getItemCount());
            Log.d("LastItem", "childCount: " + getChildCount());
            Log.d("LastItem", "child: " + getChildAt(getChildCount() - 1));
            Log.d("LastItem", "RV childCount: " + mRecyclerView.getChildCount());
            Log.d("LastItem", "RV child: " + mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1));
            throw e;
        }
    }

    private View myFindReferenceChildClosestToEnd(RecyclerView.State state) {
        return this.mShouldReverseLayoutExpose ? this.myFindFirstReferenceChild(state.getItemCount()) : this.myFindLastReferenceChild(state.getItemCount());
    }

    private View myFindReferenceChildClosestToStart(RecyclerView.State state) {
        return this.mShouldReverseLayoutExpose ? this.myFindLastReferenceChild(state.getItemCount()) : this.myFindFirstReferenceChild(state.getItemCount());
    }


    private View myFindFirstReferenceChild(int itemCount) {
        return this.findReferenceChildInternal(0, this.getChildCount(), itemCount);
    }

    private View myFindLastReferenceChild(int itemCount) {
        return this.findReferenceChildInternal(this.getChildCount() - 1, -1, itemCount);
    }


    private View findReferenceChildInternal(int start, int end, int itemCount) {
        this.ensureLayoutStateExpose();
        View invalidMatch = null;
        View outOfBoundsMatch = null;
        int boundsStart = this.mOrientationHelper.getStartAfterPadding();
        int boundsEnd = this.mOrientationHelper.getEndAfterPadding();
        int diff = end > start ? 1 : -1;

        for (int i = start; i != end; i += diff) {
            View view = this.getChildAt(i);
            int position = this.getPosition(view);
            if (position >= 0 && position < itemCount) {
                if (((RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view;
                    }
                } else {
                    if (this.mOrientationHelper.getDecoratedStart(view) < boundsEnd && this.mOrientationHelper.getDecoratedEnd(view) >= boundsStart) {
                        return view;
                    }

                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view;
                    }
                }
            }
        }

        return outOfBoundsMatch != null ? outOfBoundsMatch : invalidMatch;
    }

    /**
     * If necessary, layouts new items for predictive animations
     */
    private void layoutForPredictiveAnimationsExpose(RecyclerView.Recycler recycler,
                                                     RecyclerView.State state, int startOffset, int endOffset) {
        // If there are scrap children that we did not layout, we need to find where they did go
        // and layout them accordingly so that animations can work as expected.
        // This case may happen if new views are added or an existing view expands and pushes
        // another view out of bounds.
        if (!state.willRunPredictiveAnimations() || getChildCount() == 0 || state.isPreLayout()
                || !supportsPredictiveItemAnimations()) {
            return;
        }

        // to make the logic simpler, we calculate the size of children and call fill.
        int scrapExtraStart = 0, scrapExtraEnd = 0;
        final List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
        final int scrapSize = scrapList.size();
        final int firstChildPos = getPosition(getChildAt(0));
        for (int i = 0; i < scrapSize; i++) {
            RecyclerView.ViewHolder scrap = scrapList.get(i);
            final int position = scrap.getPosition();
            final int direction = position < firstChildPos != mShouldReverseLayoutExpose
                    ? LayoutState.LAYOUT_START : LayoutState.LAYOUT_END;
            if (direction == LayoutState.LAYOUT_START) {
                scrapExtraStart += mOrientationHelper.getDecoratedMeasurement(scrap.itemView);
            } else {
                scrapExtraEnd += mOrientationHelper.getDecoratedMeasurement(scrap.itemView);
            }
        }

        if (DEBUG) {
            Log.d(TAG, "for unused scrap, decided to add " + scrapExtraStart
                    + " towards start and " + scrapExtraEnd + " towards end");
        }
        mLayoutState.mScrapList = scrapList;
        if (scrapExtraStart > 0) {
            View anchor = getChildClosestToStartExpose();
            updateLayoutStateToFillStartExpose(getPosition(anchor), startOffset);
            mLayoutState.mExtra = scrapExtraStart;
            mLayoutState.mAvailable = 0;
            mLayoutState.mCurrentPosition += mShouldReverseLayoutExpose ? 1 : -1;
            mLayoutState.mOnRefresLayout = true;
            fill(recycler, mLayoutState, state, false);
        }

        if (scrapExtraEnd > 0) {
            View anchor = getChildClosestToEndExpose();
            updateLayoutStateToFillEndExpose(getPosition(anchor), endOffset);
            mLayoutState.mExtra = scrapExtraEnd;
            mLayoutState.mAvailable = 0;
            mLayoutState.mCurrentPosition += mShouldReverseLayoutExpose ? -1 : 1;
            mLayoutState.mOnRefresLayout = true;
            fill(recycler, mLayoutState, state, false);
        }
        mLayoutState.mScrapList = null;
    }

    private void updateAnchorInfoForLayoutExpose(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (updateAnchorFromPendingDataExpose(state, anchorInfo)) {
            if (DEBUG) {
                Log.d(TAG, "updated anchor info from pending information");
            }
            return;
        }

        if (updateAnchorFromChildrenExpose(state, anchorInfo)) {
            if (DEBUG) {
                Log.d(TAG, "updated anchor info from existing children");
            }
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "deciding anchor info for fresh state");
        }
        anchorInfo.assignCoordinateFromPadding();
        anchorInfo.mPosition = getStackFromEnd() ? state.getItemCount() - 1 : 0;
    }

    /**
     * Finds an anchor child from existing Views. Most of the time, this is the view closest to
     * start or end that has a valid position (e.g. not removed).
     * <p/>
     * If a child has focus, it is given priority.
     */
    private boolean updateAnchorFromChildrenExpose(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (getChildCount() == 0) {
            return false;
        }
        View focused = getFocusedChild();
        if (focused != null && anchorInfo.assignFromViewIfValid(focused, state)) {
            if (DEBUG) {
                Log.d(TAG, "decided anchor child from focused view");
            }
            return true;
        }

        if (mLastStackFromEnd != getStackFromEnd()) {
            return false;
        }


        final View referenceChild = anchorInfo.mLayoutFromEnd ?
                myFindReferenceChildClosestToEnd(state)
                : myFindReferenceChildClosestToStart(state);


        if (referenceChild != null) {
            anchorInfo.assignFromView(referenceChild);
            // If all visible views are removed in 1 pass, reference child might be out of bounds.
            // If that is the case, offset it back to 0 so that we use these pre-layout children.
            if (!state.isPreLayout() && supportsPredictiveItemAnimations()) {
                // validate this child is at least partially visible. if not, offset it to start
                final boolean notVisible =
                        mOrientationHelper.getDecoratedStart(referenceChild) >= mOrientationHelper
                                .getEndAfterPadding()
                                || mOrientationHelper.getDecoratedEnd(referenceChild)
                                < mOrientationHelper.getStartAfterPadding();
                if (notVisible) {
                    anchorInfo.mCoordinate = anchorInfo.mLayoutFromEnd
                            ? mOrientationHelper.getEndAfterPadding()
                            : mOrientationHelper.getStartAfterPadding();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * If there is a pending scroll position or saved states, updates the anchor info from that
     * data and returns true
     */
    private boolean updateAnchorFromPendingDataExpose(RecyclerView.State state, AnchorInfo anchorInfo) {
        if (state.isPreLayout() || mCurrentPendingScrollPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        // validate scroll position
        if (mCurrentPendingScrollPosition < 0 || mCurrentPendingScrollPosition >= state.getItemCount()) {
            mCurrentPendingScrollPosition = RecyclerView.NO_POSITION;
            mPendingScrollPositionOffset = INVALID_OFFSET;
            if (DEBUG) {
                Log.e(TAG, "ignoring invalid scroll position " + mCurrentPendingScrollPosition);
            }
            return false;
        }

        // if child is visible, try to make it a reference child and ensure it is fully visible.
        // if child is not visible, align it depending on its virtual position.
        anchorInfo.mPosition = mCurrentPendingScrollPosition;
        if (mCurrentPendingSavedState != null && mCurrentPendingSavedState.getInt("AnchorPosition")>=0) {
            // Anchor offset depends on how that child was laid out. Here, we update it
            // according to our current view bounds
            anchorInfo.mLayoutFromEnd = mCurrentPendingSavedState.getBoolean("AnchorLayoutFromEnd");
            if (anchorInfo.mLayoutFromEnd) {
                anchorInfo.mCoordinate = mOrientationHelper.getEndAfterPadding() -
                        mCurrentPendingSavedState.getInt("AnchorOffset");
            } else {
                anchorInfo.mCoordinate = mOrientationHelper.getStartAfterPadding() +
                        mCurrentPendingSavedState.getInt("AnchorOffset");
            }
            return true;
        }

        if (mPendingScrollPositionOffset == INVALID_OFFSET) {
            View child = findViewByPosition(mCurrentPendingScrollPosition);
            if (child != null) {
                final int childSize = mOrientationHelper.getDecoratedMeasurement(child);
                if (childSize > mOrientationHelper.getTotalSpace()) {
                    // item does not fit. fix depending on layout direction
                    anchorInfo.assignCoordinateFromPadding();
                    return true;
                }
                final int startGap = mOrientationHelper.getDecoratedStart(child)
                        - mOrientationHelper.getStartAfterPadding();
                if (startGap < 0) {
                    anchorInfo.mCoordinate = mOrientationHelper.getStartAfterPadding();
                    anchorInfo.mLayoutFromEnd = false;
                    return true;
                }
                final int endGap = mOrientationHelper.getEndAfterPadding() -
                        mOrientationHelper.getDecoratedEnd(child);
                if (endGap < 0) {
                    anchorInfo.mCoordinate = mOrientationHelper.getEndAfterPadding();
                    anchorInfo.mLayoutFromEnd = true;
                    return true;
                }
                anchorInfo.mCoordinate = anchorInfo.mLayoutFromEnd
                        ? (mOrientationHelper.getDecoratedEnd(child) + mOrientationHelper
                        .getTotalSpaceChange())
                        : mOrientationHelper.getDecoratedStart(child);
            } else { // item is not visible.
                if (getChildCount() > 0) {
                    // get position of any child, does not matter
                    int pos = getPosition(getChildAt(0));
                    anchorInfo.mLayoutFromEnd = mCurrentPendingScrollPosition < pos
                            == mShouldReverseLayoutExpose;
                }
                anchorInfo.assignCoordinateFromPadding();
            }
            return true;
        }
        // override layout from end values for consistency
        anchorInfo.mLayoutFromEnd = mShouldReverseLayoutExpose;
        if (mShouldReverseLayoutExpose) {
            anchorInfo.mCoordinate = mOrientationHelper.getEndAfterPadding() -
                    mPendingScrollPositionOffset;
        } else {
            anchorInfo.mCoordinate = mOrientationHelper.getStartAfterPadding() +
                    mPendingScrollPositionOffset;
        }
        return true;
    }

    /**
     * @return The final offset amount for children
     */
    private int fixLayoutEndGapExpose(int endOffset, RecyclerView.Recycler recycler,
                                      RecyclerView.State state, boolean canOffsetChildren) {
        int gap = mOrientationHelper.getEndAfterPadding() - endOffset;
        int fixOffset = 0;
        if (gap > 0) {
            fixOffset = -scrollInternalBy(-gap, recycler, state);
        } else {
            return 0; // nothing to fix
        }
        // move offset according to scroll amount
        endOffset += fixOffset;
        if (canOffsetChildren) {
            // re-calculate gap, see if we could fix it
            gap = mOrientationHelper.getEndAfterPadding() - endOffset;
            if (gap > 0) {
                mOrientationHelper.offsetChildren(gap);
                return gap + fixOffset;
            }
        }
        return fixOffset;
    }

    /**
     * @return The final offset amount for children
     */
    private int fixLayoutStartGapExpose(int startOffset, RecyclerView.Recycler recycler,
                                        RecyclerView.State state, boolean canOffsetChildren) {
        int gap = startOffset - mOrientationHelper.getStartAfterPadding();
        int fixOffset = 0;
        if (gap > 0) {
            // check if we should fix this gap.
            fixOffset = -scrollInternalBy(gap, recycler, state);
        } else {
            return 0; // nothing to fix
        }
        startOffset += fixOffset;
        if (canOffsetChildren) {
            // re-calculate gap, see if we could fix it
            gap = startOffset - mOrientationHelper.getStartAfterPadding();
            if (gap > 0) {
                mOrientationHelper.offsetChildren(-gap);
                return fixOffset - gap;
            }
        }
        return fixOffset;
    }

    private void updateLayoutStateToFillEndExpose(AnchorInfo anchorInfo) {
        updateLayoutStateToFillEndExpose(anchorInfo.mPosition, anchorInfo.mCoordinate);
    }

    private void updateLayoutStateToFillEndExpose(int itemPosition, int offset) {
        mLayoutState.mAvailable = mOrientationHelper.getEndAfterPadding() - offset;
        mLayoutState.mItemDirection = mShouldReverseLayoutExpose ? LayoutState.ITEM_DIRECTION_HEAD :
                LayoutState.ITEM_DIRECTION_TAIL;
        mLayoutState.mCurrentPosition = itemPosition;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mOffset = offset;
        mLayoutState.mScrollingOffset = LayoutState.SCOLLING_OFFSET_NaN;
    }

    private void updateLayoutStateToFillStartExpose(AnchorInfo anchorInfo) {
        updateLayoutStateToFillStartExpose(anchorInfo.mPosition, anchorInfo.mCoordinate);
    }

    private void updateLayoutStateToFillStartExpose(int itemPosition, int offset) {
        mLayoutState.mAvailable = offset - mOrientationHelper.getStartAfterPadding();
        mLayoutState.mCurrentPosition = itemPosition;
        mLayoutState.mItemDirection = mShouldReverseLayoutExpose ? LayoutState.ITEM_DIRECTION_TAIL :
                LayoutState.ITEM_DIRECTION_HEAD;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START;
        mLayoutState.mOffset = offset;
        mLayoutState.mScrollingOffset = LayoutState.SCOLLING_OFFSET_NaN;

    }

    private Object[] emptyArgs = new Object[0];

    protected void ensureLayoutStateExpose() {
        if (mLayoutState == null) {
            mLayoutState = new LayoutState();
        }

        if (mOrientationHelper == null) {
            mOrientationHelper = OrientationHelperEx.createOrientationHelper(this, getOrientation());
        }

        try {
            mEnsureLayoutStateMethod.invoke(this, emptyArgs);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>Scroll the RecyclerView to make the position visible.</p>
     * <p/>
     * <p>RecyclerView will scroll the minimum amount that is necessary to make the
     * target position visible. If you are looking for a similar behavior to
     * {@link android.widget.ListView#setSelection(int)} or
     * {@link android.widget.ListView#setSelectionFromTop(int, int)}, use
     * {@link #scrollToPositionWithOffset(int, int)}.</p>
     * <p/>
     * <p>Note that scroll position change will not be reflected until the next layout call.</p>
     *
     * @param position Scroll to this adapter position
     * @see #scrollToPositionWithOffset(int, int)
     */
    @Override
    public void scrollToPosition(int position) {
        mCurrentPendingScrollPosition = position;
        mPendingScrollPositionOffset = INVALID_OFFSET;
        if (mCurrentPendingSavedState != null) {
            mCurrentPendingSavedState.putInt("AnchorPosition", RecyclerView.NO_POSITION);
        }
        requestLayout();
    }

    /**
     * Scroll to the specified adapter position with the given offset from resolved layout
     * start. Resolved layout start depends on {@link #getReverseLayout()},
     * {@link ViewCompat#getLayoutDirection(View)} and {@link #getStackFromEnd()}.
     * <p/>
     * For example, if layout is {@link #VERTICAL} and {@link #getStackFromEnd()} is true, calling
     * <code>scrollToPositionWithOffset(10, 20)</code> will layout such that
     * <code>item[10]</code>'s bottom is 20 pixels above the RecyclerView's bottom.
     * <p/>
     * Note that scroll position change will not be reflected until the next layout call.
     * <p/>
     * <p/>
     * If you are just trying to make a position visible, use {@link #scrollToPosition(int)}.
     *
     * @param position Index (starting at 0) of the reference item.
     * @param offset   The distance (in pixels) between the start edge of the item view and
     *                 start edge of the RecyclerView.
     * @see #setReverseLayout(boolean)
     * @see #scrollToPosition(int)
     */
    public void scrollToPositionWithOffset(int position, int offset) {
        mCurrentPendingScrollPosition = position;
        mPendingScrollPositionOffset = offset;
        if (mCurrentPendingSavedState != null) {
            mCurrentPendingSavedState.putInt("AnchorPosition", RecyclerView.NO_POSITION);
        }
        requestLayout();
    }

    protected void updateLayoutStateExpose(int layoutDirection, int requiredSpace,
                                           boolean canUseExistingSpace, RecyclerView.State state) {
        mLayoutState.mExtra = getExtraLayoutSpace(state);
        mLayoutState.mLayoutDirection = layoutDirection;
        int fastScrollSpace;
        if (layoutDirection == LayoutState.LAYOUT_END) {
            mLayoutState.mExtra += mOrientationHelper.getEndPadding();
            // get the first child in the direction we are going
            final View child = getChildClosestToEndExpose();
            // the direction in which we are traversing children
            mLayoutState.mItemDirection = mShouldReverseLayoutExpose ? LayoutState.ITEM_DIRECTION_HEAD
                    : LayoutState.ITEM_DIRECTION_TAIL;
            mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;
            mLayoutState.mOffset = mOrientationHelper.getDecoratedEnd(child) + computeAlignOffset(child, true, false);
            // calculate how much we can scroll without adding new children (independent of layout)
            fastScrollSpace = mLayoutState.mOffset
                    - mOrientationHelper.getEndAfterPadding();

        } else {
            final View child = getChildClosestToStartExpose();
            mLayoutState.mExtra += mOrientationHelper.getStartAfterPadding();
            mLayoutState.mItemDirection = mShouldReverseLayoutExpose ? LayoutState.ITEM_DIRECTION_TAIL
                    : LayoutState.ITEM_DIRECTION_HEAD;
            mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;

            mLayoutState.mOffset = mOrientationHelper.getDecoratedStart(child) + computeAlignOffset(child, false, false);
            fastScrollSpace = -mLayoutState.mOffset
                    + mOrientationHelper.getStartAfterPadding();
        }
        mLayoutState.mAvailable = requiredSpace;
        if (canUseExistingSpace) {
            mLayoutState.mAvailable -= fastScrollSpace;
        }
        mLayoutState.mScrollingOffset = fastScrollSpace;
    }

    /**
     * adjust align offset when fill view during scrolling or get margins when layout from anchor
     *
     * @param child
     * @param isLayoutEnd
     * @return
     */
    protected int computeAlignOffset(View child, boolean isLayoutEnd, boolean useAnchor) {
        return 0;
    }

    /**
     * adjust align offset when fill view during scrolling or get margins when layout from anchor
     *
     * @param position
     * @param isLayoutEnd
     * @return
     */
    protected int computeAlignOffset(int position, boolean isLayoutEnd, boolean useAnchor) {
        return 0;
    }

    public boolean isEnableMarginOverLap() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        if (getOrientation() == VERTICAL) {
            return 0;
        }
        return scrollInternalBy(dx, recycler, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                                  RecyclerView.State state) {
        if (getOrientation() == HORIZONTAL) {
            return 0;
        }
        return scrollInternalBy(dy, recycler, state);
    }

    /**
     * Handle scroll event internally, cover both horizontal and vertical
     *
     * @param dy       Pixel that will be scrolled
     * @param recycler Recycler hold recycled views
     * @param state    Current {@link RecyclerView} state, hold whether in preLayout, etc.
     * @return The actual scrolled pixel, it may not be the same as {@code dy}, like when reaching the edge of view
     */
    protected int scrollInternalBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }


        // indicate whether need recycle in this pass, true when scrolling, false when layout
        mLayoutState.mRecycle = true;
        ensureLayoutStateExpose();
        final int layoutDirection = dy > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
        final int absDy = Math.abs(dy);
        updateLayoutStateExpose(layoutDirection, absDy, true, state);
        final int freeScroll = mLayoutState.mScrollingOffset;

        mLayoutState.mOnRefresLayout = false;

        final int consumed = freeScroll + fill(recycler, mLayoutState, state, false);
        if (consumed < 0) {
            if (DEBUG) {
                Log.d(TAG, "Don't have any more elements to scroll");
            }
            return 0;
        }
        final int scrolled = absDy > consumed ? layoutDirection * consumed : dy;
        mOrientationHelper.offsetChildren(-scrolled);
        if (DEBUG) {
            Log.d(TAG, "scroll req: " + dy + " scrolled: " + scrolled);
        }

        return scrolled;
    }

    @Override
    public void assertNotInLayoutOrScroll(String message) {
        if (mCurrentPendingSavedState == null) {
            super.assertNotInLayoutOrScroll(message);
        }
    }

    /**
     * Recycles children between given indices.
     *
     * @param startIndex inclusive
     * @param endIndex   exclusive
     */
    protected void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Recycling " + Math.abs(startIndex - endIndex) + " items");
        }
        if (endIndex > startIndex) {
            for (int i = endIndex - 1; i >= startIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        } else {
            for (int i = startIndex; i > endIndex; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        }
    }

    /**
     * Recycles views that went out of bounds after scrolling towards the end of the layout.
     *
     * @param recycler Recycler instance of {@link RecyclerView}
     * @param dt       This can be used to add additional padding to the visible area. This is used
     *                 to
     *                 detect children that will go out of bounds after scrolling, without actually
     *                 moving them.
     */
    private void recycleViewsFromStartExpose(RecyclerView.Recycler recycler, int dt) {
        if (dt < 0) {
            if (DEBUG) {
                Log.d(TAG, "Called recycle from start with a negative value. This might happen"
                        + " during layout changes but may be sign of a bug");
            }
            return;
        }
        // ignore padding, ViewGroup may not clip children.
        final int limit = dt;
        final int childCount = getChildCount();
        if (mShouldReverseLayoutExpose) {
            for (int i = childCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedEnd(child) + recycleOffset > limit) {// stop here
                    recycleChildren(recycler, childCount - 1, i);
                    return;
                }
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedEnd(child) + recycleOffset > limit) {// stop here
                    recycleChildren(recycler, 0, i);
                    return;
                }
            }
        }
    }


    /**
     * Recycles views that went out of bounds after scrolling towards the start of the layout.
     *
     * @param recycler Recycler instance of {@link RecyclerView}
     * @param dt       This can be used to add additional padding to the visible area. This is used
     *                 to detect children that will go out of bounds after scrolling, without
     *                 actually moving them.
     */
    private void recycleViewsFromEndExpose(RecyclerView.Recycler recycler, int dt) {
        final int childCount = getChildCount();
        if (dt < 0) {
            if (DEBUG) {
                Log.d(TAG, "Called recycle from end with a negative value. This might happen"
                        + " during layout changes but may be sign of a bug");
            }
            return;
        }
        final int limit = mOrientationHelper.getEnd() - dt;
        if (mShouldReverseLayoutExpose) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedStart(child) - recycleOffset < limit) {// stop here
                    recycleChildren(recycler, 0, i);
                    return;
                }
            }
        } else {
            for (int i = childCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (mOrientationHelper.getDecoratedStart(child) - recycleOffset < limit) {// stop here
                    recycleChildren(recycler, childCount - 1, i);
                    return;
                }
            }
        }

    }

    /**
     * Helper method to call appropriate recycle method depending on current layout direction
     *
     * @param recycler    Current recycler that is attached to RecyclerView
     * @param layoutState Current layout state. Right now, this object does not change but
     *                    we may consider moving it out of this view so passing around as a
     *                    parameter for now, rather than accessing {@link #mLayoutState}
     * @see #recycleViewsFromStartExpose(RecyclerView.Recycler, int)
     * @see #recycleViewsFromEndExpose(RecyclerView.Recycler, int)
     * @see LinearLayoutManager.LayoutState#mLayoutDirection
     */
    private void recycleByLayoutStateExpose(RecyclerView.Recycler recycler, LayoutState layoutState) {
        if (!layoutState.mRecycle) {
            return;
        }
        if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            recycleViewsFromEndExpose(recycler, layoutState.mScrollingOffset);
        } else {
            recycleViewsFromStartExpose(recycler, layoutState.mScrollingOffset);
        }
    }

    private com.alibaba.android.vlayout.layout.LayoutChunkResult layoutChunkResultCache
            = new com.alibaba.android.vlayout.layout.LayoutChunkResult();

    /**
     * The magic functions :). Fills the given layout, defined by the layoutState. This is fairly
     * independent from the rest of the {@link LinearLayoutManager}
     * and with little change, can be made publicly available as a helper class.
     *
     * @param recycler        Current recycler that is attached to RecyclerView
     * @param layoutState     Configuration on how we should fill out the available space.
     * @param state           Context passed by the RecyclerView to control scroll steps.
     * @param stopOnFocusable If true, filling stops in the first focusable new child
     * @return Number of pixels that it added. Useful for scoll functions.
     */
    protected int fill(RecyclerView.Recycler recycler, LayoutState layoutState,
                       RecyclerView.State state, boolean stopOnFocusable) {
        // max offset we should set is mFastScroll + available
        final int start = layoutState.mAvailable;
        if (layoutState.mScrollingOffset != LayoutState.SCOLLING_OFFSET_NaN) {
            // TODO ugly bug fix. should not happen
            if (layoutState.mAvailable < 0) {
                layoutState.mScrollingOffset += layoutState.mAvailable;
            }
            recycleByLayoutStateExpose(recycler, layoutState);
        }
        int remainingSpace = layoutState.mAvailable + layoutState.mExtra + (
            layoutState.mLayoutDirection == LayoutState.LAYOUT_START ? 0 : recycleOffset); //FIXME  opt here to fix bg and shake
        while (remainingSpace > 0 && layoutState.hasMore(state)) {
            layoutChunkResultCache.resetInternal();
            layoutChunk(recycler, state, layoutState, layoutChunkResultCache);
            if (layoutChunkResultCache.mFinished) {
                break;
            }
            layoutState.mOffset += layoutChunkResultCache.mConsumed * layoutState.mLayoutDirection;
            /**
             * Consume the available space if:
             * * layoutChunk did not request to be ignored
             * * OR we are laying out scrap children
             * * OR we are not doing pre-layout
             */
            if (!layoutChunkResultCache.mIgnoreConsumed || mLayoutState.mScrapList != null
                    || !state.isPreLayout()) {
                layoutState.mAvailable -= layoutChunkResultCache.mConsumed;
                // we keep a separate remaining space because mAvailable is important for recycling
                remainingSpace -= layoutChunkResultCache.mConsumed;
            }

            if (layoutState.mScrollingOffset != LayoutState.SCOLLING_OFFSET_NaN) {
                layoutState.mScrollingOffset += layoutChunkResultCache.mConsumed;
                if (layoutState.mAvailable < 0) {
                    layoutState.mScrollingOffset += layoutState.mAvailable;
                }
                recycleByLayoutStateExpose(recycler, layoutState);
            }
            if (stopOnFocusable && layoutChunkResultCache.mFocusable) {
                break;
            }
        }
        if (DEBUG) {
            validateChildOrderExpose();
        }
        return start - layoutState.mAvailable;
    }

    protected void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
                               LayoutState layoutState, com.alibaba.android.vlayout.layout.LayoutChunkResult result) {
        View view = layoutState.next(recycler);
        if (view == null) {
            if (DEBUG && layoutState.mScrapList == null) {
                throw new RuntimeException("received null view when unexpected");
            }
            // if we are laying out views in scrap, this may return null which means there is
            // no more items to layout.
            result.mFinished = true;
            return;
        }
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (layoutState.mScrapList == null) {
            // can not find in scrapList
            if (mShouldReverseLayoutExpose == (layoutState.mLayoutDirection
                    == LayoutState.LAYOUT_START)) {
                addView(view);
            } else {
                addView(view, 0);
            }
        } else {
            if (mShouldReverseLayoutExpose == (layoutState.mLayoutDirection
                    == LayoutState.LAYOUT_START)) {
                addDisappearingView(view);
            } else {
                addDisappearingView(view, 0);
            }
        }
        measureChildWithMargins(view, 0, 0);
        result.mConsumed = mOrientationHelper.getDecoratedMeasurement(view);
        int left, top, right, bottom;
        if (getOrientation() == VERTICAL) {
            if (isLayoutRTL()) {
                right = getWidth() - getPaddingRight();
                left = right - mOrientationHelper.getDecoratedMeasurementInOther(view);
            } else {
                left = getPaddingLeft();
                right = left + mOrientationHelper.getDecoratedMeasurementInOther(view);
            }

            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                bottom = layoutState.mOffset;
                top = layoutState.mOffset - result.mConsumed;
            } else {
                top = layoutState.mOffset;
                bottom = layoutState.mOffset + result.mConsumed;
            }
        } else {
            top = getPaddingTop();
            bottom = top + mOrientationHelper.getDecoratedMeasurementInOther(view);

            // whether this layout pass is layout form start to end or in reverse
            if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                right = layoutState.mOffset;
                left = layoutState.mOffset - result.mConsumed;
            } else {
                left = layoutState.mOffset;
                right = layoutState.mOffset + result.mConsumed;
            }
        }
        // We calculate everything with View's bounding box (which includes decor and margins)
        // To calculate correct layout position, we subtract margins.
        layoutDecorated(view, left + params.leftMargin, top + params.topMargin,
                right - params.rightMargin, bottom - params.bottomMargin);
        if (DEBUG) {
            Log.d(TAG, "laid out child at position " + getPosition(view) + ", with l:"
                    + (left + params.leftMargin) + ", t:" + (top + params.topMargin) + ", r:"
                    + (right - params.rightMargin) + ", b:" + (bottom - params.bottomMargin));
        }
        // Consume the available space if the view is not removed OR changed
        if (params.isItemRemoved() || params.isItemChanged()) {
            result.mIgnoreConsumed = true;
        }
        result.mFocusable = view.isFocusable();
    }

    /**
     * Converts a focusDirection to orientation.
     *
     * @param focusDirection One of {@link View#FOCUS_UP}, {@link View#FOCUS_DOWN},
     *                       {@link View#FOCUS_LEFT}, {@link View#FOCUS_RIGHT},
     *                       {@link View#FOCUS_BACKWARD}, {@link View#FOCUS_FORWARD}
     *                       or 0 for not applicable
     * @return {@link LayoutState#LAYOUT_START} or {@link LayoutState#LAYOUT_END} if focus direction
     * is applicable to current state, {@link LayoutState#INVALID_LAYOUT} otherwise.
     */
    private int convertFocusDirectionToLayoutDirectionExpose(int focusDirection) {
        int orientation = getOrientation();
        switch (focusDirection) {
            case View.FOCUS_BACKWARD:
                return LayoutState.LAYOUT_START;
            case View.FOCUS_FORWARD:
                return LayoutState.LAYOUT_END;
            case View.FOCUS_UP:
                return orientation == VERTICAL ? LayoutState.LAYOUT_START
                        : LayoutState.INVALID_LAYOUT;
            case View.FOCUS_DOWN:
                return orientation == VERTICAL ? LayoutState.LAYOUT_END
                        : LayoutState.INVALID_LAYOUT;
            case View.FOCUS_LEFT:
                return orientation == HORIZONTAL ? LayoutState.LAYOUT_START
                        : LayoutState.INVALID_LAYOUT;
            case View.FOCUS_RIGHT:
                return orientation == HORIZONTAL ? LayoutState.LAYOUT_END
                        : LayoutState.INVALID_LAYOUT;
            default:
                if (DEBUG) {
                    Log.d(TAG, "Unknown focus request:" + focusDirection);
                }
                return LayoutState.INVALID_LAYOUT;
        }

    }

    /**
     * Convenience method to find the child closes to start. Caller should check it has enough
     * children.
     *
     * @return The child closes to start of the layout from user's perspective.
     */
    private View getChildClosestToStartExpose() {
        return getChildAt(mShouldReverseLayoutExpose ? getChildCount() - 1 : 0);
    }

    /**
     * Convenience method to find the child closes to end. Caller should check it has enough
     * children.
     *
     * @return The child closes to end of the layout from user's perspective.
     */
    private View getChildClosestToEndExpose() {
        return getChildAt(mShouldReverseLayoutExpose ? 0 : getChildCount() - 1);
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection,
                                    RecyclerView.Recycler recycler, RecyclerView.State state) {
        myResolveShouldLayoutReverse();
        if (getChildCount() == 0) {
            return null;
        }

        final int layoutDir = convertFocusDirectionToLayoutDirectionExpose(focusDirection);
        if (layoutDir == LayoutState.INVALID_LAYOUT) {
            return null;
        }

        View referenceChild = null;
        if (layoutDir == LayoutState.LAYOUT_START) {
            referenceChild = myFindReferenceChildClosestToStart(state);
        } else {
            referenceChild = myFindReferenceChildClosestToEnd(state);

        }

        if (referenceChild == null) {
            if (DEBUG) {
                Log.d(TAG,
                        "Cannot find a child with a valid position to be used for focus search.");
            }
            return null;
        }
        ensureLayoutStateExpose();
        final int maxScroll = (int) (MAX_SCROLL_FACTOR * mOrientationHelper.getTotalSpace());
        updateLayoutStateExpose(layoutDir, maxScroll, false, state);
        mLayoutState.mScrollingOffset = LayoutState.SCOLLING_OFFSET_NaN;
        mLayoutState.mRecycle = false;
        mLayoutState.mOnRefresLayout = false;
        fill(recycler, mLayoutState, state, true);
        final View nextFocus;
        if (layoutDir == LayoutState.LAYOUT_START) {
            nextFocus = getChildClosestToStartExpose();
        } else {
            nextFocus = getChildClosestToEndExpose();
        }
        if (nextFocus == referenceChild || !nextFocus.isFocusable()) {
            return null;
        }
        return nextFocus;
    }

    /**
     * Used for debugging.
     * Logs the internal representation of children to default logger.
     */
    private void logChildren() {
        Log.d(TAG, "internal representation of views on the screen");
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Log.d(TAG, "item " + getPosition(child) + ", coord:"
                    + mOrientationHelper.getDecoratedStart(child));
        }
        Log.d(TAG, "==============");
    }

    /**
     * Used for debugging.
     * Validates that child views are laid out in correct order. This is important because rest of
     * the algorithm relies on this constraint.
     * <p/>
     * In default layout, child 0 should be closest to screen position 0 and last child should be
     * closest to position WIDTH or HEIGHT.
     * In reverse layout, last child should be closes to screen position 0 and first child should
     * be closest to position WIDTH  or HEIGHT
     */
    private void validateChildOrderExpose() {
        Log.d(TAG, "validating child count " + getChildCount());
        if (getChildCount() < 1) {
            return;
        }
        int lastPos = getPosition(getChildAt(0));
        int lastScreenLoc = mOrientationHelper.getDecoratedStart(getChildAt(0));
        if (mShouldReverseLayoutExpose) {
            for (int i = 1; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int pos = getPosition(child);
                int screenLoc = mOrientationHelper.getDecoratedStart(child);
                if (pos < lastPos) {
                    logChildren();
                    throw new RuntimeException("detected invalid position. loc invalid? " +
                            (screenLoc < lastScreenLoc));
                }
                if (screenLoc > lastScreenLoc) {
                    logChildren();
                    throw new RuntimeException("detected invalid location");
                }
            }
        } else {
            for (int i = 1; i < getChildCount(); i++) {
                View child = getChildAt(i);
                int pos = getPosition(child);
                int screenLoc = mOrientationHelper.getDecoratedStart(child);
                if (pos < lastPos) {
                    logChildren();
                    throw new RuntimeException("detected invalid position. loc invalid? " +
                            (screenLoc < lastScreenLoc));
                }
                if (screenLoc < lastScreenLoc) {
                    logChildren();
                    throw new RuntimeException("detected invalid location");
                }
            }
        }
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return mCurrentPendingSavedState == null && mLastStackFromEnd == getStackFromEnd();
    }

    //==================================
    // Extends method
    //==================================
    protected void addHiddenView(View view, boolean head) {
        int index = head ? 0 : -1;
        addView(view, index);
        mChildHelperWrapper.hide(view);
    }

    protected void hideView(View view) {
        mChildHelperWrapper.hide(view);
    }

    protected void showView(View view) {
        mChildHelperWrapper.show(view);
    }

    protected View findHiddenView(int position) {
        return mChildHelperWrapper.findHiddenNonRemovedView(position, RecyclerView.INVALID_TYPE);
    }

    protected boolean isHidden(View view) {
        return mChildHelperWrapper.isHidden(view);
    }

    static final int FLAG_INVALID = 1 << 2;

    static final int FLAG_UPDATED = 1 << 1;

    private static Field vhField = null;
    private static Method vhSetFlags = null;

    protected static boolean isViewHolderUpdated(RecyclerView.ViewHolder holder) {
        return new ViewHolderWrapper(holder).requireUpdated();
    }

    protected static void attachViewHolder(RecyclerView.LayoutParams params, RecyclerView.ViewHolder holder) {
        try {

            if (vhField == null) {
                vhField = RecyclerView.LayoutParams.class.getDeclaredField("mViewHolder");
            }

            vhField.setAccessible(true);
            vhField.set(params, holder);

            if (vhSetFlags == null) {
                vhSetFlags = RecyclerView.ViewHolder.class.getDeclaredMethod("setFlags", int.class, int.class);
                vhSetFlags.setAccessible(true);
            }

            vhSetFlags.invoke(holder, FLAG_INVALID, FLAG_INVALID);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    /**
     * Helper class that keeps temporary state while {LayoutManager} is filling out the empty
     * space.
     */
    public static class LayoutState {

        private Method vhIsRemoved = null;

        final static String TAG = "_ExposeLLayoutManager#LayoutState";

        public final static int LAYOUT_START = -1;

        public final static int LAYOUT_END = 1;

        final static int INVALID_LAYOUT = Integer.MIN_VALUE;

        public final static int ITEM_DIRECTION_HEAD = -1;

        public final static int ITEM_DIRECTION_TAIL = 1;

        final static int SCOLLING_OFFSET_NaN = Integer.MIN_VALUE;

        public boolean mOnRefresLayout = false;

        /**
         * We may not want to recycle children in some cases (e.g. layout)
         */
        public boolean mRecycle = true;

        /**
         * Pixel offset where layout should start
         */
        public int mOffset;

        /**
         * Number of pixels that we should fill, in the layout direction.
         */
        public int mAvailable;

        /**
         * Current position on the adapter to get the next item.
         */
        public int mCurrentPosition;

        /**
         * Defines the direction in which the data adapter is traversed.
         * Should be {@link #ITEM_DIRECTION_HEAD} or {@link #ITEM_DIRECTION_TAIL}
         */
        public int mItemDirection;

        /**
         * Defines the direction in which the layout is filled.
         * Should be {@link #LAYOUT_START} or {@link #LAYOUT_END}
         */
        public int mLayoutDirection;

        /**
         * Used when LayoutState is constructed in a scrolling state.
         * It should be set the amount of scrolling we can make without creating a new view.
         * Settings this is required for efficient view recycling.
         */
        public int mScrollingOffset;

        /**
         * Used if you want to pre-layout items that are not yet visible.
         * The difference with {@link #mAvailable} is that, when recycling, distance laid out for
         * {@link #mExtra} is not considered to avoid recycling visible children.
         */
        public int mExtra = 0;


        /**
         * Used when Layout is fixed scrolling
         */
        public int mFixOffset = 0;

        /**
         * Equal to {@link RecyclerView.State#isPreLayout()}. When consuming scrap, if this value
         * is set to true, we skip removed views since they should not be laid out in post layout
         * step.
         */
        public boolean mIsPreLayout = false;

        /**
         * When LLM needs to layout particular views, it sets this list in which case, LayoutState
         * will only return views from this list and return null if it cannot find an item.
         */
        public List<RecyclerView.ViewHolder> mScrapList = null;

        public LayoutState() {
            try {
                vhIsRemoved = RecyclerView.ViewHolder.class.getDeclaredMethod("isRemoved");
                vhIsRemoved.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }


        /**
         * @return true if there are more items in the data adapter
         */
        public boolean hasMore(RecyclerView.State state) {
            return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
        }

        /**
         * Gets the view for the next element that we should layout.
         * Also updates current item index to the next item, based on {@link #mItemDirection}
         *
         * @return The next element that we should layout.
         */
        public View next(RecyclerView.Recycler recycler) {
            if (mScrapList != null) {
                return nextFromLimitedList();
            }
            final View view = recycler.getViewForPosition(mCurrentPosition);
            mCurrentPosition += mItemDirection;
            return view;
        }

        /**
         * Returns next item from limited list.
         * <p/>
         * Upon finding a valid VH, sets current item position to VH.itemPosition + mItemDirection
         *
         * @return View if an item in the current position or direction exists if not null.
         */
        @SuppressLint("LongLogTag")
        private View nextFromLimitedList() {
            int size = mScrapList.size();
            RecyclerView.ViewHolder closest = null;
            int closestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < size; i++) {
                RecyclerView.ViewHolder viewHolder = mScrapList.get(i);
                if (!mIsPreLayout) {
                    boolean isRemoved = false;
                    try {
                        isRemoved = (boolean) vhIsRemoved.invoke(viewHolder);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    if (!mIsPreLayout && isRemoved) {
                        continue;
                    }
                }
                final int distance = (viewHolder.getPosition() - mCurrentPosition) * mItemDirection;
                if (distance < 0) {
                    continue; // item is not in current direction
                }
                if (distance < closestDistance) {
                    closest = viewHolder;
                    closestDistance = distance;
                    if (distance == 0) {
                        break;
                    }
                }
            }
            if (DEBUG) {
                Log.d(TAG, "layout from scrap. found view:?" + (closest != null));
            }
            if (closest != null) {
                mCurrentPosition = closest.getPosition() + mItemDirection;
                return closest.itemView;
            }
            return null;
        }

        @SuppressLint("LongLogTag")
        void log() {
            Log.d(TAG, "avail:" + mAvailable + ", ind:" + mCurrentPosition + ", dir:" +
                    mItemDirection + ", offset:" + mOffset + ", layoutDir:" + mLayoutDirection);
        }
    }

    /**
     * Simple data class to keep Anchor information
     */
    protected class AnchorInfo {
        public int mPosition;
        public int mCoordinate;
        public boolean mLayoutFromEnd;

        void reset() {
            mPosition = RecyclerView.NO_POSITION;
            mCoordinate = INVALID_OFFSET;
            mLayoutFromEnd = false;
        }

        /**
         * assigns anchor coordinate from the RecyclerView's padding depending on current
         * layoutFromEnd value
         */
        void assignCoordinateFromPadding() {
            mCoordinate = mLayoutFromEnd
                    ? mOrientationHelper.getEndAfterPadding()
                    : mOrientationHelper.getStartAfterPadding();
        }

        @Override
        public String toString() {
            return "AnchorInfo{" +
                    "mPosition=" + mPosition +
                    ", mCoordinate=" + mCoordinate +
                    ", mLayoutFromEnd=" + mLayoutFromEnd +
                    '}';
        }

        /**
         * Assign anchor position information from the provided view if it is valid as a reference
         * child.
         */
        public boolean assignFromViewIfValid(View child, RecyclerView.State state) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
            if (!lp.isItemRemoved() && lp.getViewPosition() >= 0
                    && lp.getViewPosition() < state.getItemCount()) {
                assignFromView(child);
                return true;
            }
            return false;
        }

        public void assignFromView(View child) {
            if (mLayoutFromEnd) {
                mCoordinate = mOrientationHelper.getDecoratedEnd(child) + computeAlignOffset(child, mLayoutFromEnd, true) +
                        mOrientationHelper.getTotalSpaceChange();
                if (DEBUG) {
                    Log.d(TAG, "1 mLayoutFromEnd " + mLayoutFromEnd + " mOrientationHelper.getDecoratedEnd(child) "
                        + mOrientationHelper.getDecoratedEnd(child) + " computeAlignOffset(child, mLayoutFromEnd, true) " + computeAlignOffset(child, mLayoutFromEnd, true));
                }
            } else {
                mCoordinate = mOrientationHelper.getDecoratedStart(child) + computeAlignOffset(child, mLayoutFromEnd, true);
                if (DEBUG) {
                    Log.d(TAG, "2 mLayoutFromEnd " + mLayoutFromEnd + " mOrientationHelper.getDecoratedStart(child) "
                        + mOrientationHelper.getDecoratedStart(child) + " computeAlignOffset(child, mLayoutFromEnd, true) " + computeAlignOffset(child, mLayoutFromEnd, true));
                }
            }

            mPosition = getPosition(child);
            if (DEBUG) {
                Log.d(TAG, "position " + mPosition + " mCoordinate " + mCoordinate);
            }
        }
    }


    static class ViewHolderWrapper {
        private RecyclerView.ViewHolder mHolder;

        private static Method mShouldIgnore;
        private static Method mIsInvalid;
        private static Method mIsRemoved;
        private static Method mIsChanged;
        private static Method mSetFlags;


        static {
            try {
                mShouldIgnore = RecyclerView.ViewHolder.class.getDeclaredMethod("shouldIgnore");
                mShouldIgnore.setAccessible(true);
                mIsInvalid = RecyclerView.ViewHolder.class.getDeclaredMethod("isInvalid");
                mIsInvalid.setAccessible(true);
                mIsRemoved = RecyclerView.ViewHolder.class.getDeclaredMethod("isRemoved");
                mIsRemoved.setAccessible(true);

                mSetFlags = RecyclerView.ViewHolder.class.getDeclaredMethod("setFlags", int.class, int.class);
                mSetFlags.setAccessible(true);

                try {
                    mIsChanged = RecyclerView.ViewHolder.class.getDeclaredMethod("isChanged");
                } catch (NoSuchMethodException e) {
                    mIsChanged = RecyclerView.ViewHolder.class.getDeclaredMethod("isUpdated");
                }

                mIsChanged.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }


        public static void setFlags(RecyclerView.ViewHolder viewHolder, int flags, int mask) {
            try {
                mSetFlags.invoke(viewHolder, flags, mask);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        public ViewHolderWrapper(RecyclerView.ViewHolder holder) {
            this.mHolder = holder;

        }

        boolean isInvalid() {
            if (mIsInvalid == null) return true;
            try {
                return (boolean) mIsInvalid.invoke(mHolder);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return true;
        }

        boolean isRemoved() {
            if (mIsRemoved == null) return true;
            try {
                return (boolean) mIsRemoved.invoke(mHolder);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return true;
        }

        boolean isChanged() {
            if (mIsChanged == null) return true;
            try {
                return (boolean) mIsChanged.invoke(mHolder);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return true;
        }

        void setFlags(int flags, int mask) {
            try {
                mSetFlags.invoke(mHolder, flags, mask);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }


        public boolean requireUpdated() {
            return isInvalid() || isRemoved() || isChanged();
        }


    }

    class ChildHelperWrapper {
        private Object mInnerChildHelper;

        private Method mHideMethod;

        private Method mFindHiddenNonRemovedViewMethod;

        /** start from 25.2.0, maybe earlier, this method reduce parameters from two to one */
        private Method mFindHiddenNonRemovedViewMethod25;

        private Method mIsHideMethod;

        private Field mHiddenViewField;

        private Object mInnerBucket;

        private Method mClearMethod;

        private Field mChildHelperField;

        private List mInnerHiddenView;

        private RecyclerView.LayoutManager mLayoutManager;

        private Object[] args = new Object[1];

        void ensureChildHelper() {
            try {
                if (mInnerChildHelper == null) {
                    mInnerChildHelper = mChildHelperField.get(mLayoutManager);
                    if (mInnerChildHelper == null) return;

                    Class<?> helperClz = mInnerChildHelper.getClass();
                    mHideMethod = helperClz.getDeclaredMethod("hide", View.class);
                    mHideMethod.setAccessible(true);
                    try {
                        mFindHiddenNonRemovedViewMethod = helperClz.getDeclaredMethod("findHiddenNonRemovedView", int.class, int.class);
                        mFindHiddenNonRemovedViewMethod.setAccessible(true);
                    } catch (NoSuchMethodException nsme) {
                        mFindHiddenNonRemovedViewMethod25 = helperClz.getDeclaredMethod("findHiddenNonRemovedView", int.class);
                        mFindHiddenNonRemovedViewMethod25.setAccessible(true);
                    }
                    mIsHideMethod = helperClz.getDeclaredMethod("isHidden", View.class);
                    mIsHideMethod.setAccessible(true);

                    Field bucketField = helperClz.getDeclaredField("mBucket");
                    bucketField.setAccessible(true);

                    mInnerBucket = bucketField.get(mInnerChildHelper);
                    mClearMethod = mInnerBucket.getClass().getDeclaredMethod("clear", int.class);
                    mClearMethod.setAccessible(true);

                    mHiddenViewField = helperClz.getDeclaredField("mHiddenViews");
                    mHiddenViewField.setAccessible(true);
                    mInnerHiddenView = (List) mHiddenViewField.get(mInnerChildHelper);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        ChildHelperWrapper(RecyclerView.LayoutManager layoutManager) {
            this.mLayoutManager = layoutManager;
            try {
                mChildHelperField = RecyclerView.LayoutManager.class.getDeclaredField("mChildHelper");
                mChildHelperField.setAccessible(true);
                ensureChildHelper();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void hide(View view) {
            try {
                ensureChildHelper();
                if (mInnerHiddenView.indexOf(view) < 0) {
                    args[0] = view;
                    mHideMethod.invoke(mInnerChildHelper, args);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void show(View view) {
            try {
                ensureChildHelper();
                int index = mRecyclerView.indexOfChild(view);
                args[0] = Integer.valueOf(index);
                mClearMethod.invoke(mInnerBucket, args);
                if (mInnerHiddenView != null)
                    mInnerHiddenView.remove(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        View findHiddenNonRemovedView(int position, int type) {
            try {
                ensureChildHelper();
                if (mFindHiddenNonRemovedViewMethod != null) {
                    return (View) mFindHiddenNonRemovedViewMethod.invoke(mInnerChildHelper, position, RecyclerView.INVALID_TYPE);
                } else if (mFindHiddenNonRemovedViewMethod25 != null) {
                    return (View) mFindHiddenNonRemovedViewMethod25.invoke(mInnerChildHelper, position);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        boolean isHidden(View view) {
            try {
                ensureChildHelper();
                args[0] = view;
                return (boolean) mIsHideMethod.invoke(mInnerChildHelper, args);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

    }

}

