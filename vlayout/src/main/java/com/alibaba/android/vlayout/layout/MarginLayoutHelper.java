package com.alibaba.android.vlayout.layout;

import com.alibaba.android.vlayout.LayoutHelper;

/**
 * Created by villadora on 15/8/31.
 */
public abstract class MarginLayoutHelper extends LayoutHelper {


    protected int mMarginLeft;
    protected int mMarginRight;
    protected int mMarginTop;
    protected int mMarginBottom;


    public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        this.mMarginLeft = leftMargin;
        this.mMarginTop = topMargin;
        this.mMarginRight = rightMargin;
        this.mMarginBottom = bottomMargin;
    }

    public int getExtraMargin(int offset, boolean layoutFromEnd, boolean layoutInVertical) {
        return 0;
    }


    protected int getHorizontalMargin() {
        return mMarginLeft + mMarginRight;
    }

    protected int getVerticalMargin() {
        return mMarginTop + mMarginBottom;
    }

}

