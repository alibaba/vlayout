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

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.OrientationHelperEx;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import java.util.Arrays;

import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;

/**
 * <pre>
 * Currently support 1+6(max) layout
 *
 * Created by J!nl!n on 2017/3/7.
 *
 * 1 + 4
 * -------------------------
 * |       |       |       |
 * |       |   2   |   3   |
 * |       |       |       |
 * |   1   |-------|-------|
 * |       |       |       |
 * |       |   4   |   5   |
 * |       |       |       |
 * -------------------------
 *
 *  1 + 5
 * -------------------------
 * |           |     2     |
 * |     1     |-----------|
 * |           |     3     |
 * -------------------------
 * |       |       |       |
 * |   4   |   5   |   6   |
 * |       |       |       |
 * -------------------------
 *
 *  1 + 6
 * -------------------------
 * |       |   2   |   3   |
 * |       |-------|-------|
 * |   1   |   4   |   5   |
 * |       |-------|-------|
 * |       |   6   |   7   |
 * -------------------------
 * </pre>
 */
public class OnePlusNLayoutHelperEx extends AbstractFullFillLayoutHelper {

    private static final String TAG = "OnePlusNLayoutHelper";

    private Rect mAreaRect = new Rect();

    private View[] mChildrenViews;

    private float[] mColWeights = new float[0];

    private float mRowWeight = Float.NaN;

    public OnePlusNLayoutHelperEx() {
        setItemCount(0);
    }

    public OnePlusNLayoutHelperEx(int itemCount) {
        this(itemCount, 0, 0, 0, 0);
    }

    public OnePlusNLayoutHelperEx(int itemCount, int leftMargin, int topMargin, int rightMargin,
                                  int bottomMargin) {
        setItemCount(itemCount);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Currently, this layout supports maximum children up to 5, otherwise {@link
     * IllegalArgumentException}
     * will be thrown
     *
     * @param start start position of items handled by this layoutHelper
     * @param end   end position of items handled by this layoutHelper, if end &lt; start or end -
     *              start &gt 4, it will throw {@link IllegalArgumentException}
     */
    @Override
    public void onRangeChange(int start, int end) {
        if (end - start < 4) {
            throw new IllegalArgumentException(
                    "pls use OnePlusNLayoutHelper instead of OnePlusNLayoutHelperEx which childcount <= 5");
        }
        if (end - start > 6) {
            throw new IllegalArgumentException(
                    "OnePlusNLayoutHelper only supports maximum 7 children now");
        }
    }

    public void setColWeights(float[] weights) {
        if (weights != null) {
            this.mColWeights = Arrays.copyOf(weights, weights.length);
        } else {
            this.mColWeights = new float[0];
        }
    }

    public void setRowWeight(float weight) {
        this.mRowWeight = weight;
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                            LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        // reach the end of this layout
        final int originCurPos = layoutState.getCurrentPosition();
        if (isOutOfRange(originCurPos)) {
            return;
        }

        if (mChildrenViews == null || mChildrenViews.length != getItemCount()) {
            mChildrenViews = new View[getItemCount()];
        }

        int count = getAllChildren(mChildrenViews, recycler, layoutState, result, helper);

        if (count != getItemCount()) {
            Log.w(TAG, "The real number of children is not match with range of LayoutHelper");
        }

        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        final int parentWidth = helper.getContentWidth();
        final int parentHeight = helper.getContentHeight();
        final int parentHPadding = helper.getPaddingLeft() + helper.getPaddingRight()
                + getHorizontalMargin() + getHorizontalPadding();
        final int parentVPadding = helper.getPaddingTop() + helper.getPaddingBottom()
                + getVerticalMargin() + getVerticalPadding();

        int mainConsumed = 0;

        if (count == 5) {
            mainConsumed = handleFive(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight,
                parentHPadding, parentVPadding);
        } else if (count == 6) {
            mainConsumed = handSix(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight,
                parentHPadding, parentVPadding);
        } else if (count == 7) {
            mainConsumed = handSeven(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight,
                parentHPadding, parentVPadding);
        }

        result.mConsumed = mainConsumed;

        Arrays.fill(mChildrenViews, null);
    }


    private float getViewMainWeight(int index) {
        if (mColWeights.length > index) {
            return mColWeights[index];
        }

        return Float.NaN;
    }

    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor,
                                  LayoutManagerHelper helper) {
        if (getItemCount() == 3) {
            if (offset == 1 && isLayoutEnd) {
                Log.w(TAG, "Should not happen after adjust anchor");
                return 0;
            }
        } else if (getItemCount() == 4) {
            if (offset == 1 && isLayoutEnd) {
                return 0;
            }
        }

        if (helper.getOrientation() == VERTICAL) {
            if (isLayoutEnd) {
                return mMarginBottom + mPaddingBottom;
            } else {
                return -mMarginTop - mPaddingTop;
            }
        } else {
            if (isLayoutEnd) {
                return mMarginRight + mPaddingRight;
            } else {
                return -mMarginLeft - mPaddingLeft;
            }
        }
    }

    private int handleFive(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {
        int mainConsumed = 0;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mChildrenViews[0];
        final VirtualLayoutManager.LayoutParams lp1 = (VirtualLayoutManager.LayoutParams) child1.getLayoutParams();
        final View child2 = helper.getReverseLayout() ? mChildrenViews[4] : mChildrenViews[1];
        final VirtualLayoutManager.LayoutParams lp2 = (VirtualLayoutManager.LayoutParams) child2.getLayoutParams();
        final View child3 = helper.getReverseLayout() ? mChildrenViews[3] : mChildrenViews[2];
        final VirtualLayoutManager.LayoutParams lp3 = (VirtualLayoutManager.LayoutParams) child3.getLayoutParams();
        final View child4 = helper.getReverseLayout() ? mChildrenViews[2] : mChildrenViews[3];
        final VirtualLayoutManager.LayoutParams lp4 = (VirtualLayoutManager.LayoutParams) child4.getLayoutParams();
        final View child5 = helper.getReverseLayout() ? mChildrenViews[1] : mChildrenViews[4];
        final VirtualLayoutManager.LayoutParams lp5 = (VirtualLayoutManager.LayoutParams) child5.getLayoutParams();

        final float weight1 = getViewMainWeight(0);
        final float weight2 = getViewMainWeight(1);
        final float weight3 = getViewMainWeight(2);
        final float weight4 = getViewMainWeight(3);
        final float weight5 = getViewMainWeight(4);

        if (layoutInVertical) {

            lp2.topMargin = lp1.topMargin;
            lp3.bottomMargin = lp4.bottomMargin = lp1.bottomMargin;
            lp3.leftMargin = lp2.leftMargin;
            lp4.rightMargin = lp2.rightMargin;
            lp5.rightMargin = lp3.rightMargin;

            if (!Float.isNaN(mAspectRatio)) {
                lp1.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
            }

            int availableSpace = parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin
                - lp2.leftMargin - lp2.rightMargin
                - lp3.leftMargin - lp3.rightMargin;

            int width1 = Float.isNaN(weight1) ? (int) (availableSpace / 3.0f + 0.5f)
                : (int) (availableSpace * weight1 / 100 + 0.5f);
            int width2 = Float.isNaN(weight2) ? (availableSpace - width1) / 2
                : (int) (availableSpace * weight2 / 100 + 0.5f);
            int width3 = Float.isNaN(weight3) ? width2
                : (int) (availableSpace * weight3 / 100 + 0.5f);
            int width4 = Float.isNaN(weight4) ? width2
                : (int) (availableSpace * weight4 / 100 + 0.5f);
            int width5 = Float.isNaN(weight5) ? width2
                : (int) (availableSpace * weight5 / 100 + 0.5f);

            helper.measureChildWithMargins(child1,
                MeasureSpec.makeMeasureSpec(width1 + lp1.leftMargin + lp1.rightMargin,
                    MeasureSpec.EXACTLY),
                helper.getChildMeasureSpec(helper.getContentHeight(), lp1.height, true));

            int height1 = child1.getMeasuredHeight();
            int height2 = Float.isNaN(mRowWeight) ?
                (int) ((height1 - lp2.bottomMargin - lp3.topMargin) / 2.0f + 0.5f)
                : (int) ((height1 - lp2.bottomMargin - lp3.topMargin) * mRowWeight / 100
                    + 0.5f);
            int height3 = (height1 - lp2.bottomMargin - lp3.topMargin) - height2;

            helper.measureChildWithMargins(child2,
                MeasureSpec.makeMeasureSpec(width2 + lp2.leftMargin + lp2.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height2 + lp2.topMargin + lp2.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child3,
                MeasureSpec.makeMeasureSpec(width3 + lp3.leftMargin + lp3.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height3 + lp3.topMargin + lp3.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child4,
                MeasureSpec.makeMeasureSpec(width4 + lp4.leftMargin + lp4.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height3 + lp4.topMargin + lp4.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child5,
                MeasureSpec.makeMeasureSpec(width5 + lp5.leftMargin + lp5.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height3 + lp5.topMargin + lp5.bottomMargin,
                    MeasureSpec.EXACTLY));

            mainConsumed = Math.max(height1 + lp1.topMargin + lp1.bottomMargin,
                height2 + lp2.topMargin + lp2.bottomMargin + Math
                    .max(height3 + lp3.topMargin + lp3.bottomMargin,
                        height3 + lp4.topMargin + lp4.bottomMargin))
                + getVerticalMargin() + getVerticalPadding();

            calculateRect(mainConsumed - getVerticalMargin() - getVerticalPadding(), mAreaRect, layoutState, helper);

            int right1 = mAreaRect.left + orientationHelper
                .getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top,
                right1, mAreaRect.bottom, helper);

            int right2 = right1 + orientationHelper.getDecoratedMeasurementInOther(child2);
            layoutChildWithMargin(child2, right1, mAreaRect.top, right2,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2),
                helper);

            int right3 = right2 + orientationHelper.getDecoratedMeasurementInOther(child3);
            layoutChildWithMargin(child3, right2,
                mAreaRect.top,
                right3, mAreaRect.top + orientationHelper.getDecoratedMeasurement(child3), helper);

            int right4 = right1 + orientationHelper.getDecoratedMeasurementInOther(child4);
            layoutChildWithMargin(child4, right1,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child4),
                right4,
                mAreaRect.bottom, helper);

            layoutChildWithMargin(child5, right4,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child5),
                right4 + orientationHelper.getDecoratedMeasurementInOther(child5),
                mAreaRect.bottom, helper);
        } else {
            // TODO: horizontal support
        }

        handleStateOnResult(result, mChildrenViews);

        return mainConsumed;
    }

    private int handSix(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {

        int mainConsumed = 0;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mChildrenViews[0];
        final VirtualLayoutManager.LayoutParams lp1 = (VirtualLayoutManager.LayoutParams) child1.getLayoutParams();
        final View child2 = helper.getReverseLayout() ? mChildrenViews[5] : mChildrenViews[1];
        final VirtualLayoutManager.LayoutParams lp2 = (VirtualLayoutManager.LayoutParams) child2.getLayoutParams();
        final View child3 = helper.getReverseLayout() ? mChildrenViews[4] : mChildrenViews[2];
        final VirtualLayoutManager.LayoutParams lp3 = (VirtualLayoutManager.LayoutParams) child3.getLayoutParams();
        final View child4 = helper.getReverseLayout() ? mChildrenViews[3] : mChildrenViews[3];
        final VirtualLayoutManager.LayoutParams lp4 = (VirtualLayoutManager.LayoutParams) child4.getLayoutParams();
        final View child5 = helper.getReverseLayout() ? mChildrenViews[2] : mChildrenViews[4];
        final VirtualLayoutManager.LayoutParams lp5 = (VirtualLayoutManager.LayoutParams) child5.getLayoutParams();
        final View child6 = helper.getReverseLayout() ? mChildrenViews[1] : mChildrenViews[5];
        final VirtualLayoutManager.LayoutParams lp6 = (VirtualLayoutManager.LayoutParams) child6.getLayoutParams();

        final float weight1 = getViewMainWeight(0);
        final float weight2 = getViewMainWeight(1);
        final float weight3 = getViewMainWeight(2);
        final float weight4 = getViewMainWeight(3);
        final float weight5 = getViewMainWeight(4);
        final float weight6 = getViewMainWeight(5);

        if (layoutInVertical) {

            lp2.topMargin = lp1.topMargin;
            lp3.bottomMargin = lp4.bottomMargin = lp1.bottomMargin;
            lp3.leftMargin = lp2.leftMargin;
            lp4.rightMargin = lp2.rightMargin;
            lp5.rightMargin = lp2.rightMargin;

            if (!Float.isNaN(mAspectRatio)) {
                lp1.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
            }

            int availableSpace = parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin
                - lp2.leftMargin
                - lp2.rightMargin;

            int width1 = Float.isNaN(weight1) ?
                (int) (availableSpace / 2.0f + 0.5f)
                : (int) (availableSpace * weight1 / 100 + 0.5f);
            int width2 = Float.isNaN(weight2) ? availableSpace - width1 :
                (int) (availableSpace * weight2 / 100 + 0.5f);
            int width3 = Float.isNaN(weight3) ? width2
                : (int) (availableSpace * weight3 / 100 + 0.5);

            int bottomavailableSpace = parentWidth - parentHPadding - lp4.leftMargin - lp4.rightMargin
                - lp5.leftMargin - lp5.rightMargin
                - lp6.leftMargin - lp6.rightMargin;

            int width4 = Float.isNaN(weight4) ? (int) (bottomavailableSpace / 3.0f + 0.5f)
                : (int) (availableSpace * weight4 / 100 + 0.5f);
            int width5 = Float.isNaN(weight5) ? width4
                : (int) (availableSpace * weight5 / 100 + 0.5f);
            int width6 = Float.isNaN(weight6) ? width4
                : (int) (availableSpace * weight6 / 100 + 0.5f);

            helper.measureChildWithMargins(child1,
                MeasureSpec.makeMeasureSpec(width1 + lp1.leftMargin + lp1.rightMargin,
                    MeasureSpec.EXACTLY),
                helper.getChildMeasureSpec(helper.getContentHeight(), lp1.height, true));

            int height1 = child1.getMeasuredHeight();
            int height2 = Float.isNaN(mRowWeight) ?
                (int) ((height1 - lp2.bottomMargin - lp3.topMargin) / 2.0f + 0.5f)
                : (int) ((height1 - lp2.bottomMargin - lp3.topMargin) * mRowWeight / 100
                    + 0.5f);
            int height3 = (height1 - lp2.bottomMargin - lp3.topMargin) - height2;

            helper.measureChildWithMargins(child2,
                MeasureSpec.makeMeasureSpec(width2 + lp2.leftMargin + lp2.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height2 + lp2.topMargin + lp2.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child3,
                MeasureSpec.makeMeasureSpec(width3 + lp3.leftMargin + lp3.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height3 + lp3.topMargin + lp3.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child4,
                MeasureSpec.makeMeasureSpec(width4 + lp4.leftMargin + lp4.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height3 + lp4.topMargin + lp4.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child5,
                MeasureSpec.makeMeasureSpec(width5 + lp5.leftMargin + lp5.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height3 + lp5.topMargin + lp5.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child6,
                MeasureSpec.makeMeasureSpec(width6 + lp6.leftMargin + lp6.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height3 + lp6.topMargin + lp6.bottomMargin,
                    MeasureSpec.EXACTLY));


            int maxTopHeight = Math.max(height1 + lp1.topMargin + lp1.bottomMargin,
                (height2 + lp2.topMargin + lp2.bottomMargin) * 2);

            int maxBottomHeight = Math.max(height3 + lp4.topMargin + lp4.bottomMargin,
                Math.max(height3 + lp5.topMargin + lp5.bottomMargin,
                    height3 + lp6.topMargin + lp6.bottomMargin));

            mainConsumed = maxTopHeight + maxBottomHeight
                + getVerticalMargin() + getVerticalPadding();

            calculateRect(mainConsumed - getVerticalMargin() - getVerticalPadding(), mAreaRect, layoutState, helper);

            int right1 = mAreaRect.left + orientationHelper
                .getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top,
                right1, mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child4), helper);

            int right2 = right1 + orientationHelper.getDecoratedMeasurementInOther(child2);
            layoutChildWithMargin(child2, right1, mAreaRect.top, right2,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2),
                helper);

            int right3 = right1 + orientationHelper.getDecoratedMeasurementInOther(child3);
            layoutChildWithMargin(child3, right1,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child3),
                right3, mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child4), helper);

            int right4 = mAreaRect.left + orientationHelper.getDecoratedMeasurementInOther(child4);
            layoutChildWithMargin(child4, mAreaRect.left,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child4),
                right4,
                mAreaRect.bottom, helper);

            int right5 = right4 + orientationHelper.getDecoratedMeasurementInOther(child5);
            layoutChildWithMargin(child5, right4,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child5),
                right5,
                mAreaRect.bottom, helper);

            int right6 = right5 + orientationHelper.getDecoratedMeasurementInOther(child6);
            layoutChildWithMargin(child6, right5,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child6),
                right6,
                mAreaRect.bottom, helper);
        } else {
            // TODO: horizontal support
        }

        handleStateOnResult(result, mChildrenViews);
        return mainConsumed;
    }

    private int handSeven(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {
        int mainConsumed = 0;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mChildrenViews[0];
        final VirtualLayoutManager.LayoutParams lp1 = (VirtualLayoutManager.LayoutParams) child1.getLayoutParams();
        final View child2 = helper.getReverseLayout() ? mChildrenViews[6] : mChildrenViews[1];
        final VirtualLayoutManager.LayoutParams lp2 = (VirtualLayoutManager.LayoutParams) child2.getLayoutParams();
        final View child3 = helper.getReverseLayout() ? mChildrenViews[5] : mChildrenViews[2];
        final VirtualLayoutManager.LayoutParams lp3 = (VirtualLayoutManager.LayoutParams) child3.getLayoutParams();
        final View child4 = helper.getReverseLayout() ? mChildrenViews[4] : mChildrenViews[3];
        final VirtualLayoutManager.LayoutParams lp4 = (VirtualLayoutManager.LayoutParams) child4.getLayoutParams();
        final View child5 = helper.getReverseLayout() ? mChildrenViews[3] : mChildrenViews[4];
        final VirtualLayoutManager.LayoutParams lp5 = (VirtualLayoutManager.LayoutParams) child5.getLayoutParams();
        final View child6 = helper.getReverseLayout() ? mChildrenViews[2] : mChildrenViews[5];
        final VirtualLayoutManager.LayoutParams lp6 = (VirtualLayoutManager.LayoutParams) child6.getLayoutParams();
        final View child7 = helper.getReverseLayout() ? mChildrenViews[1] : mChildrenViews[6];
        final VirtualLayoutManager.LayoutParams lp7 = (VirtualLayoutManager.LayoutParams) child7.getLayoutParams();

        final float weight1 = getViewMainWeight(0);
        final float weight2 = getViewMainWeight(1);
        final float weight3 = getViewMainWeight(2);
        final float weight4 = getViewMainWeight(3);
        final float weight5 = getViewMainWeight(4);
        final float weight6 = getViewMainWeight(5);
        final float weight7 = getViewMainWeight(6);

        if (layoutInVertical) {

            if (!Float.isNaN(mAspectRatio)) {
                lp1.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
            }

            int availableSpace = parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin
                - lp2.leftMargin - lp2.rightMargin
                - lp3.leftMargin - lp3.rightMargin;

            int width1 = Float.isNaN(weight1) ?
                (int) (availableSpace / 3.0f + 0.5f)
                : (int) (availableSpace * weight1 / 100 + 0.5f);
            int width2 = Float.isNaN(weight2) ? (availableSpace - width1) / 2 :
                (int) (availableSpace * weight2 / 100 + 0.5f);
            int width3 = Float.isNaN(weight3) ? width2
                : (int) (availableSpace * weight3 / 100 + 0.5);

            int width4 = Float.isNaN(weight4) ? width2
                : (int) (availableSpace * weight4 / 100 + 0.5f);
            int width5 = Float.isNaN(weight5) ? width2
                : (int) (availableSpace * weight5 / 100 + 0.5f);
            int width6 = Float.isNaN(weight6) ? width2
                : (int) (availableSpace * weight6 / 100 + 0.5f);
            int width7 = Float.isNaN(weight6) ? width2
                : (int) (availableSpace * weight7 / 100 + 0.5f);

            helper.measureChildWithMargins(child1,
                MeasureSpec.makeMeasureSpec(width1 + lp1.leftMargin + lp1.rightMargin,
                    MeasureSpec.EXACTLY),
                helper.getChildMeasureSpec(helper.getContentHeight(), lp1.height, true));

            int height1 = child1.getMeasuredHeight();
            int height2 = Float.isNaN(mRowWeight) ?
                (int) ((height1 - lp2.bottomMargin - lp3.topMargin) / 3.0f + 0.5f)
                : (int) ((height1 - lp2.bottomMargin - lp3.topMargin) * mRowWeight / 100
                    + 0.5f);

            helper.measureChildWithMargins(child2,
                MeasureSpec.makeMeasureSpec(width2 + lp2.leftMargin + lp2.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height2 + lp2.topMargin + lp2.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child3,
                MeasureSpec.makeMeasureSpec(width3 + lp3.leftMargin + lp3.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height2 + lp3.topMargin + lp3.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child4,
                MeasureSpec.makeMeasureSpec(width4 + lp4.leftMargin + lp4.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height2 + lp4.topMargin + lp4.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child5,
                MeasureSpec.makeMeasureSpec(width5 + lp5.leftMargin + lp5.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height2 + lp5.topMargin + lp5.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child6,
                MeasureSpec.makeMeasureSpec(width6 + lp6.leftMargin + lp6.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height2 + lp6.topMargin + lp6.bottomMargin,
                    MeasureSpec.EXACTLY));

            helper.measureChildWithMargins(child7,
                MeasureSpec.makeMeasureSpec(width7 + lp7.leftMargin + lp7.rightMargin,
                    MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height2 + lp7.topMargin + lp7.bottomMargin,
                    MeasureSpec.EXACTLY));


            int maxRightHeight =
                Math.max(height2 + lp2.topMargin + lp2.bottomMargin, height2 + lp3.topMargin + lp3.bottomMargin) +
                    Math.max(height2 + lp4.topMargin + lp4.bottomMargin, height2 + lp5.topMargin + lp5.bottomMargin) +
                    Math.max(height2 + lp6.topMargin + lp6.bottomMargin, height2 + lp7.topMargin + lp7.bottomMargin);

            int maxHeight = Math.max(height1 + lp1.topMargin + lp1.bottomMargin, maxRightHeight);

            mainConsumed = maxHeight
                + getVerticalMargin() + getVerticalPadding();

            calculateRect(mainConsumed - getVerticalMargin() - getVerticalPadding(), mAreaRect, layoutState, helper);

            int right1 = mAreaRect.left + orientationHelper.getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top, right1, mAreaRect.bottom, helper);

            int right2 = right1 + orientationHelper.getDecoratedMeasurementInOther(child2);
            layoutChildWithMargin(child2, right1,
                mAreaRect.top,
                right2,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2), helper);

            int right3 = right2 + orientationHelper.getDecoratedMeasurementInOther(child3);
            layoutChildWithMargin(child3, right2, mAreaRect.top, right3,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child3), helper);

            int right4 = right1 + orientationHelper.getDecoratedMeasurementInOther(child4);
            layoutChildWithMargin(child4, right1,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2),
                right4,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child6), helper);

            int right5 = right4 + orientationHelper.getDecoratedMeasurementInOther(child5);
            layoutChildWithMargin(child5, right4,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2),
                right5,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child7), helper);

            int right6 = right1 + orientationHelper.getDecoratedMeasurementInOther(child6);
            layoutChildWithMargin(child6, right1,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child6),
                right6,
                mAreaRect.bottom, helper);

            layoutChildWithMargin(child7, right6,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child7),
                right6 + orientationHelper.getDecoratedMeasurementInOther(child7),
                mAreaRect.bottom, helper);
        } else {
            // TODO: horizontal support
        }

        handleStateOnResult(result, mChildrenViews);
        return mainConsumed;
    }
}
