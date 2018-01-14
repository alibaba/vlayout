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

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.OrientationHelperEx;
import com.alibaba.android.vlayout.VirtualLayoutManager;

import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;
import static com.alibaba.android.vlayout.layout.FixLayoutHelper.BOTTOM_LEFT;
import static com.alibaba.android.vlayout.layout.FixLayoutHelper.BOTTOM_RIGHT;
import static com.alibaba.android.vlayout.layout.FixLayoutHelper.TOP_RIGHT;

/**
 * LayoutHelper that will be located as fix position at first layout, but its position could be changed by dragingg and dropping
 * <p>
 * Created by villadora on 15/8/28.
 */
public class FloatLayoutHelper extends FixAreaLayoutHelper {

    private static final String TAG = "FloatLayoutHelper";

    private int mTransitionX = 0;
    private int mTransitionY = 0;
    private boolean dragEnable;

    public FloatLayoutHelper() {

        this.dragEnable = true;
    }

    private int mZIndex = 1;

    private int mPos = -1;

    protected View mFixView = null;

    protected boolean mDoNormalHandle = false;

    private int mX = 0;
    private int mY = 0;
    private int mAlignType = FixLayoutHelper.TOP_LEFT;

    public void setDefaultLocation(int x, int y) {
        this.mX = x;
        this.mY = y;
    }

    public void setX(int x) {
        this.mX = x;
    }

    public void setY(int y) {
        this.mY = y;
    }

    public void setAlignType(int alignType) {
        this.mAlignType = alignType;
    }

    @Override
    public void setItemCount(int itemCount) {
        if (itemCount > 0) {
            super.setItemCount(1);
        } else {
            super.setItemCount(0);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Only start is used, use should not use this measured
     *
     * @param start position of items handled by this layoutHelper
     * @param end   should be the same as start
     */
    @Override
    public void onRangeChange(int start, int end) {
        this.mPos = start;
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                            VirtualLayoutManager.LayoutStateWrapper layoutState, LayoutChunkResult result,
                            final LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }


        // find view in currentPosition
        View view = mFixView;
        if (view == null)
            view = layoutState.next(recycler);
        else {
            layoutState.skipCurrentPosition();
        }

        if (view == null) {
            result.mFinished = true;
            return;
        }

        helper.getChildViewHolder(view).setIsRecyclable(false);

        mDoNormalHandle = state.isPreLayout();

        if (mDoNormalHandle) {
            // in PreLayout do normal layout
            helper.addChildView(layoutState, view);
        }

        mFixView = view;
        mFixView.setClickable(true);

        doMeasureAndLayout(view, helper);


        result.mConsumed = 0;
        result.mIgnoreConsumed = true;

        handleStateOnResult(result, view);

    }

    @Override
    public void setBgColor(int bgColor) {
        // disable bgColor
    }

    @Override
    public boolean requireLayoutView() {
        return false;
    }

    @Override
    public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutManagerHelper helper) {
        super.beforeLayout(recycler, state, helper);

        if (mFixView != null && helper.isViewHolderUpdated(mFixView)) {
            // remove view, not recycle
            helper.removeChildView(mFixView);
            helper.recycleView(mFixView);
            mFixView.setOnTouchListener(null);
            mFixView = null;
        }

        mDoNormalHandle = false;
    }

    @Override
    public void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                            int startPosition, int endPosition, int scrolled,
                            LayoutManagerHelper helper) {
        super.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);

        // disabled if mPos is negative number
        if (mPos < 0) return;

        if (mDoNormalHandle) {
            mFixView = null;
            return;
        }

        // Not in normal flow
        if (shouldBeDraw(startPosition, endPosition)) {
            if (mFixView != null) {
                // already capture in layoutViews phase
                // if it's not shown on screen
                // TODO: nested scrollBy
                if (mFixView.getParent() == null) {
                    helper.addFixedView(mFixView);
                    if (dragEnable) {
                        mFixView.setOnTouchListener(touchDragListener);
                    }
                    mFixView.setTranslationX(mTransitionX);
                    mFixView.setTranslationY(mTransitionY);
                } else {
                    helper.showView(mFixView);
                    // helper.removeChildView(mFixView);
                    if (dragEnable) {
                        mFixView.setOnTouchListener(touchDragListener);
                    }
                    helper.addFixedView(mFixView);
                }
            } else {
                mFixView = recycler.getViewForPosition(mPos);
                helper.getChildViewHolder(mFixView).setIsRecyclable(false);
                doMeasureAndLayout(mFixView, helper);
                helper.addFixedView(mFixView);
                mFixView.setTranslationX(mTransitionX);
                mFixView.setTranslationY(mTransitionY);
                if (dragEnable) {
                    mFixView.setOnTouchListener(touchDragListener);
                }
            }
        }

    }

    protected boolean shouldBeDraw(int startPosition, int endPosition) {
        return true;
    }


    @Nullable
    @Override
    public View getFixedView() {
        return mFixView;
    }

    @Override
    public void onClear(LayoutManagerHelper helper) {
        super.onClear(helper);
        if (mFixView != null) {
            mFixView.setOnTouchListener(null);
            helper.removeChildView(mFixView);
            helper.recycleView(mFixView);
            mFixView = null;
        }
    }

    private void doMeasureAndLayout(View view, LayoutManagerHelper helper) {
        if (view == null || helper == null) return;

        final VirtualLayoutManager.LayoutParams params = (VirtualLayoutManager.LayoutParams) view.getLayoutParams();
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
            final int widthSpec = helper.getChildMeasureSpec(
                    helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight(), params.width, !layoutInVertical);
            int heightSpec;
            if (!Float.isNaN(params.mAspectRatio) && params.mAspectRatio > 0) {
                heightSpec = helper.getChildMeasureSpec(
                        helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                        (int) (View.MeasureSpec.getSize(widthSpec) / params.mAspectRatio + 0.5f), layoutInVertical);
            } else if (!Float.isNaN(mAspectRatio) && mAspectRatio > 0) {
                heightSpec = helper.getChildMeasureSpec(
                        helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                        (int) (View.MeasureSpec.getSize(widthSpec) / mAspectRatio + 0.5f), layoutInVertical);
            } else {
                heightSpec = helper.getChildMeasureSpec(
                        helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                        params.height, layoutInVertical);
            }
            // do measurement, measure child without taking off margins, see https://github.com/alibaba/Tangram-Android/issues/81
            helper.measureChild(view, widthSpec, heightSpec);
        } else {
            int widthSpec;
            final int heightSpec = helper.getChildMeasureSpec(
                    helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(), params.height, layoutInVertical);
            if (!Float.isNaN(params.mAspectRatio) && params.mAspectRatio > 0) {
                widthSpec = helper.getChildMeasureSpec(
                        helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight(),
                        (int) (View.MeasureSpec.getSize(heightSpec) * params.mAspectRatio + 0.5f), !layoutInVertical);
            } else if (!Float.isNaN(mAspectRatio) && mAspectRatio > 0) {
                widthSpec = helper.getChildMeasureSpec(
                        helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight(),
                        (int) (View.MeasureSpec.getSize(heightSpec) * mAspectRatio + 0.5f), !layoutInVertical);
            } else {
                widthSpec = helper.getChildMeasureSpec(
                        helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight(),
                        params.width, !layoutInVertical);
            }
            // do measurement,  measure child without taking off margins, see https://github.com/alibaba/Tangram-Android/issues/81
            helper.measureChild(view, widthSpec, heightSpec);
        }


        final OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();
        int left, top, right, bottom;

        if (mAlignType == TOP_RIGHT) {
            top = helper.getPaddingTop() + mY + mAdjuster.top;
            right = helper.getContentWidth() - helper.getPaddingRight() - mX - mAdjuster.right;
            left = right - params.leftMargin - params.rightMargin - view.getMeasuredWidth();
            bottom = top + params.topMargin + params.bottomMargin + view.getMeasuredHeight();
        } else if (mAlignType == BOTTOM_LEFT) {
            left = helper.getPaddingLeft() + mX + mAdjuster.left;
            bottom = helper.getContentHeight() - helper.getPaddingBottom() - mY - mAdjuster.bottom;
            right = left + params.leftMargin + params.rightMargin + view.getMeasuredWidth();
            top = bottom - view.getMeasuredHeight() - params.topMargin - params.bottomMargin;
        } else if (mAlignType == BOTTOM_RIGHT) {
            right = helper.getContentWidth() - helper.getPaddingRight() - mX - mAdjuster.right;
            bottom = helper.getContentHeight() - helper.getPaddingBottom() - mY - mAdjuster.bottom;
            left = right - (layoutInVertical ? orientationHelper.getDecoratedMeasurementInOther(view) : orientationHelper.getDecoratedMeasurement(view));
            top = bottom - (layoutInVertical ? orientationHelper.getDecoratedMeasurement(view) : orientationHelper.getDecoratedMeasurementInOther(view));
        } else {
            // TOP_LEFT
            left = helper.getPaddingLeft() + mX + mAdjuster.left;
            top = helper.getPaddingTop() + mY + mAdjuster.top;
            right = left + (layoutInVertical ? orientationHelper.getDecoratedMeasurementInOther(view) : orientationHelper.getDecoratedMeasurement(view));
            bottom = top + (layoutInVertical ? orientationHelper.getDecoratedMeasurement(view) : orientationHelper.getDecoratedMeasurementInOther(view));
        }

        if (left < helper.getPaddingLeft() + mAdjuster.left) {
            left = helper.getPaddingLeft() + mAdjuster.left;
            right = left + (layoutInVertical ? orientationHelper.getDecoratedMeasurementInOther(view) : orientationHelper.getDecoratedMeasurement(view));
        }

        if (right > helper.getContentWidth() - helper.getPaddingRight() - mAdjuster.right) {
            right = helper.getContentWidth() - helper.getPaddingRight() - mAdjuster.right;
            left = right - params.leftMargin - params.rightMargin - view.getMeasuredWidth();
        }

        if (top < helper.getPaddingTop() + mAdjuster.top) {
            top = helper.getPaddingTop() + mAdjuster.top;
            bottom = top + (layoutInVertical ? orientationHelper.getDecoratedMeasurement(view) : orientationHelper.getDecoratedMeasurementInOther(view));
        }

        if (bottom > helper.getContentHeight() - helper.getPaddingBottom() - mAdjuster.bottom) {
            bottom = helper.getContentHeight() - helper.getPaddingBottom() - mAdjuster.bottom;
            top = bottom - (layoutInVertical ? orientationHelper.getDecoratedMeasurement(view) : orientationHelper.getDecoratedMeasurementInOther(view));
        }

        layoutChildWithMargin(view, left, top, right, bottom, helper);
    }


    private final View.OnTouchListener touchDragListener = new View.OnTouchListener() {
        private boolean isDrag;

        private int mTouchSlop;

        private int lastPosX;

        private int lastPosY;

        private int parentViewHeight;

        private int parentViewWidth;

        private int leftMargin;
        private int topMargin;
        private int rightMargin;
        private int bottomMargin;


        private final Rect parentLoction = new Rect();

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // 做初始化
            if (mTouchSlop == 0) {
                final ViewConfiguration configuration = ViewConfiguration.get(v
                        .getContext());
                mTouchSlop = configuration.getScaledTouchSlop();
                parentViewHeight = ((View) (v.getParent())).getHeight();
                parentViewWidth = ((View) (v.getParent())).getWidth();
                ((View) (v.getParent())).getGlobalVisibleRect(parentLoction);

                ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
                if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) layoutParams;
                    leftMargin = params.leftMargin;
                    topMargin = params.topMargin;
                    rightMargin = params.rightMargin;
                    bottomMargin = params.bottomMargin;
                }

            }

            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isDrag = false;
                    (v.getParent()).requestDisallowInterceptTouchEvent(true);
                    lastPosX = (int) event.getX();
                    lastPosY = (int) event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getX() - lastPosX) > mTouchSlop
                            || Math.abs(event.getY() - lastPosY) > mTouchSlop) {
                        isDrag = true;
                    }
                    if (isDrag) {
                        int posX = (int) event.getRawX();
                        int posY = (int) event.getRawY();
                        int rParentX = posX - parentLoction.left;
                        int rParentY = posY - parentLoction.top;
                        int width = v.getWidth();
                        int height = v.getHeight();
                        int translateY = rParentY - height / 2;
                        int translateX = rParentX - width / 2;
                        int curTranslateX = translateX - v.getLeft() - leftMargin - mAdjuster.left;
                        v.setTranslationX(curTranslateX);
                        int curTranslateY = translateY - v.getTop() - topMargin/* - mAdjuster.top*/;
                        if (curTranslateY + v.getHeight() + v.getTop() + bottomMargin/* + mAdjuster.bottom */ > parentViewHeight) {
                            curTranslateY = parentViewHeight - v.getHeight()
                                    - v.getTop() - bottomMargin/* - mAdjuster.bottom*/;
                        }
                        if (curTranslateY + v.getTop() - topMargin/* - mAdjuster.top*/ < 0) {
                            curTranslateY = -v.getTop() + topMargin/* + mAdjuster.top*/;
                        }
                        v.setTranslationY(curTranslateY);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    doPullOverAnimation(v);
                    (v.getParent()).requestDisallowInterceptTouchEvent(false);
                    v.setPressed(false);
                    break;
            }
            return isDrag;
        }

        private void doPullOverAnimation(final View v) {
            ObjectAnimator animator;
            if (v.getTranslationX() + v.getWidth() / 2 + v.getLeft() > parentViewWidth / 2) {
                animator = ObjectAnimator.ofFloat(v, "translationX",
                        v.getTranslationX(), parentViewWidth - v.getWidth()
                                - v.getLeft() - rightMargin - mAdjuster.right);
                mTransitionX = parentViewWidth - v.getWidth() - v.getLeft() - rightMargin - mAdjuster.right;
            } else {
                animator = ObjectAnimator.ofFloat(v, "translationX",
                        v.getTranslationX(), -v.getLeft() + leftMargin + mAdjuster.left);
                mTransitionX = -v.getLeft() + leftMargin + mAdjuster.left;
            }

            mTransitionY = (int) v.getTranslationY();
            animator.setDuration(200);
            animator.start();
        }
    };

    public void setDragEnable(boolean dragEnable) {
        this.dragEnable = dragEnable;
        if (null != mFixView) {
            mFixView.setOnTouchListener(dragEnable ? touchDragListener : null);
        }
    }
}
