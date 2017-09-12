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
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.OrientationHelperEx;
import com.alibaba.android.vlayout.Range;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import java.util.Arrays;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;

/**
 * <pre>
 * Currently support 1+3(max) layout
 * 1 + 0
 * -------------------------
 * |                       |
 * |                       |
 * |           1           |
 * |                       |
 * |                       |
 * |                       |
 * -------------------------
 *
 * 1 + 1
 * -------------------------
 * |           |           |
 * |           |           |
 * |           |           |
 * |     1     |     2     |
 * |           |           |
 * |           |           |
 * |           |           |
 * -------------------------
 *
 * 1 + 2
 * -------------------------
 * |           |           |
 * |           |     2     |
 * |           |           |
 * |     1     |-----------|
 * |           |           |
 * |           |     3     |
 * |           |           |
 * -------------------------
 *
 * 1 + 3
 * -------------------------
 * |           |           |
 * |           |     2     |
 * |           |           |
 * |     1     |-----------|
 * |           |     |     |
 * |           |  3  |  4  |
 * |           |     |     |
 * -------------------------
 *  1 + 4
 * -------------------------
 * |           |           |
 * |           |     2     |
 * |           |           |
 * |     1     |-----------|
 * |           |   |   |   |
 * |           | 3 | 4 | 5 |
 * |           |   |   |   |
 * -------------------------
 * </pre>
 *
 * @author villadora
 * @since 1.0.0
 */
public class OnePlusNLayoutHelper extends AbstractFullFillLayoutHelper {

    private static final String TAG = "OnePlusNLayoutHelper";


    private Rect mAreaRect = new Rect();

    private View[] mChildrenViews;

    private View mHeader;

    private View mFooter;

    private View[] mContentViews;

    private float[] mColWeights = new float[0];

    private float mRowWeight = Float.NaN;

    private boolean hasHeader = false;

    private boolean hasFooter = false;

    public OnePlusNLayoutHelper() {
        setItemCount(0);
    }

    public OnePlusNLayoutHelper(int itemCount) {
        this(itemCount, 0, 0, 0, 0);
    }

    public OnePlusNLayoutHelper(int itemCount, int leftMargin, int topMargin, int rightMargin,
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
     *              start &gt 6, it will throw {@link IllegalArgumentException}
     */
    @Override
    public void onRangeChange(int start, int end) {
        if (end - start > 6) {
            throw new IllegalArgumentException(
                    "OnePlusNLayoutHelper only supports maximum 7 children now");
        }
    }

    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    public void setHasFooter(boolean hasFooter) {
        this.hasFooter = hasFooter;
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
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        final int originCurPos = layoutState.getCurrentPosition();

        if (mChildrenViews == null || mChildrenViews.length != getItemCount()) {
            mChildrenViews = new View[getItemCount()];
        }

        int count = getAllChildren(mChildrenViews, recycler, layoutState, result, helper);
        int contentCount = count;
        if (hasHeader) {
            contentCount = contentCount - 1;
            mHeader = mChildrenViews[0];
            mChildrenViews[0] = null;
        }
        if (hasFooter) {
            contentCount = contentCount - 1;
            mFooter = mChildrenViews[count - 1];
            mChildrenViews[count - 1] = null;
        }
        if (mContentViews == null || mContentViews.length != contentCount) {
            mContentViews = new View[contentCount];
        }
        int j = 0;
        for (int i = 0; i < mChildrenViews.length; i++) {
            if (mChildrenViews[i] == null) {
                continue;
            }
            mContentViews[j] = mChildrenViews[i];
            j++;
        }

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
        int headerConsumed = handleHeader(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight,
            parentHPadding, parentVPadding);
        int footerConsumed = handleFooter(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight,
            parentHPadding, parentVPadding);
        if (contentCount == 1) {
            mainConsumed = handleOne(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding, headerConsumed, footerConsumed);
        } else if (contentCount == 2) {
            mainConsumed = handleTwo(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding, headerConsumed, footerConsumed);
        } else if (contentCount == 3) {
            mainConsumed = handleThree(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding, headerConsumed, footerConsumed);
        } else if (contentCount == 4) {
            mainConsumed = handleFour(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding, headerConsumed, footerConsumed);
        } else if (contentCount == 5) {
            mainConsumed = handleFive(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding, headerConsumed, footerConsumed);
        }

        result.mConsumed = mainConsumed;

        Arrays.fill(mChildrenViews, null);
        Arrays.fill(mContentViews, null);
        //mHeader = null;
        //mFooter = null;
    }


    private float getViewMainWeight(ViewGroup.MarginLayoutParams params, int index) {
        if (mColWeights.length > index) {
            return mColWeights[index];
        }

        return Float.NaN;
    }

    @Override
    protected void onClear(LayoutManagerHelper helper) {
        super.onClear(helper);
        mHeader = null;
        mFooter = null;
    }

    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor,
            LayoutManagerHelper helper) {
        //Log.d("Longer", "range " + getRange() + " offset " + offset + " isLayoutEnd " + isLayoutEnd + " useAnchor " + useAnchor + " mHeader " + (mHeader != null ? mHeader.hashCode() : "null"));
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();
        if (offset == 1 && isLayoutEnd) {
            Log.w(TAG, "Should not happen after adjust anchor without header");
            return 0;
        } else if (useAnchor) {
            Log.w(TAG, "Happens when header scroll out of window and layoutManager use first content view as anchor to layout");
            return -mMarginTop - mPaddingTop - (mHeader != null ? orientationHelper.getDecoratedMeasurement(mHeader) : 0);
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

    private int handleHeader(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {
        if (mHeader == null) {
            return 0;
        }
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            mHeader.getLayoutParams());

        // fill width
        int widthSpec = helper.getChildMeasureSpec(parentWidth - parentHPadding,
            layoutInVertical ? MATCH_PARENT : lp.width, !layoutInVertical);
        int heightSpec = helper.getChildMeasureSpec(parentHeight - parentVPadding,
            layoutInVertical ? lp.height : MeasureSpec.EXACTLY, layoutInVertical);
        helper.measureChildWithMargins(mHeader, widthSpec, heightSpec);
        return orientationHelper.getDecoratedMeasurement(mHeader);
    }

    private int handleFooter(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {
        if (mFooter == null) {
            return 0;
        }

        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            mFooter.getLayoutParams());

        // fill width
        int widthSpec = helper.getChildMeasureSpec(parentWidth - parentHPadding,
            layoutInVertical ? MATCH_PARENT : lp.width, !layoutInVertical);
        int heightSpec = helper.getChildMeasureSpec(parentHeight - parentVPadding,
            layoutInVertical ? lp.height : MeasureSpec.EXACTLY, layoutInVertical);
        helper.measureChildWithMargins(mFooter, widthSpec, heightSpec);
        return orientationHelper.getDecoratedMeasurement(mFooter);
    }

    private int handleOne(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding,
        int headerConsumed, int footerConsumed) {
        int mainConsumed = headerConsumed + footerConsumed;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        View view = mContentViews[0];
        final ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            view.getLayoutParams());

        if (!Float.isNaN(mAspectRatio)) {
            if (layoutInVertical) {
                lp.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
            } else {
                lp.width = (int) ((parentHeight - parentVPadding) * mAspectRatio);
            }
        }

        final float weight = getViewMainWeight(lp, 0);

        // fill width
        int widthSpec = helper.getChildMeasureSpec(
            Float.isNaN(weight) ? (parentWidth - parentHPadding)
                : (int) ((parentWidth - parentHPadding) * weight),
            layoutInVertical ? MATCH_PARENT : lp.width, !layoutInVertical);
        int heightSpec = helper.getChildMeasureSpec(parentHeight - parentVPadding,
            layoutInVertical ? lp.height : MeasureSpec.EXACTLY, layoutInVertical);

        helper.measureChildWithMargins(view, widthSpec, heightSpec);

        mainConsumed += orientationHelper.getDecoratedMeasurement(view) + (layoutInVertical ?
            getVerticalMargin() + getVerticalPadding()
            : getHorizontalMargin() + getHorizontalPadding());

        calculateRect(mainConsumed - getVerticalMargin() - getVerticalPadding(), mAreaRect, layoutState, helper);

        if (mHeader != null) {
            layoutChildWithMargin(mHeader, mAreaRect.left, mAreaRect.top, mAreaRect.right, mAreaRect.top + headerConsumed, helper);
        }
        layoutChildWithMargin(view, mAreaRect.left, mAreaRect.top + headerConsumed, mAreaRect.right, mAreaRect.bottom - footerConsumed,
            helper);
        if (mFooter != null) {
            layoutChildWithMargin(mFooter, mAreaRect.left, mAreaRect.bottom - footerConsumed, mAreaRect.right, mAreaRect.bottom, helper);
        }
        handleStateOnResult(result, mHeader, view, mFooter);
        return mainConsumed;
    }

    private int handleTwo(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding,
        int headerConsumed, int footerConsumed) {
        int mainConsumed = headerConsumed + footerConsumed;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mContentViews[0];
        final ViewGroup.MarginLayoutParams lp1 = new ViewGroup.MarginLayoutParams(
            child1.getLayoutParams());
        final View child2 = mContentViews[1];
        final ViewGroup.MarginLayoutParams lp2 = new ViewGroup.MarginLayoutParams(
            child2.getLayoutParams());
        final float weight1 = getViewMainWeight(lp1, 0);
        final float weight2 = getViewMainWeight(lp1, 1);


        if (layoutInVertical) {

            if (!Float.isNaN(mAspectRatio)) {
                lp1.height = lp2.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
            }

            lp2.topMargin = lp1.topMargin;
            lp2.bottomMargin = lp1.bottomMargin;

            int availableSpace = parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin
                - lp2.leftMargin - lp2.rightMargin;
            int width1 = Float.isNaN(weight1) ? (int) (availableSpace / 2.0f + 0.5f)
                : (int) (availableSpace * weight1 / 100 + 0.5f);
            int width2 = Float.isNaN(weight2) ? (availableSpace - width1)
                : (int) (availableSpace * weight2 / 100 + 0.5f);

            helper.measureChildWithMargins(child1,
                MeasureSpec.makeMeasureSpec(width1 + lp1.leftMargin + lp1.rightMargin,
                    MeasureSpec.EXACTLY),
                helper.getChildMeasureSpec(helper.getContentHeight(), lp1.height, true));

            helper.measureChildWithMargins(child2,
                MeasureSpec.makeMeasureSpec(width2 + lp2.leftMargin + lp2.rightMargin,
                    MeasureSpec.EXACTLY),
                helper.getChildMeasureSpec(helper.getContentHeight(), lp2.height, true));

            mainConsumed += Math.max(orientationHelper.getDecoratedMeasurement(child1),
                orientationHelper.getDecoratedMeasurement(child2)) + getVerticalMargin()
                + getVerticalPadding();

            calculateRect(mainConsumed - getVerticalMargin() - getVerticalPadding(), mAreaRect, layoutState, helper);

            if (mHeader != null) {
                layoutChildWithMargin(mHeader, mAreaRect.left, mAreaRect.top, mAreaRect.right, mAreaRect.top + headerConsumed, helper);
            }
            if (mFooter != null) {
                layoutChildWithMargin(mFooter, mAreaRect.left, mAreaRect.bottom - footerConsumed, mAreaRect.right, mAreaRect.bottom, helper);
            }

            int right1 = mAreaRect.left + orientationHelper
                .getDecoratedMeasurementInOther(child1);

            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top + headerConsumed,
                right1, mAreaRect.bottom - footerConsumed, helper);

            layoutChildWithMargin(child2,
                right1, mAreaRect.top + headerConsumed,
                right1 + orientationHelper.getDecoratedMeasurementInOther(child2),
                mAreaRect.bottom - footerConsumed, helper);

        } else {

            if (!Float.isNaN(mAspectRatio)) {
                lp1.width = lp2.width = (int) ((parentHeight - parentVPadding) * mAspectRatio);
            }

            int availableSpace = parentHeight - parentVPadding - lp1.topMargin
                - lp1.bottomMargin
                - lp2.topMargin - lp2.bottomMargin;
            int height1 = Float.isNaN(weight1) ? (int) (availableSpace / 2.0f + 0.5f)
                : (int) (availableSpace * weight1 / 100 + 0.5f);
            int height2 = Float.isNaN(weight2) ? (int) (availableSpace - height1)
                : (int) (availableSpace * weight2 / 100 + 0.5f);

            helper.measureChildWithMargins(child1,
                helper.getChildMeasureSpec(helper.getContentWidth(), lp1.width, true),
                MeasureSpec.makeMeasureSpec(height1 + lp1.topMargin + lp1.bottomMargin,
                    MeasureSpec.EXACTLY));

            int width = child1.getMeasuredWidth();

            helper.measureChildWithMargins(child2,
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height2 + lp2.topMargin + lp2.bottomMargin,
                    MeasureSpec.EXACTLY));

            mainConsumed += Math.max(orientationHelper.getDecoratedMeasurement(child1),
                orientationHelper.getDecoratedMeasurement(child2)) + getHorizontalMargin()
                + getHorizontalPadding();

            calculateRect(mainConsumed - getHorizontalPadding() - getHorizontalMargin(), mAreaRect, layoutState, helper);

            int bottom1 = mAreaRect.top + orientationHelper
                .getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top,
                mAreaRect.right, bottom1, helper);

            layoutChildWithMargin(child2, mAreaRect.left,
                bottom1, mAreaRect.right,
                bottom1 + orientationHelper.getDecoratedMeasurementInOther(child2), helper);
        }

        handleStateOnResult(result, mHeader, child1, child2, mFooter);
        return mainConsumed;
    }

    private int handleThree(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding,
        int headerConsumed, int footerConsumed) {
        int mainConsumed = headerConsumed + footerConsumed;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mContentViews[0];
        final ViewGroup.MarginLayoutParams lp1 = new ViewGroup.MarginLayoutParams(
            child1.getLayoutParams());
        final View child2 = helper.getReverseLayout() ? mContentViews[2] : mContentViews[1];
        final View child3 = helper.getReverseLayout() ? mContentViews[1] : mContentViews[2];

        final ViewGroup.MarginLayoutParams lp2 = new ViewGroup.MarginLayoutParams(
            child2.getLayoutParams());
        final ViewGroup.MarginLayoutParams lp3 = new ViewGroup.MarginLayoutParams(
            child3.getLayoutParams());

        final float weight1 = getViewMainWeight(lp1, 0);
        final float weight2 = getViewMainWeight(lp1, 1);
        final float weight3 = getViewMainWeight(lp1, 2);

        if (layoutInVertical) {

            if (!Float.isNaN(mAspectRatio)) {
                lp1.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
            }

            // make border consistent
            lp2.topMargin = lp1.topMargin;
            lp3.bottomMargin = lp1.bottomMargin;

            lp3.leftMargin = lp2.leftMargin;
            lp3.rightMargin = lp2.rightMargin;

            int availableSpace = parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin
                - lp2.leftMargin - lp2.rightMargin;
            int width1 = Float.isNaN(weight1) ? (int) (availableSpace / 2.0f + 0.5f)
                : (int) (availableSpace * weight1 / 100 + 0.5f);
            int width2 = Float.isNaN(weight2) ? (int) (availableSpace - width1)
                : (int) (availableSpace * weight2 / 100 + 0.5);
            int width3 = Float.isNaN(weight3) ? (int) (width2)
                : (int) (availableSpace * weight3 / 100 + 0.5);

            helper.measureChildWithMargins(child1,
                MeasureSpec.makeMeasureSpec(width1 + lp1.leftMargin + lp1.rightMargin,
                    MeasureSpec.EXACTLY),
                helper.getChildMeasureSpec(helper.getContentHeight(), lp1.height, true));

            int height1 = child1.getMeasuredHeight();
            int height2 =
                Float.isNaN(mRowWeight) ?
                    (int) ((height1 - lp2.bottomMargin - lp3.topMargin) / 2.0f + 0.5f)
                    : (int) ((height1 - lp2.bottomMargin - lp3.topMargin) * mRowWeight
                        / 100 + 0.5f);

            int height3 = height1 - lp2.bottomMargin - lp3.topMargin - height2;

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

            mainConsumed += Math.max(height1 + lp1.topMargin + lp1.bottomMargin,
                height2 + lp2.topMargin + lp2.bottomMargin + height3 + lp3.topMargin
                    + lp3.bottomMargin)
                + getVerticalMargin() + getVerticalPadding();

            calculateRect(mainConsumed - getVerticalMargin() - getVerticalPadding(), mAreaRect, layoutState, helper);

            if (mHeader != null) {
                layoutChildWithMargin(mHeader, mAreaRect.left, mAreaRect.top, mAreaRect.right, mAreaRect.top + headerConsumed, helper);
            }
            if (mFooter != null) {
                layoutChildWithMargin(mFooter, mAreaRect.left, mAreaRect.bottom - footerConsumed, mAreaRect.right, mAreaRect.bottom, helper);
            }

            int right1 = mAreaRect.left + orientationHelper
                .getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top + headerConsumed, right1,
                mAreaRect.bottom - footerConsumed, helper);

            int right2 = right1 + orientationHelper.getDecoratedMeasurementInOther(child2);
            layoutChildWithMargin(child2,
                right1, mAreaRect.top + headerConsumed, right2,
                mAreaRect.top + child2.getMeasuredHeight() + lp2.topMargin
                    + lp2.bottomMargin + headerConsumed, helper);

            layoutChildWithMargin(child3,
                right1,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child3) - footerConsumed,
                right1 + orientationHelper.getDecoratedMeasurementInOther(child3),
                mAreaRect.bottom - footerConsumed, helper);
        } else {
            // TODO: horizontal support
        }

        handleStateOnResult(result, mHeader, child1, child2, child3, mFooter);
        return mainConsumed;
    }

    private int handleFour(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding,
        int headerConsumed, int footerConsumed) {

        int mainConsumed = headerConsumed + footerConsumed;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mContentViews[0];
        final VirtualLayoutManager.LayoutParams lp1 = new VirtualLayoutManager.LayoutParams(
            child1.getLayoutParams());
        final View child2 = helper.getReverseLayout() ? mContentViews[3] : mContentViews[1];
        final VirtualLayoutManager.LayoutParams lp2 = new VirtualLayoutManager.LayoutParams(
            child2.getLayoutParams());
        final View child3 = mContentViews[2];
        final VirtualLayoutManager.LayoutParams lp3 = new VirtualLayoutManager.LayoutParams(
            child3.getLayoutParams());
        final View child4 = helper.getReverseLayout() ? mContentViews[1] : mContentViews[3];
        final VirtualLayoutManager.LayoutParams lp4 = new VirtualLayoutManager.LayoutParams(
            child4.getLayoutParams());

        final float weight1 = getViewMainWeight(lp1, 0);
        final float weight2 = getViewMainWeight(lp1, 1);
        final float weight3 = getViewMainWeight(lp1, 2);
        final float weight4 = getViewMainWeight(lp1, 3);

        if (layoutInVertical) {

            lp2.topMargin = lp1.topMargin;
            lp3.bottomMargin = lp4.bottomMargin = lp1.bottomMargin;
            lp3.leftMargin = lp2.leftMargin;
            lp4.rightMargin = lp2.rightMargin;

            if (!Float.isNaN(mAspectRatio)) {
                lp1.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
            }

            int availableSpace = parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin
                - lp2.leftMargin
                - lp2.rightMargin;

            int width1 = Float.isNaN(weight1) ?
                (int) (availableSpace / 2.0f + 0.5f)
                : (int) (availableSpace * weight1 / 100 + 0.5f);
            int width2 = Float.isNaN(weight2) ? (int) (availableSpace - width1) :
                (int) (availableSpace * weight2 / 100 + 0.5f);

            int width3 = Float.isNaN(weight3) ? (int) (
                (width2 - lp3.rightMargin - lp4.leftMargin) / 2.0f + 0.5f)
                : (int) (availableSpace * weight3 / 100 + 0.5f);
            int width4 = Float.isNaN(weight4) ? (int) ((width2 - lp3.rightMargin
                - lp4.leftMargin - width3))
                : (int) (availableSpace * weight4 / 100 + 0.5f);

            helper.measureChildWithMargins(child1,
                MeasureSpec.makeMeasureSpec(width1 + lp1.leftMargin + lp1.rightMargin,
                    MeasureSpec.EXACTLY),
                helper.getChildMeasureSpec(helper.getContentHeight(), lp1.height, true));

            int height1 = child1.getMeasuredHeight();
            int height2 = Float.isNaN(mRowWeight) ?
                (int) ((height1 - lp2.bottomMargin - lp3.topMargin) / 2.0f + 0.5f)
                : (int) ((height1 - lp2.bottomMargin - lp3.topMargin) * mRowWeight / 100
                    + 0.5f);
            int height3 = (int) ((height1 - lp2.bottomMargin - lp3.topMargin) - height2);

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

            mainConsumed += Math.max(height1 + lp1.topMargin + lp1.bottomMargin,
                height2 + lp2.topMargin + lp2.bottomMargin + Math
                    .max(height3 + lp3.topMargin + lp3.bottomMargin,
                        height3 + lp4.topMargin + lp4.bottomMargin))
                + getVerticalMargin() + getVerticalPadding();

            calculateRect(mainConsumed - getVerticalMargin() - getVerticalPadding(), mAreaRect, layoutState, helper);

            if (mHeader != null) {
                layoutChildWithMargin(mHeader, mAreaRect.left, mAreaRect.top, mAreaRect.right, mAreaRect.top + headerConsumed, helper);
            }
            if (mFooter != null) {
                layoutChildWithMargin(mFooter, mAreaRect.left, mAreaRect.bottom - footerConsumed, mAreaRect.right, mAreaRect.bottom, helper);
            }

            int right1 = mAreaRect.left + orientationHelper
                .getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top + headerConsumed,
                right1, mAreaRect.bottom - footerConsumed, helper);

            int right2 = right1 + orientationHelper.getDecoratedMeasurementInOther(child2);
            layoutChildWithMargin(child2, right1, mAreaRect.top + headerConsumed, right2,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2) + headerConsumed,
                helper);

            int right3 = right1 + orientationHelper.getDecoratedMeasurementInOther(child3);
            layoutChildWithMargin(child3, right1,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child3) - footerConsumed,
                right3, mAreaRect.bottom - footerConsumed, helper);

            layoutChildWithMargin(child4, right3,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child4) - footerConsumed,
                right3 + orientationHelper.getDecoratedMeasurementInOther(child4),
                mAreaRect.bottom - footerConsumed, helper);
        } else {
            // TODO: horizontal support
        }

        handleStateOnResult(result, mHeader, child1, child2, child3, child4, mFooter);
        return mainConsumed;
    }

    private int handleFive(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding,
        int headerConsumed, int footerConsumed) {
        int mainConsumed = headerConsumed + footerConsumed;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mContentViews[0];
        final VirtualLayoutManager.LayoutParams lp1 = new VirtualLayoutManager.LayoutParams(
            child1.getLayoutParams());
        final View child2 = helper.getReverseLayout() ? mContentViews[4] : mContentViews[1];
        final VirtualLayoutManager.LayoutParams lp2 = new VirtualLayoutManager.LayoutParams(
            child2.getLayoutParams());
        final View child3 = helper.getReverseLayout() ? mContentViews[3] : mContentViews[2];
        final VirtualLayoutManager.LayoutParams lp3 = new VirtualLayoutManager.LayoutParams(
            child3.getLayoutParams());
        final View child4 = helper.getReverseLayout() ? mContentViews[2] : mContentViews[3];
        final VirtualLayoutManager.LayoutParams lp4 = new VirtualLayoutManager.LayoutParams(
            child4.getLayoutParams());
        final View child5 = helper.getReverseLayout() ? mContentViews[1] : mContentViews[4];
        final VirtualLayoutManager.LayoutParams lp5 = new VirtualLayoutManager.LayoutParams(
            child5.getLayoutParams());

        final float weight1 = getViewMainWeight(lp1, 0);
        final float weight2 = getViewMainWeight(lp1, 1);
        final float weight3 = getViewMainWeight(lp1, 2);
        final float weight4 = getViewMainWeight(lp1, 3);
        final float weight5 = getViewMainWeight(lp1, 4);

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
            int width2 = Float.isNaN(weight2) ? (int) (availableSpace - width1) :
                (int) (availableSpace * weight2 / 100 + 0.5f);

            int width3 = Float.isNaN(weight3) ? (int) (
                (width2 - lp3.rightMargin - lp4.leftMargin) / 3.0f + 0.5f)
                : (int) (availableSpace * weight3 / 100 + 0.5f);
            int width4 = Float.isNaN(weight4) ? (int) (
                (width2 - lp3.rightMargin - lp4.leftMargin) / 3.0f + 0.5f)
                : (int) (availableSpace * weight4 / 100 + 0.5f);
            int width5 = Float.isNaN(weight5) ? (int) ((width2 - lp3.rightMargin
                - lp4.leftMargin - width3 - width4))
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
            int height3 = (int) ((height1 - lp2.bottomMargin - lp3.topMargin) - height2);

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

            mainConsumed += Math.max(height1 + lp1.topMargin + lp1.bottomMargin,
                height2 + lp2.topMargin + lp2.bottomMargin + Math
                    .max(height3 + lp3.topMargin + lp3.bottomMargin,
                        height3 + lp4.topMargin + lp4.bottomMargin))
                + getVerticalMargin() + getVerticalPadding();

            calculateRect(mainConsumed - getVerticalMargin() - getVerticalPadding(), mAreaRect, layoutState, helper);

            if (mHeader != null) {
                layoutChildWithMargin(mHeader, mAreaRect.left, mAreaRect.top, mAreaRect.right, mAreaRect.top + headerConsumed, helper);
            }
            if (mFooter != null) {
                layoutChildWithMargin(mFooter, mAreaRect.left, mAreaRect.bottom - footerConsumed, mAreaRect.right, mAreaRect.bottom, helper);
            }

            int right1 = mAreaRect.left + orientationHelper
                .getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top + headerConsumed,
                right1, mAreaRect.bottom - footerConsumed, helper);

            int right2 = right1 + orientationHelper.getDecoratedMeasurementInOther(child2);
            layoutChildWithMargin(child2, right1, mAreaRect.top + headerConsumed, right2,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2) + headerConsumed,
                helper);

            int right3 = right1 + orientationHelper.getDecoratedMeasurementInOther(child3);
            layoutChildWithMargin(child3, right1,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child3) - footerConsumed,
                right3, mAreaRect.bottom - footerConsumed, helper);

            int right4 = right3 + orientationHelper.getDecoratedMeasurementInOther(child4);
            layoutChildWithMargin(child4, right3,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child4) - footerConsumed,
                right3 + orientationHelper.getDecoratedMeasurementInOther(child4),
                mAreaRect.bottom - footerConsumed, helper);

            layoutChildWithMargin(child5, right4,
                mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child5) - footerConsumed,
                right4 + orientationHelper.getDecoratedMeasurementInOther(child5),
                mAreaRect.bottom - footerConsumed, helper);
        } else {
            // TODO: horizontal support
        }

        handleStateOnResult(result, mHeader, child1, child2, child3, child4, child5, mFooter);
        return mainConsumed;
    }

}
