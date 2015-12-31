package com.alibaba.android.vlayout;

import android.support.annotation.Nullable;
import android.support.v7.widget.OrientationHelper;
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
     * Add view out of normal flow, which means it won't be ignored in getChildAt, but still be able to scrolled with content
     * But it's can be find by position via {@link #findViewByPosition(int)}
     *
     * @param view View will be added
     * @param head Whether added to the head or tail
     */
    void addOffFlowView(View view, boolean head);

    /**
     * Add view to fixed layer, which overlays on the normal layer.
     * It won't be found by getChildAt and also scrolled with content.
     * Can only be get by position via {@link #findViewByPosition(int)}
     *
     * @param view Fixed view
     */
    void addFixedView(View view);

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
    OrientationHelper getMainOrientationHelper();

    /**
     * OrientationHelper in secondary direction
     *
     * @return
     */
    OrientationHelper getSecondaryOrientationHelper();

    /**
     * Measure children views with margins and decorations, use this to measure children
     *
     * @param view
     * @param widthSpec
     * @param heightSpec
     */
    void measureChild(View view, int widthSpec, int heightSpec);

    /**
     * Layout children views with margins and decorations.
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

}

