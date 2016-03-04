package com.alibaba.android.vlayout.layout;

/**
 *
 */
public class FixAreaAdjuster {

    public final int left;
    public final int top;
    public final int right;
    public final int bottom;

    public static final FixAreaAdjuster mDefaultAdjuster = new FixAreaAdjuster(0, 0, 0, 0);

    public FixAreaAdjuster(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;

    }
}
