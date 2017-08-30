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

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.alibaba.android.vlayout.layout.LayoutChunkResult;

import java.util.LinkedList;
import java.util.List;

import static com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

/**
 * Helper class to handle different layouts in {@link VirtualLayoutManager}
 *
 * @author villadora
 * @date 2015-8-14
 * @since 1.0.0
 */

public abstract class LayoutHelper {

    public static final Range<Integer> RANGE_ALL = Range.create(Integer.MIN_VALUE, Integer.MAX_VALUE);
    public static final Range<Integer> RANGE_EMPTY = Range.create(-1, -1);

    /**
     * Range for this layoutHelper, intialize with EMPTY
     */
    @NonNull
    Range<Integer> mRange = RANGE_EMPTY;

    int mZIndex = 0;


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
    public void setRange(int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end should be larger or equeal then start position");
        }

        if (start == -1 && end == -1) {
            this.mRange = RANGE_EMPTY;
            onRangeChange(start, end);
            return;
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
    public final Range<Integer> getRange() {
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
     * @param state         The new scroll state for RecyclerView
     * @param startPosition
     * @param endPosition
     */
    public void onScrollStateChanged(int state, int startPosition, int endPosition, LayoutManagerHelper helper) {

    }


    /**
     * Offset all child views attached to the parent RecyclerView by dx pixels along
     * the horizontal axis.
     *
     * @param dx Pixels to offset by
     */
    public void onOffsetChildrenHorizontal(int dx, LayoutManagerHelper helper) {

    }

    /**
     * Offset all child views attached to the parent RecyclerView by dy pixels along
     * the vertical axis.
     *
     * @param dy Pixels to offset by
     */
    public void onOffsetChildrenVertical(int dy, LayoutManagerHelper helper) {

    }

    /**
     * Get zIndex of this {@link LayoutHelper}
     *
     * @return zIndex of current layoutHelper
     */
    public int getZIndex() {
        return mZIndex;
    }

    /**
     * Experimental attribute, set zIndex of this {@link LayoutHelper}，it does not mean the z-index of view. It just reorder the layoutHelpers in linear flow.
     * Do not use it currently.
     * @param zIndex
     */
    public void setZIndex(int zIndex) {
        this.mZIndex = zIndex;
    }

    /**
     * Get View that fixed in some position
     *
     * @return
     */
    @Nullable
    public View getFixedView() {
        return null;
    }


    @NonNull
    protected final List<View> mOffFlowViews = new LinkedList<>();

    /**
     * Get Views that out of normal flow layout
     *
     * @return list of views
     */
    @NonNull
    public List<View> getOffFlowViews() {
        return mOffFlowViews;
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

    /**
     * Run to adjust layoutHelper's background area
     * @param startPosition
     * @param endPosition
     * @param helper
     */
    public abstract void adjustLayout(int startPosition, int endPosition, LayoutManagerHelper helper);

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

    public abstract boolean isFixLayout();

    /**
     * Get margins between layout when layout child at <code>offset</code>
     * Or compute offset for align line during scrolling
     *
     * @param offset      anchor child's offset in current layoutHelper, for example, 0 means first item
     * @param isLayoutEnd is the layout process will do to end or start, true means it will lay views from start to end
     * @param useAnchor   whether offset is computed for scrolling or for anchor reset
     * @param helper      view layout helper
     * @return extra offset must be calculated in {@link VirtualLayoutManager}
     */
    public abstract int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor,
        LayoutManagerHelper helper);

    public abstract int computeMarginStart(int offset, boolean isLayoutEnd, boolean useAnchor,
        LayoutManagerHelper helper);

    public abstract int computeMarginEnd(int offset, boolean isLayoutEnd, boolean useAnchor,
        LayoutManagerHelper helper);

    public abstract int computePaddingStart(int offset, boolean isLayoutEnd, boolean useAnchor,
        LayoutManagerHelper helper);

    public abstract int computePaddingEnd(int offset, boolean isLayoutEnd, boolean useAnchor,
        LayoutManagerHelper helper);

    public void onSaveState(final Bundle bundle) {

    }

    public void onRestoreInstanceState(final Bundle bundle) {

    }

}
