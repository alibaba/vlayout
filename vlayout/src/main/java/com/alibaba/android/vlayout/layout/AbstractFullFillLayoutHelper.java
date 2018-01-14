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
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.Range;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;


/**
 * 'FullFill' means that all child views it is responsible for should be layouted or recycled at once.
 * Otherwise the inner layout state will enter into chaos.
 */
public abstract class AbstractFullFillLayoutHelper extends BaseLayoutHelper {

    private static final String TAG = "FullFillLayoutHelper";

    protected boolean hasHeader = false;

    protected boolean hasFooter = false;

    protected boolean mLayoutWithAnchor = false;

    protected int getAllChildren(View[] toFill,
                                 RecyclerView.Recycler recycler, LayoutStateWrapper layoutState,
                                 LayoutChunkResult result, LayoutManagerHelper helper) {

        final boolean layingOutInPrimaryDirection = layoutState.getItemDirection() == LayoutStateWrapper.ITEM_DIRECTION_TAIL;

        int count = 0;
        int firstPos = layingOutInPrimaryDirection ? getRange().getLower() : getRange().getUpper();
        final int curPos = layoutState.getCurrentPosition();

        if (layingOutInPrimaryDirection ? (curPos > firstPos) : (curPos > firstPos)) {
            // do ugly bug fix now
            Log.w(TAG, "Please handle strange order views carefully");
        }

        while (count < toFill.length) {
            if (isOutOfRange(layoutState.getCurrentPosition()))
                break;

            View view = nextView(recycler, layoutState, helper, result);
            if (view == null) {
                break;
            }

            toFill[count] = view;

            // normalize layout params
            LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams == null) {
                view.setLayoutParams(generateDefaultLayoutParams());
            } else if (!checkLayoutParams(layoutParams)) {
                view.setLayoutParams(generateLayoutParams(layoutParams));
            }

            count++;
        }

        if (count > 0 && !layingOutInPrimaryDirection) {
            // reverse array
            int s = 0, e = count - 1;
            while (s < e) {
                View temp = toFill[s];
                toFill[s] = toFill[e];
                toFill[e] = temp;
                s++;
                e--;
            }
        }

        return count;
    }

    private LayoutManagerHelper mTempLayoutHelper;

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                            LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        mTempLayoutHelper = helper;

        doLayoutView(recycler, state, layoutState, result, helper);

        mTempLayoutHelper = null;
    }

    protected void doLayoutView(RecyclerView.Recycler recycler, RecyclerView.State state,
                                LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {

    }

    // Mock measure/layout process in ViewGroup
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {

    }

    @Override
    public void checkAnchorInfo(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo, LayoutManagerHelper helper) {
        if (anchorInfo.layoutFromEnd) {
            if (!hasFooter) {
                anchorInfo.position = getRange().getUpper();
            } else {
                //keep the previously calculated position
            }
        } else {
            if (!hasHeader) {
                anchorInfo.position = getRange().getLower();
            } else {
                //keep the previously calculated position
            }
        }
        mLayoutWithAnchor = true;
    }

    @Override
    public void afterLayout(Recycler recycler, State state, int startPosition, int endPosition, int scrolled,
        LayoutManagerHelper helper) {
        super.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);
        mLayoutWithAnchor = false;
    }

    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;
        if (layoutInVertical) {
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

    protected boolean checkLayoutParams(LayoutParams p) {
        return true;
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new LayoutParams(p);
    }


    @Override
    public boolean isRecyclable(int childPos, int startIndex, int endIndex, LayoutManagerHelper helper, boolean fromStart) {
        Range<Integer> range = getRange();
        if (range.contains(childPos)) {
            if (hasHeader && childPos == getRange().getLower()) {
                return true;
            }
            if (hasFooter && childPos == getRange().getUpper()) {
                return true;
            }
            Range<Integer> childRange = Range.create(range.getLower() + (hasHeader ? 1 : 0),
                range.getUpper() - (hasFooter ? 1 : 0));
            return Range.create(startIndex, endIndex).contains(childRange);
        } else {
            Log.w(TAG, "Child item not match");
            return true;
        }
    }

    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    public void setHasFooter(boolean hasFooter) {
        this.hasFooter = hasFooter;
    }

    protected void calculateRect(int mainAxisSize, Rect areaRect, LayoutStateWrapper layoutState, LayoutManagerHelper helper) {
        if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
            areaRect.left = helper.getPaddingLeft() + mMarginLeft + mPaddingLeft;
            areaRect.right = helper.getContentWidth() - helper.getPaddingRight() - mMarginRight - mPaddingRight;

            // whether this layout pass is layout to start or to end
            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                // fill start, from bottom to top
                areaRect.bottom = layoutState.getOffset() - (mLayoutWithAnchor ? 0 : (hasFooter ? 0 : mMarginBottom + mPaddingBottom));
                areaRect.top = areaRect.bottom - mainAxisSize;
            } else {
                areaRect.top = layoutState.getOffset() + (mLayoutWithAnchor ? 0 : (hasHeader ? 0 : mMarginTop + mPaddingTop));
                areaRect.bottom = areaRect.top + mainAxisSize;
            }
        } else {
            areaRect.top = helper.getPaddingTop() + mMarginTop + mPaddingTop;
            areaRect.bottom = helper.getContentHeight() - helper.getPaddingBottom() - mMarginBottom - mPaddingBottom;

            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                areaRect.right = layoutState.getOffset() - (mLayoutWithAnchor ? 0 :(hasFooter ? 0 : mMarginRight + mPaddingRight));
                areaRect.left = areaRect.right - mainAxisSize;
            } else {
                areaRect.left = layoutState.getOffset() + (mLayoutWithAnchor ? 0 : (hasHeader ? 0 : mMarginLeft + mPaddingLeft));
                areaRect.right = areaRect.left + mainAxisSize;
            }
        }
    }
}
