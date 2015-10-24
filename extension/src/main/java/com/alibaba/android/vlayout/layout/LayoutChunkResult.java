package com.alibaba.android.vlayout.layout;

/**
 * Created by villadora on 15/8/10.
 */
public class LayoutChunkResult {
    public int mConsumed;
    public boolean mFinished;
    public boolean mIgnoreConsumed;
    public boolean mFocusable;

    public void resetInternal() {
        mConsumed = 0;
        mFinished = false;
        mIgnoreConsumed = false;
        mFocusable = false;
    }
}
