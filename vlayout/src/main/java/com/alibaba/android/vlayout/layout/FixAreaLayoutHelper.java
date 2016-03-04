package com.alibaba.android.vlayout.layout;

/**
 * LayoutHelper that will be located as fix position
 */
public abstract class FixAreaLayoutHelper extends BaseLayoutHelper {
    protected FixAreaAdjuster mAdjuster = FixAreaAdjuster.mDefaultAdjuster;

    public void setAdjuster(FixAreaAdjuster adjuster) {
        this.mAdjuster = adjuster;
    }
}
