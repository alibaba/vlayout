package com.alibaba.android.vlayout.layout;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import java.util.Arrays;

import static android.support.v7.widget._ExposeLinearLayoutManagerEx.VERTICAL;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

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
 * </pre>
 *
 * @author villadora
 * @since 1.0.0
 */
public class OnePlusNLayoutHelper extends AbstractFullFillLayoutHelper {

    private static final String TAG = OnePlusNLayoutHelper.class.getSimpleName();


    private Rect mAreaRect = new Rect();

    private View[] mChildrenViews;

    private float mDefaultWeight = Float.NaN;

    private boolean mMarginCollapse = true;

    public OnePlusNLayoutHelper() {
        setItemCount(0);
    }

    public OnePlusNLayoutHelper(int itemCount) {
        this(itemCount, 0, 0, 0, 0);
    }

    public OnePlusNLayoutHelper(int itemCount, int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        setItemCount(itemCount);
    }

    /**
     * @param marginCollapse tell whether collapse margins
     */
    public void setMarginCollapse(boolean marginCollapse) {
        this.mMarginCollapse = marginCollapse;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Currently, this layout supports maximum children up to 4, otherwise {@link IllegalArgumentException}
     * will be thrown
     *
     * @param start start position of items handled by this layoutHelper
     * @param end   end position of items handled by this layoutHelper, if end &lt; start or end - start &gt 4, it will throw {@link IllegalArgumentException}
     */
    @Override
    public void onRangeChange(int start, int end) {
        if (end - start > 3) {
            throw new IllegalArgumentException("OnePlusNLayoutHelper only supports maximum 4 children now");
        }
    }


    public void setDefaultWeight(float weight) {
        mDefaultWeight = weight >= 0 ? weight : Float.NaN;
    }


    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        final int originCurPos = layoutState.getCurrentPosition();

        if (mChildrenViews == null || mChildrenViews.length != getItemCount()) {
            mChildrenViews = new View[getItemCount()];
        }

        int count = getAllChildren(mChildrenViews, recycler, layoutState, result, helper);

        if (count != getItemCount()) {
            Log.w(TAG, "The real number of children is not match with range of LayoutHelper");
        }

        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        final OrientationHelper orientationHelper = helper.getMainOrientationHelper();

        final int parentWidth = helper.getContentWidth();
        final int parentHeight = helper.getContentHeight();
        final int parentHPadding = helper.getPaddingLeft() + helper.getPaddingRight() + getHorizontalMargin();
        final int parentVPadding = helper.getPaddingTop() + helper.getPaddingBottom() + getVerticalMargin();

        int mainConsumed = 0;

        if (count == 1) {
            View view = mChildrenViews[0];
            final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();


            if (mMarginCollapse) {
                lp.leftMargin = mergeLayoutMargin(lp.leftMargin, mMarginLeft);
                lp.topMargin = mergeLayoutMargin(lp.topMargin, mMarginTop);
                lp.rightMargin = mergeLayoutMargin(lp.rightMargin, mMarginRight);
                lp.bottomMargin = mergeLayoutMargin(lp.bottomMargin, mMarginBottom);
            }

            if (!Float.isNaN(mAspectRatio)) {
                if (layoutInVertical) {
                    lp.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
                } else {
                    lp.width = (int) ((parentHeight - parentVPadding) * mAspectRatio);
                }
            }

            // fill width
            int widthSpec = helper.getChildMeasureSpec(parentWidth - parentHPadding,
                    layoutInVertical ? MATCH_PARENT : lp.width, !layoutInVertical);
            int heightSpec = helper.getChildMeasureSpec(parentHeight - parentVPadding,
                    layoutInVertical ? lp.height : MeasureSpec.EXACTLY, layoutInVertical);

            helper.measureChild(view, widthSpec, heightSpec);

            mainConsumed = orientationHelper.getDecoratedMeasurement(view) + (layoutInVertical ? getVerticalMargin() : getHorizontalMargin());

            calculateRect(mainConsumed, mAreaRect, layoutState, helper);

            layoutChild(view, mAreaRect.left, mAreaRect.top, mAreaRect.right, mAreaRect.bottom, helper);
            handleStateOnResult(result, view);
        } else if (count == 2) {

            final View child1 = mChildrenViews[0];
            final VirtualLayoutManager.LayoutParams lp1 = (VirtualLayoutManager.LayoutParams) child1.getLayoutParams();
            final View child2 = mChildrenViews[1];
            final VirtualLayoutManager.LayoutParams lp2 = (VirtualLayoutManager.LayoutParams) child2.getLayoutParams();
            final float weight1 = getViewWeight(lp1);

            if (layoutInVertical) {

                if (mMarginCollapse) {
                    lp1.leftMargin = mergeLayoutMargin(lp1.leftMargin, mMarginLeft);
                    lp1.topMargin = mergeLayoutMargin(lp1.topMargin, mMarginTop);
                    lp2.rightMargin = mergeLayoutMargin(lp2.rightMargin, mMarginRight);
                    lp1.bottomMargin = mergeLayoutMargin(lp1.bottomMargin, mMarginBottom);

                    lp1.rightMargin = Math.max(lp1.rightMargin, lp2.leftMargin);
                    lp2.leftMargin = 0;
                }


                if (!Float.isNaN(mAspectRatio)) {
                    lp1.height = lp2.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
                }

                lp2.topMargin = lp1.topMargin;
                lp2.bottomMargin = lp1.bottomMargin;


                int width1 = Float.isNaN(weight1) ?
                        (int) ((parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin - lp2.leftMargin
                                - lp2.rightMargin) / 2.0f + 0.5f)
                        : (int) ((parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin - lp2.leftMargin
                        - lp2.rightMargin) * weight1 / 100 + 0.5f);
                int width2 =
                        (int) ((parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin - lp2.leftMargin
                                - lp2.rightMargin) - width1);

                helper.measureChild(child1,
                        MeasureSpec.makeMeasureSpec(width1 + lp1.leftMargin + lp1.rightMargin, MeasureSpec.EXACTLY),
                        helper.getChildMeasureSpec(helper.getContentHeight(), lp1.height, true));

                helper.measureChild(child2,
                        MeasureSpec.makeMeasureSpec(width2 + lp2.leftMargin + lp2.rightMargin, MeasureSpec.EXACTLY),
                        helper.getChildMeasureSpec(helper.getContentHeight(), lp2.height, true));

                mainConsumed = Math.max(orientationHelper.getDecoratedMeasurement(child1),
                        orientationHelper.getDecoratedMeasurement(child2)) + getVerticalMargin();

                calculateRect(mainConsumed, mAreaRect, layoutState, helper);

                layoutChild(child1, mAreaRect.left, mAreaRect.top,
                        mAreaRect.left + orientationHelper.getDecoratedMeasurementInOther(child1),
                        mAreaRect.bottom, helper);

                layoutChild(child2,
                        mAreaRect.right - orientationHelper.getDecoratedMeasurementInOther(child2),
                        mAreaRect.top, mAreaRect.right, mAreaRect.bottom, helper);


            } else {
                if (mMarginCollapse) {
                    lp1.leftMargin = mergeLayoutMargin(lp1.leftMargin, mMarginLeft);
                    lp1.topMargin = mergeLayoutMargin(lp1.topMargin, mMarginTop);
                    lp1.rightMargin = mergeLayoutMargin(lp1.rightMargin, mMarginRight);
                    lp2.bottomMargin = mergeLayoutMargin(lp2.bottomMargin, mMarginBottom);


                    lp1.bottomMargin = Math.max(lp1.bottomMargin, lp2.topMargin);
                    lp2.topMargin = 0;
                }

                if (!Float.isNaN(mAspectRatio)) {
                    lp1.width = lp2.width = (int) ((parentHeight - parentVPadding) * mAspectRatio);
                }


                int height1 = Float.isNaN(weight1) ?
                        (int) ((parentHeight - parentVPadding - lp1.topMargin - lp1.bottomMargin - lp2.topMargin
                                - lp2.bottomMargin) / 2.0f + 0.5f)
                        : (int) ((parentHeight - parentVPadding - lp1.topMargin - lp1.bottomMargin - lp2.topMargin
                        - lp2.bottomMargin) * weight1 / 100 + 0.5f);
                int height2 = (int) ((parentHeight - parentVPadding - lp1.topMargin - lp1.bottomMargin - lp2.topMargin
                        - lp2.bottomMargin) - height1);

                helper.measureChild(child1,
                        helper.getChildMeasureSpec(helper.getContentWidth(), lp1.width, true),
                        MeasureSpec.makeMeasureSpec(height1 + lp1.topMargin + lp1.bottomMargin, MeasureSpec.EXACTLY));

                int width = child1.getMeasuredWidth();

                helper.measureChild(child2,
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height2 + lp2.topMargin + lp2.bottomMargin, MeasureSpec.EXACTLY));

                mainConsumed = Math.max(orientationHelper.getDecoratedMeasurement(child1),
                        orientationHelper.getDecoratedMeasurement(child2)) + getHorizontalMargin();

                calculateRect(mainConsumed, mAreaRect, layoutState, helper);

                layoutChild(child1, mAreaRect.left, mAreaRect.top,
                        mAreaRect.right,
                        mAreaRect.top + orientationHelper.getDecoratedMeasurementInOther(child1),
                        helper);

                layoutChild(child2, mAreaRect.left,
                        mAreaRect.bottom - orientationHelper.getDecoratedMeasurementInOther(child2),
                        mAreaRect.right, mAreaRect.bottom, helper);
            }

            handleStateOnResult(result, child1, child2);
        } else if (count == 3) {

            final View child1 = mChildrenViews[0];
            final VirtualLayoutManager.LayoutParams lp1 = (VirtualLayoutManager.LayoutParams) child1.getLayoutParams();
            final View child2 = helper.getReverseLayout() ? mChildrenViews[2] : mChildrenViews[1];
            final View child3 = helper.getReverseLayout() ? mChildrenViews[1] : mChildrenViews[2];

            final VirtualLayoutManager.LayoutParams lp2 = (VirtualLayoutManager.LayoutParams) child2.getLayoutParams();
            final VirtualLayoutManager.LayoutParams lp3 = (VirtualLayoutManager.LayoutParams) child3.getLayoutParams();


            final float weight = getViewWeight(lp1);

            if (layoutInVertical) {

                if (mMarginCollapse) {
                    lp1.leftMargin = mergeLayoutMargin(lp1.leftMargin, mMarginLeft);
                    lp1.topMargin = mergeLayoutMargin(lp1.topMargin, mMarginTop);
                    lp2.rightMargin = mergeLayoutMargin(lp2.rightMargin, mMarginRight);
                    lp1.bottomMargin = mergeLayoutMargin(lp1.bottomMargin, mMarginBottom);


                    lp1.rightMargin = Math.max(Math.max(lp1.rightMargin, lp2.leftMargin), lp3.leftMargin);
                    lp2.leftMargin = lp3.leftMargin = 0;

                    lp2.bottomMargin = Math.max(lp2.bottomMargin, lp3.topMargin);
                    lp3.topMargin = 0;
                }


                if (!Float.isNaN(mAspectRatio)) {
                    lp1.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
                }


                // make border consistent
                lp2.topMargin = lp1.topMargin;
                lp3.bottomMargin = lp1.bottomMargin;

                lp3.leftMargin = lp2.leftMargin;
                lp3.rightMargin = lp2.rightMargin;

                int width1 = Float.isNaN(weight) ?
                        (int) ((parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin - lp2.leftMargin
                                - lp2.rightMargin) / 2.0f + 0.5f)
                        : (int) ((parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin - lp2.leftMargin
                        - lp2.rightMargin) * weight / 100 + 0.5f);
                int width2 = (int) ((parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin - lp2.leftMargin
                        - lp2.rightMargin) - width1);

                helper.measureChild(child1,
                        MeasureSpec.makeMeasureSpec(width1 + lp1.leftMargin + lp1.rightMargin, MeasureSpec.EXACTLY),
                        helper.getChildMeasureSpec(helper.getContentHeight(), lp1.height, true));

                int height1 = child1.getMeasuredHeight();
                int height2 = (int) ((height1 - lp2.bottomMargin - lp3.topMargin) / 2.0f + 0.5f);
                int height3 = height1 - lp2.bottomMargin - lp3.topMargin - height2;

                helper.measureChild(child2,
                        MeasureSpec.makeMeasureSpec(width2 + lp2.leftMargin + lp2.rightMargin, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height2 + lp2.topMargin + lp2.bottomMargin, MeasureSpec.EXACTLY));

                helper.measureChild(child3,
                        MeasureSpec.makeMeasureSpec(width2 + lp3.leftMargin + lp3.rightMargin, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height3 + lp3.topMargin + lp3.bottomMargin, MeasureSpec.EXACTLY));


                mainConsumed = Math.max(height1 + lp1.topMargin + lp1.bottomMargin,
                        height2 + lp2.topMargin + lp2.bottomMargin + height3 + lp3.topMargin + lp3.bottomMargin)
                        + getVerticalMargin();

                calculateRect(mainConsumed, mAreaRect, layoutState, helper);

                layoutChild(child1, mAreaRect.left, mAreaRect.top,
                        mAreaRect.left + orientationHelper.getDecoratedMeasurementInOther(child1),
                        mAreaRect.bottom, helper);

                layoutChild(child2,
                        mAreaRect.right - orientationHelper.getDecoratedMeasurementInOther(child2),
                        mAreaRect.top, mAreaRect.right, mAreaRect.top + child2.getMeasuredHeight() + lp2.topMargin + lp2.bottomMargin, helper);

                layoutChild(child3,
                        mAreaRect.right - orientationHelper.getDecoratedMeasurementInOther(child3),
                        mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child3),
                        mAreaRect.right, mAreaRect.bottom, helper);
            } else {
                // TODO: horizontal support
            }

            handleStateOnResult(result, child1, child2, child3);
        } else if (count == 4) {
            final View child1 = mChildrenViews[0];
            final VirtualLayoutManager.LayoutParams lp1 = (VirtualLayoutManager.LayoutParams) child1.getLayoutParams();
            final View child2 = helper.getReverseLayout() ? mChildrenViews[3] : mChildrenViews[1];
            final VirtualLayoutManager.LayoutParams lp2 = (VirtualLayoutManager.LayoutParams) child2.getLayoutParams();
            final View child3 = mChildrenViews[2];
            final VirtualLayoutManager.LayoutParams lp3 = (VirtualLayoutManager.LayoutParams) child3.getLayoutParams();
            final View child4 = helper.getReverseLayout() ? mChildrenViews[1] : mChildrenViews[3];
            final VirtualLayoutManager.LayoutParams lp4 = (VirtualLayoutManager.LayoutParams) child4.getLayoutParams();
            final float weight = getViewWeight(lp1);


            if (layoutInVertical) {

                if (mMarginCollapse) {
                    lp1.leftMargin = mergeLayoutMargin(lp1.leftMargin, mMarginLeft);
                    lp1.topMargin = mergeLayoutMargin(lp1.topMargin, mMarginTop);
                    lp2.rightMargin = mergeLayoutMargin(lp2.rightMargin, mMarginRight);
                    lp1.bottomMargin = mergeLayoutMargin(lp1.bottomMargin, mMarginBottom);


                    lp1.rightMargin = Math.max(Math.max(lp1.rightMargin, lp2.leftMargin), lp3.leftMargin);
                    lp2.leftMargin = lp3.leftMargin = 0;

                    lp2.bottomMargin = Math.max(Math.max(lp2.bottomMargin, lp3.topMargin), lp3.topMargin);
                    lp3.topMargin = lp4.topMargin = 0;

                    lp3.rightMargin = Math.max(lp3.rightMargin, lp4.leftMargin);
                    lp4.leftMargin = 0;
                }

                lp2.topMargin = lp1.topMargin;
                lp3.bottomMargin = lp4.bottomMargin = lp1.bottomMargin;
                lp3.leftMargin = lp2.leftMargin;
                lp4.rightMargin = lp2.rightMargin;

                if (!Float.isNaN(mAspectRatio)) {
                    lp1.height = (int) ((parentWidth - parentHPadding) / mAspectRatio);
                }


                int width1 = Float.isNaN(weight) ?
                        (int) ((parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin - lp2.leftMargin
                                - lp2.rightMargin) / 2.0f + 0.5f)
                        : (int) ((parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin - lp2.leftMargin
                        - lp2.rightMargin) * weight / 100 + 0.5f);
                int width2 = (int) ((parentWidth - parentHPadding - lp1.leftMargin - lp1.rightMargin - lp2.leftMargin
                        - lp2.rightMargin) - width1);
                int width3 = (int) ((width2 - lp3.rightMargin - lp4.leftMargin) / 2.0f + 0.5f);
                int width4 = (int) ((width2 - lp3.rightMargin - lp4.leftMargin - width3));

                helper.measureChild(child1,
                        MeasureSpec.makeMeasureSpec(width1 + lp1.leftMargin + lp1.rightMargin, MeasureSpec.EXACTLY),
                        helper.getChildMeasureSpec(helper.getContentHeight(), lp1.height, true));

                int height1 = child1.getMeasuredHeight();
                int height2 = (int) ((height1 - lp2.bottomMargin - lp3.topMargin) / 2.0f + 0.5f);
                int height3 = (int) ((height1 - lp2.bottomMargin - lp3.topMargin) - height2);

                helper.measureChild(child2,
                        MeasureSpec.makeMeasureSpec(width2 + lp2.leftMargin + lp2.rightMargin, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height2 + lp2.topMargin + lp2.bottomMargin, MeasureSpec.EXACTLY));

                helper.measureChild(child3,
                        MeasureSpec.makeMeasureSpec(width3 + lp3.leftMargin + lp3.rightMargin, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height3 + lp3.topMargin + lp3.bottomMargin, MeasureSpec.EXACTLY));

                helper.measureChild(child4,
                        MeasureSpec.makeMeasureSpec(width4 + lp4.leftMargin + lp4.rightMargin, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height3 + lp4.topMargin + lp4.bottomMargin, MeasureSpec.EXACTLY));

                mainConsumed = Math.max(height1 + lp1.topMargin + lp1.bottomMargin,
                        height2 + lp2.topMargin + lp2.bottomMargin + Math.max(height3 + lp3.topMargin + lp3.bottomMargin,
                                height3 + lp4.topMargin + lp4.bottomMargin)) + getVerticalMargin();

                calculateRect(mainConsumed, mAreaRect, layoutState, helper);


                layoutChild(child1, mAreaRect.left, mAreaRect.top,
                        mAreaRect.left + orientationHelper.getDecoratedMeasurementInOther(child1),
                        mAreaRect.bottom, helper);

                int childLeft2 = mAreaRect.right - orientationHelper.getDecoratedMeasurementInOther(child2);
                layoutChild(child2, childLeft2,
                        mAreaRect.top, mAreaRect.right,
                        mAreaRect.top + orientationHelper.getDecoratedMeasurement(child2),
                        helper);

                int childLeft4 = mAreaRect.right - orientationHelper.getDecoratedMeasurementInOther(child4);
                layoutChild(child4, childLeft4,
                        mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child4),
                        mAreaRect.right, mAreaRect.bottom, helper);

                layoutChild(child3, childLeft2,
                        mAreaRect.bottom - orientationHelper.getDecoratedMeasurement(child3),
                        childLeft4, mAreaRect.bottom, helper);
            } else {
                // TODO: horizontal support
            }

            handleStateOnResult(result, child1, child2, child3, child4);
        }

        result.mConsumed = mainConsumed;

        Arrays.fill(mChildrenViews, null);
    }


    private float getViewWeight(VirtualLayoutManager.LayoutParams params) {
        return getViewWeight(params, mDefaultWeight);
    }

    private float getViewWeight(VirtualLayoutManager.LayoutParams params, float defaultWeight) {
        if (params instanceof LayoutParams) {
            float weight = ((LayoutParams) params).weight;

            if (weight != 0)
                return weight;
        }

        return defaultWeight;
    }


    private int mergeLayoutMargin(int viewMargin, int layoutMargin) {
        return (viewMargin <= layoutMargin) ? 0 : viewMargin - layoutMargin;
    }

    public static class LayoutParams extends VirtualLayoutManager.LayoutParams {

        public float weight;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int positionType, int width, int height) {
            super(positionType, width, height);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(RecyclerView.LayoutParams source) {
            super(source);
        }
    }

    @Override
    protected void layoutChild(View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper) {
        super.layoutChild(child, left, top, right, bottom, helper, true);
    }

    @Override
    public int getExtraMargin(int offset, View child, boolean isLayoutEnd, boolean layoutInVertical, LayoutManagerHelper helper) {
        if (getItemCount() == 3) {
            if (offset == 1 && isLayoutEnd) {
                Log.e(TAG, "hello");
                return 0;
            }
        } else if (getItemCount() == 4) {
            if (offset == 1 && isLayoutEnd) {
                return 0;
            }
        }

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
