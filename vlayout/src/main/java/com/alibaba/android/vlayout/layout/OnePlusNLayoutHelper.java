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

import java.util.Arrays;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.OrientationHelperEx;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.AnchorInfoWrapper;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutParams;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.State;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

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

    private float[] mColWeights = new float[0];

    private float mRowWeight = Float.NaN;

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
        final OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        final boolean layoutStart = layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START;
        final int parentWidth = helper.getContentWidth();
        final int parentHeight = helper.getContentHeight();
        final int parentHPadding = helper.getPaddingLeft() + helper.getPaddingRight()
            + getHorizontalMargin() + getHorizontalPadding();
        final int parentVPadding = helper.getPaddingTop() + helper.getPaddingBottom()
            + getVerticalMargin() + getVerticalPadding();

        final int currentPosition = layoutState.getCurrentPosition();
        if (hasHeader && currentPosition == getRange().getLower()) {
            View header = nextView(recycler, layoutState, helper, result);
            int headerConsumed = handleHeader(header, layoutState, result, helper, layoutInVertical, parentWidth, parentHeight,
                parentHPadding, parentVPadding);
            if (header != null) {
                int left = 0, right = 0, top = 0, bottom = 0;
                if (layoutInVertical) {
                    if (layoutStart) {
                        bottom = layoutState.getOffset();
                        top = bottom - headerConsumed;
                    } else {
                        top = layoutState.getOffset() + (mLayoutWithAnchor ? 0 : mMarginTop + mPaddingTop);
                        bottom = top + headerConsumed;
                    }
                    left = helper.getPaddingLeft() + mMarginLeft + mPaddingLeft;
                    right = left + orientationHelper.getDecoratedMeasurementInOther(header);
                } else {
                    if (layoutStart) {
                        right = layoutState.getOffset();
                        left = right - headerConsumed;
                    } else {
                        left = layoutState.getOffset() + (mLayoutWithAnchor ? 0 : mMarginLeft + mPaddingLeft);
                        right = left + headerConsumed;
                    }
                    top = helper.getPaddingTop() + mMarginTop + mPaddingTop;
                    bottom = top + orientationHelper.getDecoratedMeasurementInOther(header);
                }
                layoutChildWithMargin(header, left, top, right, bottom, helper);
            }
            result.mConsumed = headerConsumed;
            handleStateOnResult(result, header);
        } else if (hasFooter && currentPosition == getRange().getUpper()) {
            View footer = nextView(recycler, layoutState, helper, result);
            int footerConsumed = handleFooter(footer, layoutState, result, helper, layoutInVertical, parentWidth, parentHeight,
                parentHPadding, parentVPadding);
            if (footer != null) {
                int left = 0, right = 0, top = 0, bottom = 0;
                if (layoutInVertical) {
                    if (layoutStart) {
                        bottom = layoutState.getOffset() - (mLayoutWithAnchor ? 0 : mMarginBottom + mPaddingBottom); //TODO margin overlap
                        top = bottom - footerConsumed;
                    } else {
                        top = layoutState.getOffset();
                        bottom = top + footerConsumed;
                    }
                    left = helper.getPaddingLeft() + mMarginLeft + mPaddingLeft;
                    right = left + orientationHelper.getDecoratedMeasurementInOther(footer);
                } else {
                    if (layoutStart) {
                        right = layoutState.getOffset() - (mLayoutWithAnchor ? 0 : mMarginRight + mPaddingRight); //TODO margin overlap
                        left = right - footerConsumed;
                    } else {
                        left = layoutState.getOffset();
                        right = left + footerConsumed;
                    }
                    top = helper.getPaddingTop() + mMarginTop + mPaddingTop;
                    bottom = top + orientationHelper.getDecoratedMeasurementInOther(footer);
                }
                layoutChildWithMargin(footer, left, top, right, bottom, helper);
            }
            result.mConsumed = footerConsumed;
            handleStateOnResult(result, footer);
        } else {
            int contentCount = getItemCount() - (hasHeader ? 1 : 0) - (hasFooter ? 1 : 0);
            if (mChildrenViews == null || mChildrenViews.length != contentCount) {
                mChildrenViews = new View[contentCount];
            }
            int count = getAllChildren(mChildrenViews, recycler, layoutState, result, helper);
            if (count == 0 || count < contentCount) {
                return;
            }
            int mainConsumed = 0;
            if (contentCount == 1) {
                mainConsumed = handleOne(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding);
            } else if (contentCount == 2) {
                mainConsumed = handleTwo(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding);
            } else if (contentCount == 3) {
                mainConsumed = handleThree(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding);
            } else if (contentCount == 4) {
                mainConsumed = handleFour(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding);
            } else if (contentCount == 5) {
                mainConsumed = handleFive(layoutState, result, helper, layoutInVertical, parentWidth, parentHeight, parentHPadding, parentVPadding);
            }
            result.mConsumed = mainConsumed;
            Arrays.fill(mChildrenViews, null);
        }
    }

    private float getViewMainWeight(int index) {
        if (mColWeights.length > index) {
            return mColWeights[index];
        }

        return Float.NaN;
    }

    @Override
    protected void onClear(LayoutManagerHelper helper) {
        super.onClear(helper);
    }

    @Override
    public void checkAnchorInfo(State state, AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {
        super.checkAnchorInfo(state, anchorInfo, helper);
        mLayoutWithAnchor = true;
    }

    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor,
            LayoutManagerHelper helper) {
        //Log.d(TAG,
        //    "range " + getRange() + " offset " + offset + " isLayoutEnd " + isLayoutEnd + " useAnchor " + useAnchor
        //        + " helper " + this.hashCode());
        final boolean layoutInVertical = helper.getOrientation() == LinearLayoutManager.VERTICAL;

        if (useAnchor) {
            return 0;
        }
        if (isLayoutEnd) {
            if (offset == getItemCount() - 1) {
                return layoutInVertical ? mMarginBottom + mPaddingBottom : mMarginRight + mPaddingRight;
            }
        } else {
            if (offset == 0) {
                return layoutInVertical ? -mMarginTop - mPaddingTop : -mMarginLeft - mPaddingLeft;
            }
        }
        return 0;
    }

    private int handleHeader(View header, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {
        if (header == null) {
            return 0;
        }
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final VirtualLayoutManager.LayoutParams lp = (LayoutParams) header.getLayoutParams();


        // fill width
        int widthSpec = helper.getChildMeasureSpec(parentWidth - parentHPadding,
            layoutInVertical ? MATCH_PARENT : lp.width, !layoutInVertical);
        int heightSpec = helper.getChildMeasureSpec(parentHeight - parentVPadding,
            layoutInVertical ? lp.height : MeasureSpec.EXACTLY, layoutInVertical);
        helper.measureChildWithMargins(header, widthSpec, heightSpec);
        return orientationHelper.getDecoratedMeasurement(header);
    }

    private int handleFooter(View footer, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {
        if (footer == null) {
            return 0;
        }

        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final VirtualLayoutManager.LayoutParams lp = (LayoutParams) footer.getLayoutParams();

        // fill width
        int widthSpec = helper.getChildMeasureSpec(parentWidth - parentHPadding,
            layoutInVertical ? MATCH_PARENT : lp.width, !layoutInVertical);
        int heightSpec = helper.getChildMeasureSpec(parentHeight - parentVPadding,
            layoutInVertical ? lp.height : MeasureSpec.EXACTLY, layoutInVertical);
        helper.measureChildWithMargins(footer, widthSpec, heightSpec);
        return orientationHelper.getDecoratedMeasurement(footer);
    }

    private int handleOne(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {
        int mainConsumed = 0;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        View view = mChildrenViews[0];
        final VirtualLayoutManager.LayoutParams lp = (LayoutParams) view.getLayoutParams();

        if (!Float.isNaN(mAspectRatio)) {
            if (layoutInVertical) {
                lp.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
            } else {
                lp.width = (int) ((parentHeight - parentVPadding) * mAspectRatio);
            }
        }

        final float weight = getViewMainWeight(0);

        // fill width
        int widthSpec = helper.getChildMeasureSpec(
            Float.isNaN(weight) ? (parentWidth - parentHPadding)
                : (int) ((parentWidth - parentHPadding) * weight),
            layoutInVertical ? MATCH_PARENT : lp.width, !layoutInVertical);
        int heightSpec = helper.getChildMeasureSpec(parentHeight - parentVPadding,
            layoutInVertical ? lp.height : MeasureSpec.EXACTLY, layoutInVertical);

        helper.measureChildWithMargins(view, widthSpec, heightSpec);

        mainConsumed += orientationHelper.getDecoratedMeasurement(view);

        calculateRect(mainConsumed, mAreaRect, layoutState, helper);

        layoutChildWithMargin(view, mAreaRect.left, mAreaRect.top, mAreaRect.right, mAreaRect.bottom,
            helper);
        handleStateOnResult(result, view);
        mainConsumed = mAreaRect.bottom - mAreaRect.top + (hasHeader ? 0 : mMarginTop + mPaddingTop) + (hasFooter ? 0 : mMarginBottom + mPaddingBottom);
        return mainConsumed;
    }

    private int handleTwo(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {
        int mainConsumed = 0;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mChildrenViews[0];
        final VirtualLayoutManager.LayoutParams lp1 = (VirtualLayoutManager.LayoutParams) child1.getLayoutParams();
        final View child2 = mChildrenViews[1];
        final VirtualLayoutManager.LayoutParams lp2 = (VirtualLayoutManager.LayoutParams) child2.getLayoutParams();
        final float weight1 = getViewMainWeight(0);
        final float weight2 = getViewMainWeight(1);


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
                orientationHelper.getDecoratedMeasurement(child2));

            calculateRect(mainConsumed, mAreaRect, layoutState, helper);

            int right1 = mAreaRect.left + orientationHelper
                .getDecoratedMeasurementInOther(child1);

            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top, right1, mAreaRect.bottom, helper);

            layoutChildWithMargin(child2, right1, mAreaRect.top,
                right1 + orientationHelper.getDecoratedMeasurementInOther(child2), mAreaRect.bottom, helper);

            mainConsumed = mAreaRect.bottom - mAreaRect.top + (hasHeader ? 0 : mMarginTop + mPaddingTop) + (hasFooter ? 0 : mMarginBottom + mPaddingBottom);
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
                orientationHelper.getDecoratedMeasurement(child2));

            calculateRect(mainConsumed, mAreaRect, layoutState, helper);

            int bottom1 = mAreaRect.top + orientationHelper
                .getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top,
                mAreaRect.right, bottom1, helper);

            layoutChildWithMargin(child2, mAreaRect.left,
                bottom1, mAreaRect.right,
                bottom1 + orientationHelper.getDecoratedMeasurementInOther(child2), helper);
            mainConsumed = mAreaRect.right - mAreaRect.left + (hasHeader ? 0 : mMarginLeft + mPaddingRight) + (hasFooter ? 0 : mMarginRight + mPaddingRight);
        }

        handleStateOnResult(result, mChildrenViews);
        return mainConsumed;
    }

    private int handleThree(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {
        int mainConsumed = 0;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mChildrenViews[0];
        final VirtualLayoutManager.LayoutParams lp1 = (VirtualLayoutManager.LayoutParams) child1.getLayoutParams();
        final View child2 = helper.getReverseLayout() ? mChildrenViews[2] : mChildrenViews[1];
        final View child3 = helper.getReverseLayout() ? mChildrenViews[1] : mChildrenViews[2];

        final VirtualLayoutManager.LayoutParams lp2 = (VirtualLayoutManager.LayoutParams) child2.getLayoutParams();
        final VirtualLayoutManager.LayoutParams lp3 = (VirtualLayoutManager.LayoutParams) child3.getLayoutParams();

        final float weight1 = getViewMainWeight(0);
        final float weight2 = getViewMainWeight(1);
        final float weight3 = getViewMainWeight(2);

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
                    + lp3.bottomMargin);

            calculateRect(mainConsumed, mAreaRect, layoutState, helper);

            int right1 = mAreaRect.left + orientationHelper
                .getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top, right1, mAreaRect.bottom, helper);

            int right2 = right1 + orientationHelper.getDecoratedMeasurementInOther(child2);
            layoutChildWithMargin(child2, right1, mAreaRect.top, right2,
                mAreaRect.top + child2.getMeasuredHeight() + lp2.topMargin + lp2.bottomMargin, helper);

            layoutChildWithMargin(child3, right1, mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child3),
                right1 + orientationHelper.getDecoratedMeasurementInOther(child3), mAreaRect.bottom, helper);
            mainConsumed = mAreaRect.bottom - mAreaRect.top + (hasHeader ? 0 : mMarginTop + mPaddingTop) + (hasFooter ? 0 : mMarginBottom + mPaddingBottom);
        } else {
            // TODO: horizontal support
        }

        handleStateOnResult(result, mChildrenViews);
        return mainConsumed;
    }

    private int handleFour(LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper,
        boolean layoutInVertical, int parentWidth, int parentHeight, int parentHPadding, int parentVPadding) {

        int mainConsumed = 0;
        OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();

        final View child1 = mChildrenViews[0];
        final VirtualLayoutManager.LayoutParams lp1 = (VirtualLayoutManager.LayoutParams) child1.getLayoutParams();
        final View child2 = helper.getReverseLayout() ? mChildrenViews[3] : mChildrenViews[1];
        final VirtualLayoutManager.LayoutParams lp2 = (VirtualLayoutManager.LayoutParams) child2.getLayoutParams();
        final View child3 = mChildrenViews[2];
        final VirtualLayoutManager.LayoutParams lp3 = (VirtualLayoutManager.LayoutParams) child3.getLayoutParams();
        final View child4 = helper.getReverseLayout() ? mChildrenViews[1] : mChildrenViews[3];
        final VirtualLayoutManager.LayoutParams lp4 = (VirtualLayoutManager.LayoutParams) child4.getLayoutParams();

        final float weight1 = getViewMainWeight(0);
        final float weight2 = getViewMainWeight(1);
        final float weight3 = getViewMainWeight(2);
        final float weight4 = getViewMainWeight(3);

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
                        height3 + lp4.topMargin + lp4.bottomMargin));

            calculateRect(mainConsumed, mAreaRect, layoutState, helper);

            int right1 = mAreaRect.left + orientationHelper
                .getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top, right1, mAreaRect.bottom, helper);

            int right2 = right1 + orientationHelper.getDecoratedMeasurementInOther(child2);
            layoutChildWithMargin(child2, right1, mAreaRect.top, right2,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2), helper);

            int right3 = right1 + orientationHelper.getDecoratedMeasurementInOther(child3);
            layoutChildWithMargin(child3, right1, mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child3),
                right3, mAreaRect.bottom, helper);

            layoutChildWithMargin(child4, right3, mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child4),
                right3 + orientationHelper.getDecoratedMeasurementInOther(child4), mAreaRect.bottom, helper);
            mainConsumed = mAreaRect.bottom - mAreaRect.top + (hasHeader ? 0 : mMarginTop + mPaddingTop) + (hasFooter ? 0 : mMarginBottom + mPaddingBottom);
        } else {
            // TODO: horizontal support
        }

        handleStateOnResult(result, mChildrenViews);
        return mainConsumed;
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
                        height3 + lp4.topMargin + lp4.bottomMargin));

            calculateRect(mainConsumed, mAreaRect, layoutState, helper);

            int right1 = mAreaRect.left + orientationHelper.getDecoratedMeasurementInOther(child1);
            layoutChildWithMargin(child1, mAreaRect.left, mAreaRect.top, right1, mAreaRect.bottom, helper);

            int right2 = right1 + orientationHelper.getDecoratedMeasurementInOther(child2);
            layoutChildWithMargin(child2, right1, mAreaRect.top, right2,
                mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2), helper);

            int right3 = right1 + orientationHelper.getDecoratedMeasurementInOther(child3);
            layoutChildWithMargin(child3, right1, mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child3),
                right3, mAreaRect.bottom, helper);

            int right4 = right3 + orientationHelper.getDecoratedMeasurementInOther(child4);
            layoutChildWithMargin(child4, right3, mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child4),
                right3 + orientationHelper.getDecoratedMeasurementInOther(child4), mAreaRect.bottom, helper);

            layoutChildWithMargin(child5, right4, mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child5),
                right4 + orientationHelper.getDecoratedMeasurementInOther(child5), mAreaRect.bottom, helper);
            mainConsumed = mAreaRect.bottom - mAreaRect.top + (hasHeader ? 0 : mMarginTop + mPaddingTop) + (hasFooter ? 0 : mMarginBottom + mPaddingBottom);
        } else {
            // TODO: horizontal support
        }

        handleStateOnResult(result, mChildrenViews);
        return mainConsumed;
    }

}
