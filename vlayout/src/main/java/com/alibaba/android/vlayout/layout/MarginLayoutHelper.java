package com.alibaba.android.vlayout.layout;

import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.LayoutManagerHelper;

/**
 * {@link LayoutHelper} provides margin supports.
 */
public abstract class MarginLayoutHelper extends LayoutHelper {

    protected int mPadddingLeft;
    protected int mPadddingRight;
    protected int mPadddingTop;
    protected int mPadddingBottom;

    protected int mMarginLeft;
    protected int mMarginRight;
    protected int mMarginTop;
    protected int mMarginBottom;


    /**
     * set paddings for this layoutHelper
     * @param leftPadding
     * @param topPadding
     * @param rightPadding
     * @param bottomPadding
     */
    public void setPadding(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        mPadddingLeft = leftPadding;
        mPadddingRight = rightPadding;
        mPadddingTop = topPadding;
        mPadddingBottom = bottomPadding;
    }

    /**
     * Set margins for this layoutHelper
     *
     * @param leftMargin
     * @param topMargin
     * @param rightMargin
     * @param bottomMargin
     */
    public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        this.mMarginLeft = leftMargin;
        this.mMarginTop = topMargin;
        this.mMarginRight = rightMargin;
        this.mMarginBottom = bottomMargin;
    }

    /**
     * Calculate align offset when start a new layout with anchor views
     *
     * @param offset      anchor child's offset in current layoutHelper, for example, 0 means first item
     * @param isLayoutEnd is the layout process will do to end or start, true means it will lay views from start to end
     * @param useAnchor   whether offset is computed for scrolling or for anchor reset
     * @param helper      view layout helper
     * @return pixel offset to start to the anchor view
     */
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        return 0;
    }

    /**
     * Get total margin in horizontal dimension
     *
     * @return
     */
    protected int getHorizontalMargin() {
        return mMarginLeft + mMarginRight;
    }

    /**
     * Get total margin in vertical dimension
     *
     * @return
     */
    protected int getVerticalMargin() {
        return mMarginTop + mMarginBottom;
    }

    /**
     * Get total padding in horizontal dimension
     * @return
     */
    protected int getHorizontalPadding() {
        return mPadddingLeft + mPadddingRight;
    }

    /**
     * Get total padding in vertical dimension
     * @return
     */
    protected int getVerticalPadding() {
        return mPadddingTop + mPadddingBottom;
    }

}

