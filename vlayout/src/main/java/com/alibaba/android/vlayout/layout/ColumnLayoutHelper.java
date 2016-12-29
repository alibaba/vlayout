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
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;

import java.util.Arrays;

import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;


public class ColumnLayoutHelper extends AbstractFullFillLayoutHelper {

    private View[] mEqViews;

    private View[] mViews;

    private Rect mTempArea = new Rect();

    private float[] mWeights = new float[0];

    public void setWeights(float[] weights) {
        if (weights != null) {
            this.mWeights = Arrays.copyOf(weights, weights.length);
        } else {
            this.mWeights = new float[0];
        }
    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state, VirtualLayoutManager.LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        // reach the end of this layout
        if (isOutOfRange(layoutState.getCurrentPosition())) {
            return;
        }

        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        final OrientationHelper orientationHelper = helper.getMainOrientationHelper();

        final int itemCount = getItemCount();

        if (mViews == null || mViews.length != itemCount) {
            mViews = new View[itemCount];
        }

        if (mEqViews == null || mEqViews.length != itemCount) {
            mEqViews = new View[itemCount];
        } else {
            Arrays.fill(mEqViews, null);
        }


        final int count = getAllChildren(mViews, recycler, layoutState, result, helper);

        if (layoutInVertical) {
            // TODO: only handle vertical layout now
            int maxVMargin = 0;
            int lastHMargin = 0;
            int totalMargin = 0;

            for (int i = 0; i < count; i++) {
                View view = mViews[i];
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                if (layoutParams instanceof RecyclerView.LayoutParams) {
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) layoutParams;

                    params.leftMargin = Math.max(lastHMargin, params.leftMargin);

                    totalMargin += params.leftMargin;

                    if (i != count - 1) {
                        // not last item
                        lastHMargin = params.rightMargin;
                        params.rightMargin = 0;
                    } else {
                        totalMargin += params.rightMargin;
                    }

                    maxVMargin = Math.max(maxVMargin, params.topMargin + params.bottomMargin);
                }
            }


            final int totalWidth = helper.getContentWidth() - helper.getPaddingLeft() - helper.getPaddingRight() - getHorizontalMargin();
            final int availableWidth = totalWidth - totalMargin;

            int usedWidth = 0;
            int minHeight = Integer.MAX_VALUE;

            int uniformHeight = -1;
            if (!Float.isNaN(mAspectRatio)) {
                uniformHeight = (int) (totalWidth / mAspectRatio);
            }


            int eqSize = 0;

            for (int i = 0; i < count; i++) {
                View view = mViews[i];
                ViewGroup.LayoutParams params = view.getLayoutParams();
                final int heightSpec = helper.getChildMeasureSpec(
                        helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                        uniformHeight > 0 ? uniformHeight : params.height, true);
                if (mWeights != null && i < mWeights.length && !Float.isNaN(mWeights[i]) && mWeights[i] >= 0) {

                    // calculate width with weight in percentage
                    int resizeWidth = (int) (mWeights[i] * 1.0f / 100 * availableWidth + 0.5f);

                    helper.measureChild(view, View.MeasureSpec.makeMeasureSpec(resizeWidth, View.MeasureSpec.EXACTLY), heightSpec);

                    // add width into usedWidth
                    usedWidth += resizeWidth;

                    // find minHeight
                    minHeight = Math.min(minHeight, view.getMeasuredHeight());
                } else {
                    mEqViews[eqSize++] = view;
                }
            }


            for (int i = 0; i < eqSize; i++) {
                View view = mEqViews[i];
                ViewGroup.LayoutParams params = view.getLayoutParams();
                final int heightSpec = helper.getChildMeasureSpec(
                        helper.getContentHeight() - helper.getPaddingTop() - helper.getPaddingBottom(),
                        uniformHeight > 0 ? uniformHeight : params.height, true);
                //cols只配置了部分，那么剩下的组件平分剩下的区域
                helper.measureChild(view,
                        View.MeasureSpec.makeMeasureSpec(
                                (int) ((availableWidth - usedWidth) * 1.0f / eqSize + 0.5f), View.MeasureSpec.EXACTLY),
                        heightSpec);

                // find minHeight
                minHeight = Math.min(minHeight, view.getMeasuredHeight());
            }

            // uniform all views into min height
            for (int i = 0; i < count; i++) {
                View view = mViews[i];
                if (view.getMeasuredHeight() != minHeight) {
                    //noinspection ResourceType
                    helper.measureChild(view, View.MeasureSpec.makeMeasureSpec(view.getMeasuredWidth(), View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(minHeight, View.MeasureSpec.EXACTLY));
                }
            }

            result.mConsumed = minHeight + maxVMargin + getVerticalMargin();

            calculateRect(result.mConsumed, mTempArea, layoutState, helper);

            // do layout
            int left = mTempArea.left;
            for (int i = 0; i < count; i++) {
                View view = mViews[i];
                int top = mTempArea.top, bottom = mTempArea.bottom;

                int right = left + orientationHelper.getDecoratedMeasurementInOther(view);

                layoutChild(view, left, top, right, bottom, helper);

                left = right;
            }

        }

        Arrays.fill(mViews, null);
        Arrays.fill(mEqViews, null);
    }

    @Override
    public void checkAnchorInfo(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {
        if (anchorInfo.layoutFromEnd) {
            anchorInfo.position = getRange().getUpper();
        } else {
            anchorInfo.position = getRange().getLower();
        }
    }
}
