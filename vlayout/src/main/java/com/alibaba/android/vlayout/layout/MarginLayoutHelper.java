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

package com.alibaba.android.vlayout.layout;

import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.LayoutManagerHelper;

import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;

/**
 * {@link LayoutHelper} provides margin and padding supports.
 */
public abstract class MarginLayoutHelper extends LayoutHelper {

    protected int mPaddingLeft;
    protected int mPaddingRight;
    protected int mPaddingTop;
    protected int mPaddingBottom;

    protected int mMarginLeft;
    protected int mMarginRight;
    protected int mMarginTop;
    protected int mMarginBottom;


    /**
     * set paddings for this layoutHelper
     * @param leftPadding left padding
     * @param topPadding top padding
     * @param rightPadding right padding
     * @param bottomPadding bottom padding
     */
    public void setPadding(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        mPaddingLeft = leftPadding;
        mPaddingRight = rightPadding;
        mPaddingTop = topPadding;
        mPaddingBottom = bottomPadding;
    }

    /**
     * Set margins for this layoutHelper
     *
     * @param leftMargin left margin
     * @param topMargin top margin
     * @param rightMargin right margin
     * @param bottomMargin bottom margin
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
    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        return 0;
    }

    @Override
    public int computeMarginStart(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            return mMarginTop;
        } else {
            return mMarginLeft;
        }
    }

    @Override
    public int computeMarginEnd(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            return mMarginBottom;
        } else {
            return mMarginRight;
        }
    }

    @Override
    public int computePaddingStart(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            return mPaddingTop;
        } else {
            return mPaddingLeft;
        }
    }

    @Override
    public int computePaddingEnd(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            return mPaddingBottom;
        } else {
            return mPaddingRight;
        }
    }

    /**
     * Get total margin in horizontal dimension
     *
     * @return
     */
    public int getHorizontalMargin() {
        return mMarginLeft + mMarginRight;
    }

    /**
     * Get total margin in vertical dimension
     *
     * @return
     */
    public int getVerticalMargin() {
        return mMarginTop + mMarginBottom;
    }

    /**
     * Get total padding in horizontal dimension
     * @return
     */
    public int getHorizontalPadding() {
        return mPaddingLeft + mPaddingRight;
    }

    /**
     * Get total padding in vertical dimension
     * @return
     */
    public int getVerticalPadding() {
        return mPaddingTop + mPaddingBottom;
    }

    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    public int getPaddingRight() {
        return mPaddingRight;
    }

    public int getPaddingTop() {
        return mPaddingTop;
    }

    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    public int getMarginLeft() {
        return mMarginLeft;
    }

    public int getMarginRight() {
        return mMarginRight;
    }

    public int getMarginTop() {
        return mMarginTop;
    }

    public int getMarginBottom() {
        return mMarginBottom;
    }

    public void setPaddingLeft(int paddingLeft) {
        mPaddingLeft = paddingLeft;
    }

    public void setPaddingRight(int paddingRight) {
        mPaddingRight = paddingRight;
    }

    public void setPaddingTop(int paddingTop) {
        mPaddingTop = paddingTop;
    }

    public void setPaddingBottom(int paddingBottom) {
        mPaddingBottom = paddingBottom;
    }

    public void setMarginLeft(int marginLeft) {
        mMarginLeft = marginLeft;
    }

    public void setMarginRight(int marginRight) {
        mMarginRight = marginRight;
    }

    public void setMarginTop(int marginTop) {
        mMarginTop = marginTop;
    }

    public void setMarginBottom(int marginBottom) {
        mMarginBottom = marginBottom;
    }
}

