package com.alibaba.android.vlayout.layout;

import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.LayoutHelperFinder;
import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.Range;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import android.graphics.Rect;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.Log;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;

/**
 * Created by longerian on 2017/4/24.
 *
 * @author longerian
 * @date 2017/04/24
 */

abstract public class GroupLayoutHelper extends BaseLayoutHelper {

    private static final String TAG = "GroupLayoutHelper";

    protected ArrayMap<Range<Integer>, LayoutHelper> mNestLayoutHelpers = new ArrayMap();

    protected LayoutHelperFinder mLayoutHelperFinder;

    public void setLayoutHelperFinder(LayoutHelperFinder layoutHelperFinder) {
        mLayoutHelperFinder = layoutHelperFinder;
    }

    public void addLayoutHelper(int start, LayoutHelper layoutHelper) {
        int itemCount = layoutHelper.getItemCount();
        if (itemCount > 0) {
            layoutHelper.setRange(start, start + itemCount - 1);
            Range<Integer> range = layoutHelper.getRange();
            mNestLayoutHelpers.put(range, layoutHelper);
            layoutHelper.setParentLayoutHelper(this);
        }
    }

    @Override
    public void beforeLayout(Recycler recycler, State state, LayoutManagerHelper helper) {
        super.beforeLayout(recycler, state, helper);
        if (mLayoutHelperFinder != null) {
            for (LayoutHelper delegateLayoutHelper : mLayoutHelperFinder) {
                delegateLayoutHelper.beforeLayout(recycler, state, helper);
            }
        } else {
            for (int i = 0, size = mNestLayoutHelpers.size(); i < size; i++) {
                LayoutHelper delegateLayoutHelper = mNestLayoutHelpers.valueAt(i);
                delegateLayoutHelper.beforeLayout(recycler, state, helper);
            }
        }
    }

    @Override
    public void layoutViews(Recycler recycler, State state, LayoutStateWrapper layoutState, LayoutChunkResult result,
        LayoutManagerHelper helper) {
        int currentPosition = layoutState.getCurrentPosition();
        if (mLayoutHelperFinder != null) {
            LayoutHelper delegateLayoutHelper = mLayoutHelperFinder.getLayoutHelper(currentPosition);
            if (delegateLayoutHelper != null) {
                delegateLayoutHelper.doLayout(recycler, state, layoutState, result, helper);
                Rect delegateRegion = delegateLayoutHelper.getLayoutRegion();
                if (delegateRegion != null) {
                    mLayoutRegion.union(delegateRegion.left - mPaddingLeft, delegateRegion.top - mPaddingTop,
                        delegateRegion.right + mPaddingRight, delegateRegion.bottom + mPaddingBottom);
                }
            } else {
                Log.d(TAG, "warning, layoutHelper not found for position " + currentPosition);
            }
        } else {
            for (int i = 0, size = mNestLayoutHelpers.size(); i < size; i++) {
                Range range = mNestLayoutHelpers.keyAt(i);
                if (range.contains(Integer.valueOf(currentPosition))) {
                    Log.d(TAG, "yeah, layoutHelper found for position " + currentPosition);
                    LayoutHelper delegateLayoutHelper = mNestLayoutHelpers.valueAt(i);
                    delegateLayoutHelper.doLayout(recycler, state, layoutState, result, helper);
                    Rect delegateRegion = delegateLayoutHelper.getLayoutRegion();
                    Log.d(TAG, "delegateRegion " + delegateRegion);
                    if (delegateRegion != null) {
                        mLayoutRegion.union(delegateRegion.left - mPaddingLeft, delegateRegion.top - mPaddingTop,
                            delegateRegion.right + mPaddingRight, delegateRegion.bottom + mPaddingBottom);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void afterLayout(Recycler recycler, State state, int startPosition, int endPosition, int scrolled,
        LayoutManagerHelper helper) {
        super.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);
        if (mLayoutHelperFinder != null) {
            for (LayoutHelper delegateLayoutHelper : mLayoutHelperFinder) {
                delegateLayoutHelper.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);
            }
        } else {
            for (int i = 0, size = mNestLayoutHelpers.size(); i < size; i++) {
                LayoutHelper delegateLayoutHelper = mNestLayoutHelpers.valueAt(i);
                delegateLayoutHelper.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);
            }
        }

    }

    @Override
    public int getItemCount() {
        int totalCount = 0;
        if (mLayoutHelperFinder != null) {
            for (LayoutHelper layoutHelper : mLayoutHelperFinder) {
                totalCount += layoutHelper.getItemCount();
            }
        } else {
            for (int i = 0, size = mNestLayoutHelpers.size(); i < size; i++) {
                LayoutHelper layoutHelper = mNestLayoutHelpers.valueAt(i);
                totalCount += layoutHelper.getItemCount();
            }
        }
        return totalCount;
    }

    @Override
    public int computeAlignOffset(int offset, boolean isLayoutEnd, boolean useAnchor, LayoutManagerHelper helper) {
        final boolean layoutInVertical = helper.getOrientation() == VERTICAL;

        if (isLayoutEnd) {
            if (offset == getItemCount() - 1) {
                int innerOffset = offset - mNestLayoutHelpers.valueAt(mNestLayoutHelpers.size() - 1).getRange().getLower();
                int lastChildAlignOffset = mNestLayoutHelpers.valueAt(mNestLayoutHelpers.size() - 1)
                    .computeAlignOffset(innerOffset, isLayoutEnd,
                        useAnchor, helper);
                return (layoutInVertical ? mMarginBottom + mPaddingBottom : mMarginRight + mPaddingRight) + lastChildAlignOffset;
            }
        } else {
            if (offset == 0) {
                int firstChildAlignOffset = mNestLayoutHelpers.valueAt(0).computeAlignOffset(offset, isLayoutEnd,
                    useAnchor, helper);
                return (layoutInVertical ? -mMarginTop - mPaddingTop : -mMarginLeft - mPaddingLeft) + firstChildAlignOffset;
            }
        }

        return super.computeAlignOffset(offset, isLayoutEnd, useAnchor, helper);
    }

    private LayoutHelper findLayoutByPosition(int position) {
        return null;
    }
}
