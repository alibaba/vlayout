package com.alibaba.android.vlayout;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.alibaba.android.vlayout.layout.LayoutChunkResult;

import static com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

/**
 * @author villadora
 * @date 2015-8-14
 * @since 1.0.0
 */

public abstract class LayoutHelper {

    public static final Range<Integer> RANGE_ALL = Range.create(Integer.MIN_VALUE, Integer.MAX_VALUE);
    public static final Range<Integer> RANGE_EMPTY = Range.create(-1, -1);

    @NonNull
    Range<Integer> mRange = RANGE_EMPTY;


    /**
     * Is the position should be handle by this {@link LayoutHelper}
     *
     * @param position position without offset, which is the true index in {@link VirtualLayoutManager}
     * @return true if position in range returned by {@link #getRange()}
     */
    public boolean isOutOfRange(int position) {
        return !mRange.contains(position);
    }


    /**
     * Set range of items, which will be handled by this layoutHelper
     * start position must be greater than end position, otherwise {@link IllegalArgumentException}
     * will be thrown
     *
     * @param start start position of items handled by this layoutHelper
     * @param end   end position of items handled by this layoutHelper, if end < start, it will throw {@link IllegalArgumentException}
     * @throws MismatchChildCountException when the (start - end) doesn't equal to itemCount
     */
    void setRange(int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end should be larger or equeal then start position");
        }

        if ((end - start + 1) != getItemCount()) {
            throw new MismatchChildCountException("ItemCount mismatch when range: " + mRange.toString() + " childCount: " + getItemCount());
        }

        if (start == mRange.getUpper() && end == mRange.getLower()) {
            // no change
            return;
        }

        this.mRange = Range.create(start, end);
        onRangeChange(start, end);
    }

    /**
     * This method will be called when range changes
     *
     * @param start start position of items handled by this layoutHelper
     * @param end   end position of items handled by this layoutHelper, if end < start, it will throw {@link IllegalArgumentException}
     */
    public void onRangeChange(final int start, final int end) {

    }

    /**
     * Return current range
     *
     * @return Range of integer
     */
    @NonNull
    protected final Range<Integer> getRange() {
        return mRange;
    }


    /**
     * Given a chance to check and change the chosen anchorInfo
     *
     * @param state      current {@link }RecyclerView} 's state
     * @param anchorInfo the chosen anchorInfo
     * @param helper
     */
    public void checkAnchorInfo(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {

    }

    /**
     * This method is called when scroll state is changed
     *
     * @param state The new scroll state for RecyclerView
     */
    public void onScrollStateChanged(int state, LayoutManagerHelper helper) {

    }


    /**
     * Offset all child views attached to the parent RecyclerView by dx pixels along
     * the horizontal axis.
     *
     * @param dx Pixels to offset by
     */
    public void offsetChildrenHorizontal(int dx, LayoutManagerHelper helper) {

    }

    /**
     * Offset all child views attached to the parent RecyclerView by dy pixels along
     * the vertical axis.
     *
     * @param dy Pixels to offset by
     */
    public void offsetChildrenVertical(int dy, LayoutManagerHelper helper) {

    }

    /**
     * Get zIndex of this {@link LayoutHelper}
     *
     * @return zIndex of current layoutHelper
     */
    public int getZIndex() {
        return 0;
    }


    /**
     * Tell LayoutManager whether the child can be recycled, the recycleChild range is (startIndex, endIndex)
     *
     * @param childPos   recycled child index
     * @param startIndex start index of child will be recycled
     * @param endIndex   end index of child will be recycled
     * @param helper     a helper of type {@link LayoutManagerHelper}
     * @param fromStart  whether is recycleChildren from start
     * @return whether the child in <code>childPos</code> can be recycled
     */
    public boolean isRecyclable(int childPos, int startIndex, int endIndex, LayoutManagerHelper helper, boolean fromStart) {
        return true;
    }

    /**
     * Return children count
     *
     * @return the number of children
     */
    public abstract int getItemCount();

    /**
     * Set items' count
     *
     * @param itemCount how many children in this layoutHelper
     */
    public abstract void setItemCount(int itemCount);

    public abstract void doLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                                  LayoutStateWrapper layoutState, LayoutChunkResult result,
                                  LayoutManagerHelper helper);

    public void onRefreshLayout(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {

    }

    /**
     * Called before <code>doLayout</code>
     *
     * @param recycler recycler
     * @param state    RecyclerView's State
     * @param helper   LayoutManagerHelper to handle views
     */
    public abstract void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                                      LayoutManagerHelper helper);

    /**
     * Called after <code>doLayout</code>
     *
     * @param recycler      recycler
     * @param state         RecyclerView's State
     * @param startPosition firstVisiblePosition in {@link RecyclerView}
     * @param endPosition   lastVisiblePosition in {@link RecyclerView}
     * @param scrolled      how many offset scrolled if layout is happened in a scrolling process
     * @param helper        LayoutManagerHelper
     */
    public abstract void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                                     int startPosition, int endPosition, int scrolled,
                                     LayoutManagerHelper helper);

    public void onItemsChanged(LayoutManagerHelper helper) {

    }

    /**
     * Called when this layoutHelper will be removed from LayoutManager, please release views and other resources here
     *
     * @param helper LayoutManagerHelper
     */
    public abstract void clear(LayoutManagerHelper helper);

    /**
     * Whether a background layoutView is required
     *
     * @return true if require a layoutView
     */
    public abstract boolean requireLayoutView();

    /**
     * Bind properties to <code>layoutView</code>
     *
     * @param layoutView generated layoutView as backgroundView
     */
    public abstract void bindLayoutView(View layoutView);


    /**
     * Get margins between layout when layout child at <code>offset</code>
     *
     * @param offset           anchor child's offset in current layoutHelper, for example, 0 means first item
     * @param child            anchor child view
     * @param isLayoutEnd      is the layout process will do to end or start, true means it will lay views from start to end
     * @param layoutInVertical is layout child in vertical or horizontal   @return extra margin must be calculated in {@link VirtualLayoutManager}
     * @param helper
     */
    public abstract int getExtraMargin(int offset, View child, boolean isLayoutEnd, boolean layoutInVertical, LayoutManagerHelper helper);


    public void onSaveState(final Bundle bundle) {

    }

    public void onRestoreInstanceState(final Bundle bundle) {

    }

}
