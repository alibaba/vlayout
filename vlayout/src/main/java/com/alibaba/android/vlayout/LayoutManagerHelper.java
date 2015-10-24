package com.alibaba.android.vlayout;

import android.support.v7.widget.OrientationHelper;
import android.view.View;

/**
 * Created by villadora on 15/8/10.
 */
public interface LayoutManagerHelper {


    /*
     * View operation helpers
     */

    /**
     * It may be cached and reused, it's layoutHelper's responsibility to make sure the properties are correct
     *
     * @return default LayoutView
     */
    LayoutView generateLayoutView();

    void addChildView(VirtualLayoutManager.LayoutStateWrapper layoutState, View view, int position);

    void addChildView(VirtualLayoutManager.LayoutStateWrapper layoutState, View view);

    void removeChildView(View view);

    void addOffFlowView(View view, boolean head);

    View findViewByPosition(int position);

    View findHiddenViewByPosition(int position);


    /*
     * Measure and layout helpers
     */
    OrientationHelper getMainOrientationHelper();

    void measureChild(View view, int widthSpec, int heightSpec);

    void layoutChild(View view, int left, int top, int right, int bottom);

    int getChildMeasureSpec(int parentSize, int size, boolean canScroll);

    /*
     * Properties helpers
     */
    int getPosition(View view);

    int getOrientation();

    int getPaddingTop();

    int getPaddingBottom();

    int getPaddingRight();

    int getPaddingLeft();

    int getContentWidth();

    int getContentHeight();

    boolean isDoLayoutRTL();

    boolean getReverseLayout();

}

