package com.alibaba.android.vlayout.extend;

/**
 * Object save layout result for one pass
 * 记录每次布局循环后的结果
 */
public class LayoutChunkResult {
    // how many pixels that consumed by this pass
    public int mConsumed;

    // whether reach the end item
    public boolean mFinished;

    // should ignore consumes this time
    public boolean mIgnoreConsumed;

    // should the items in this pass be focused
    public boolean mFocusable;

    // reset state
    public void resetInternal() {
        mConsumed = 0;
        mFinished = false;
        mIgnoreConsumed = false;
        mFocusable = false;
    }
}
