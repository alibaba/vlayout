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

import android.support.annotation.Nullable;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * LayoutManagerHelper that provides methods for {@link LayoutHelper}
 */
public interface LayoutManagerHelper {


    /*
     * View operation helpers
     */

    /**
     * It may be cached and reused, it's layoutHelper's responsibility to make sure the properties are correct
     *
     * @return default LayoutView
     */
    View generateLayoutView();

    /**
     * Get current child count, without hidden and fixed children
     *
     * @return Total number of children in normal flow, not include fixed and hidden children
     */
    int getChildCount();

    /**
     * Get visible child
     *
     * @param index Index to get child at
     * @return Child view, null if none.
     */
    @Nullable
    View getChildAt(int index);

    /**
     * Add view to defined index
     *
     * @param view  View that added
     * @param index Index to add child at
     */
    void addChildView(View view, int index);

    /**
     * Add view to specified index, which when animation are required
     *
     * @param layoutState Current layoutState to perform animation check
     * @param view        View will be added
     * @param index       Index to add child at
     */
    void addChildView(VirtualLayoutManager.LayoutStateWrapper layoutState, View view, int index);

    /**
     * Add view to head/tail according to layoutState state
     *
     * @param layoutState current layoutState to perform animation check
     * @param view        View will be added
     */
    void addChildView(VirtualLayoutManager.LayoutStateWrapper layoutState, View view);

    /**
     * Remove view from container
     *
     * @param view View will be removed
     */
    void removeChildView(View view);

    /**
     * Tell whether the data bind to the view has updated, if true means it need rebinding
     *
     * @param view
     * @return
     */
    boolean isViewHolderUpdated(View view);

    /**
     * Add view out of normal flow, which means it won't be ignored in getChildAt, but still be able to scrolled with content
     * But it's can be find by position via {@link #findViewByPosition(int)}
     *
     * @param view View will be added
     * @param head Whether added to the head or tail
     */
    void addOffFlowView(View view, boolean head);

    /**
     * Add view out of normal flow, which means it won't be ignored in getChildAt, but still be able to scrolled with content
     * But it's can be find by position via {@link #findViewByPosition(int)}.
     * The differece between with {@link #addOffFlowView(View, boolean)} is that this method does not hide the view, it is used to add background view with overlapping.
     * @param view View will be added
     * @param head Whether added to the head or tail
     */
    void addBackgroundView(View view, boolean head);

    /**
     * Add view to fixed layer, which overlays on the normal layer.
     * It won't be found by getChildAt and also scrolled with content.
     * Can only be get by position via {@link #findViewByPosition(int)}
     *
     * @param view Fixed view
     */
    void addFixedView(View view);

    /**
     * Mark a view as hidden, it will show on the screen,
     * but can not be access via {@link RecyclerView.LayoutManager#getChildCount()} and {@link android.support.v7.widget.RecyclerView.LayoutManager#getChildAt(int)}
     *
     * @param view
     */
    void hideView(View view);

    /**
     * Mark a hidden view re-shown, so you can get it from {@link android.support.v7.widget.RecyclerView.LayoutManager#getChildAt(int)}
     *
     * @param view
     */
    void showView(View view);

    /**
     * Get {@link android.support.v7.widget.RecyclerView.ViewHolder} for a view in RecyclerView
     *
     * @param child
     * @return
     */
    RecyclerView.ViewHolder getChildViewHolder(View child);

    /**
     * Get current container recyclerView
     *
     * @return
     */
    RecyclerView getRecyclerView();

    /**
     * Find view via item position {@param position}
     *
     * @param position Position of the item that view associated with
     * @return View that found, null if not.
     */
    @Nullable
    View findViewByPosition(int position);


    /*
     * Measure and layout helpers
     */

    /**
     * MainOrientationHelper
     *
     * @return
     */
    OrientationHelperEx getMainOrientationHelper();

    /**
     * OrientationHelper in secondary direction
     *
     * @return
     */
    OrientationHelperEx getSecondaryOrientationHelper();

    /**
     * Measure children views with decorations, use this to measure children
     *
     * @param view
     * @param widthSpec
     * @param heightSpec
     */
    void measureChild(View view, int widthSpec, int heightSpec);

    /**
     * Measure children views with margins and decorations, use this to measure children
     *
     * @param child
     * @param widthUsed
     * @param heightUsed
     */
    void measureChildWithMargins(View child, int widthUsed, int heightUsed);


        /**
         * Layout children views with margins and decorations.
         *
         * @param view
         * @param left
         * @param top
         * @param right
         * @param bottom
         */
    void layoutChildWithMargins(View view, int left, int top, int right, int bottom);

    /**
     * Layout children views with decorations but without margins.
     *
     * @param view
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    void layoutChild(View view, int left, int top, int right, int bottom);

    /**
     * Quick helper to get measureSize
     *
     * @param parentSize
     * @param size
     * @param canScroll  whehter can scroll in this direction
     * @return
     */
    int getChildMeasureSpec(int parentSize, int size, boolean canScroll);

    /*
     * Properties helpers
     */

    /**
     * Find item position of a view, not the index of view in RecyclerView
     *
     * @param view
     * @return
     */
    int getPosition(View view);

    int getOrientation();

    int getPaddingTop();

    int getPaddingBottom();

    int getPaddingRight();

    int getPaddingLeft();

    int getContentWidth();

    int getContentHeight();

    boolean isDoLayoutRTL();

    boolean getReverseLayout();

    /**
     * Recycle child back to recycledPool
     *
     * @param child View will be recycled
     */
    void recycleView(View child);

    /**
     * return layout helper for specific position
     * @param position
     * @return
     */
    LayoutHelper findLayoutHelperByPosition(int position);

    /**
     * return first visibile item position in layoutManager
     * @return
     */
    int findFirstVisibleItemPosition();

    /**
     *
     * return last visibile item position in layoutManager
     * @return
     */
    int findLastVisibleItemPosition();

    /**
     * @return true to make margin between items or layout helpers overlapping, in vlayout, we support both vertical and horizontal margins overlapping between the siblings
     */
    boolean isEnableMarginOverLap();

    int getDecoratedLeft(View child);

    int getDecoratedTop(View child);

    int getDecoratedRight(View child);

    int getDecoratedBottom(View child);
}

