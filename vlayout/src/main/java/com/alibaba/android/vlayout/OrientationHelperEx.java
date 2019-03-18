package com.alibaba.android.vlayout;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by longerian on 2017/6/6.
 *
 * @author longerian
 * @date 2017/06/06
 */

abstract public class OrientationHelperEx {

    private static final int INVALID_SIZE = Integer.MIN_VALUE;

    protected final ExposeLinearLayoutManagerEx mLayoutManager;

    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;

    public static final int VERTICAL = LinearLayout.VERTICAL;

    private int mLastTotalSpace = INVALID_SIZE;

    private OrientationHelperEx(ExposeLinearLayoutManagerEx layoutManager) {
        mLayoutManager = layoutManager;
    }

    /**
     * Call this method after onLayout method is complete if state is NOT pre-layout.
     * This method records information like layout bounds that might be useful in the next layout
     * calculations.
     */
    public void onLayoutComplete() {
        mLastTotalSpace = getTotalSpace();
    }

    /**
     * Returns the layout space change between the previous layout pass and current layout pass.
     * <p>
     * Make sure you call {@link #onLayoutComplete()} at the end of your LayoutManager's
     * {@link RecyclerView.LayoutManager#onLayoutChildren(RecyclerView.Recycler,
     * RecyclerView.State)} method.
     *
     * @return The difference between the current total space and previous layout's total space.
     * @see #onLayoutComplete()
     */
    public int getTotalSpaceChange() {
        return INVALID_SIZE == mLastTotalSpace ? 0 : getTotalSpace() - mLastTotalSpace;
    }

    /**
     * Returns the start of the view including its decoration and margin.
     * <p>
     * For example, for the horizontal helper, if a View's left is at pixel 20, has 2px left
     * decoration and 3px left margin, returned value will be 15px.
     *
     * @param view The view element to check
     * @return The first pixel of the element
     * @see #getDecoratedEnd(android.view.View)
     */
    public abstract int getDecoratedStart(View view);

    /**
     * Returns the end of the view including its decoration and margin.
     * <p>
     * For example, for the horizontal helper, if a View's right is at pixel 200, has 2px right
     * decoration and 3px right margin, returned value will be 205.
     *
     * @param view The view element to check
     * @return The last pixel of the element
     * @see #getDecoratedStart(android.view.View)
     */
    public abstract int getDecoratedEnd(View view);

    /**
     * Returns the space occupied by this View in the current orientation including decorations and
     * margins.
     *
     * @param view The view element to check
     * @return Total space occupied by this view
     * @see #getDecoratedMeasurementInOther(View)
     */
    public abstract int getDecoratedMeasurement(View view);

    /**
     * Returns the space occupied by this View in the perpendicular orientation including
     * decorations and margins.
     *
     * @param view The view element to check
     * @return Total space occupied by this view in the perpendicular orientation to current one
     * @see #getDecoratedMeasurement(View)
     */
    public abstract int getDecoratedMeasurementInOther(View view);

    /**
     * Returns the start position of the layout after the start padding is added.
     *
     * @return The very first pixel we can draw.
     */
    public abstract int getStartAfterPadding();

    /**
     * Returns the end position of the layout after the end padding is removed.
     *
     * @return The end boundary for this layout.
     */
    public abstract int getEndAfterPadding();

    /**
     * Returns the end position of the layout without taking padding into account.
     *
     * @return The end boundary for this layout without considering padding.
     */
    public abstract int getEnd();

    /**
     * Offsets all children's positions by the given amount.
     *
     * @param amount Value to add to each child's layout parameters
     */
    public abstract void offsetChildren(int amount);

    /**
     * Returns the total space to layout. This number is the difference between
     * {@link #getEndAfterPadding()} and {@link #getStartAfterPadding()}.
     *
     * @return Total space to layout children
     */
    public abstract int getTotalSpace();

    /**
     * Offsets the child in this orientation.
     *
     * @param view   View to offset
     * @param offset offset amount
     */
    public abstract void offsetChild(View view, int offset);

    /**
     * Returns the padding at the end of the layout. For horizontal helper, this is the right
     * padding and for vertical helper, this is the bottom padding. This method does not check
     * whether the layout is RTL or not.
     *
     * @return The padding at the end of the layout.
     */
    public abstract int getEndPadding();

    /**
     * Creates an OrientationHelper for the given LayoutManager and orientation.
     *
     * @param layoutManager LayoutManager to attach to
     * @param orientation   Desired orientation. Should be {@link #HORIZONTAL} or {@link #VERTICAL}
     * @return A new OrientationHelper
     */
    public static OrientationHelperEx createOrientationHelper(
        ExposeLinearLayoutManagerEx layoutManager, int orientation) {
        switch (orientation) {
            case HORIZONTAL:
                return createHorizontalHelper(layoutManager);
            case VERTICAL:
                return createVerticalHelper(layoutManager);
        }
        throw new IllegalArgumentException("invalid orientation");
    }

    /**
     * Creates a horizontal OrientationHelper for the given LayoutManager.
     *
     * @param layoutManager The LayoutManager to attach to.
     * @return A new OrientationHelper
     */
    public static OrientationHelperEx createHorizontalHelper(
        ExposeLinearLayoutManagerEx layoutManager) {
        return new OrientationHelperEx(layoutManager) {
            @Override
            public int getEndAfterPadding() {
                return mLayoutManager.getWidth() - mLayoutManager.getPaddingRight();
            }

            @Override
            public int getEnd() {
                return mLayoutManager.getWidth();
            }

            @Override
            public void offsetChildren(int amount) {
                mLayoutManager.offsetChildrenHorizontal(amount);
            }

            @Override
            public int getStartAfterPadding() {
                return mLayoutManager.getPaddingLeft();
            }

            @Override
            public int getDecoratedMeasurement(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedMeasuredWidth(view) + params.leftMargin
                    + params.rightMargin : mLayoutManager.getDecoratedMeasuredWidth(view);
            }

            @Override
            public int getDecoratedMeasurementInOther(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
                return mLayoutManager.getDecoratedMeasuredHeight(view) + params.topMargin + params.bottomMargin;
            }

            @Override
            public int getDecoratedEnd(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedRight(view)
                    + params.rightMargin : mLayoutManager.getDecoratedRight(view);
            }

            @Override
            public int getDecoratedStart(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedLeft(view)
                    - params.leftMargin : mLayoutManager.getDecoratedLeft(view);
            }

            @Override
            public int getTotalSpace() {
                return mLayoutManager.getWidth() - mLayoutManager.getPaddingLeft()
                    - mLayoutManager.getPaddingRight();
            }

            @Override
            public void offsetChild(View view, int offset) {
                view.offsetLeftAndRight(offset);
            }

            @Override
            public int getEndPadding() {
                return mLayoutManager.getPaddingRight();
            }
        };
    }

    /**
     * Creates a vertical OrientationHelper for the given LayoutManager.
     *
     * @param layoutManager The LayoutManager to attach to.
     * @return A new OrientationHelper
     */
    public static OrientationHelperEx createVerticalHelper(ExposeLinearLayoutManagerEx layoutManager) {
        return new OrientationHelperEx(layoutManager) {
            @Override
            public int getEndAfterPadding() {
                return mLayoutManager.getHeight() - mLayoutManager.getPaddingBottom();
            }

            @Override
            public int getEnd() {
                return mLayoutManager.getHeight();
            }

            @Override
            public void offsetChildren(int amount) {
                mLayoutManager.offsetChildrenVertical(amount);
            }

            @Override
            public int getStartAfterPadding() {
                return mLayoutManager.getPaddingTop();
            }

            @Override
            public int getDecoratedMeasurement(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedMeasuredHeight(view) + params.topMargin
                    + params.bottomMargin : mLayoutManager.getDecoratedMeasuredHeight(view);
            }

            @Override
            public int getDecoratedMeasurementInOther(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
                return mLayoutManager.getDecoratedMeasuredWidth(view) + params.leftMargin + params.rightMargin ;
            }

            @Override
            public int getDecoratedEnd(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedBottom(view)
                    + params.bottomMargin : mLayoutManager.getDecoratedBottom(view);
            }

            @Override
            public int getDecoratedStart(View view) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
                return !mLayoutManager.isEnableMarginOverLap() ? mLayoutManager.getDecoratedTop(view) - params.topMargin
                    : mLayoutManager.getDecoratedTop(view);
            }

            @Override
            public int getTotalSpace() {
                return mLayoutManager.getHeight() - mLayoutManager.getPaddingTop()
                    - mLayoutManager.getPaddingBottom();
            }

            @Override
            public void offsetChild(View view, int offset) {
                view.offsetTopAndBottom(offset);
            }

            @Override
            public int getEndPadding() {
                return mLayoutManager.getPaddingBottom();
            }
        };
    }
}
