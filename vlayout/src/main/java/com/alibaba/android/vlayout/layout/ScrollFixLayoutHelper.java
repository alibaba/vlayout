package com.alibaba.android.vlayout.layout;

import android.view.View;

import com.alibaba.android.vlayout.LayoutManagerHelper;

/**
 * Absolute layout which only shows after scrolling to its' position,
 * it'll layout View based on leftMargin/topMargin/rightMargin/bottomMargin.
 *
 * @author villadora
 * @since 1.0.0
 */
public class ScrollFixLayoutHelper extends FixLayoutHelper {

    private static final String TAG = "ScrollFixLayoutHelper";


    public ScrollFixLayoutHelper() {
        this(TOP_LEFT);
    }

    public ScrollFixLayoutHelper(int alignType) {
        super(alignType);
    }


    @Override
    protected boolean shouldBeDraw(int startPosition, int endPosition, int scrolled) {
        return endPosition >= getRange().getLower() - 1;
    }

    @Override
    public int getExtraMargin(int offset, View child, boolean isLayoutEnd, boolean layoutInVertical, LayoutManagerHelper helper) {
        if (layoutInVertical) {
            if (isLayoutEnd) {
                return mMarginBottom;
            } else {
                return -mMarginTop;
            }
        } else {
            if (isLayoutEnd) {
                return mMarginRight;
            } else {
                return -mMarginLeft;
            }
        }
    }
}
