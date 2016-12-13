package com.alibaba.android.vlayout.layout;

import android.view.View;
import android.view.ViewPropertyAnimator;

/**
 * LayoutHelper that will be located as fix position
 */
public abstract class FixAreaLayoutHelper extends BaseLayoutHelper {
    protected FixAreaAdjuster mAdjuster = FixAreaAdjuster.mDefaultAdjuster;

    protected FixViewAnimatorHelper mFixViewAnimatorHelper;

    public void setAdjuster(FixAreaAdjuster adjuster) {
        this.mAdjuster = adjuster;
    }

    public void setFixViewAnimatorHelper(
            FixViewAnimatorHelper fixViewAnimatorHelper) {
        mFixViewAnimatorHelper = fixViewAnimatorHelper;
    }

    public interface FixViewAnimatorHelper {

        ViewPropertyAnimator onGetFixViewAppearAnimator(View fixView);

        ViewPropertyAnimator onGetFixViewDisappearAnimator(View fixView);

    }
}
