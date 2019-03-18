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

import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.OrientationHelperEx;
import com.alibaba.android.vlayout.R;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * {@link com.alibaba.android.vlayout.LayoutHelper} that provides basic methods
 */
public abstract class BaseLayoutHelper extends MarginLayoutHelper {

    private static final String TAG = "BaseLayoutHelper";

    public static boolean DEBUG = false;

    protected Rect mLayoutRegion = new Rect();

    View mLayoutView;

    int mBgColor;

    float mAspectRatio = Float.NaN;

    public BaseLayoutHelper() {

    }

    @Override
    public boolean isFixLayout() {
        return false;
    }

    public int getBgColor() {
        return this.mBgColor;
    }

    /**
     * Set backgroundColor for LayoutView
     *
     * @param bgColor
     */
    public void setBgColor(int bgColor) {
        this.mBgColor = bgColor;
    }

    public void setAspectRatio(float aspectRatio) {
        this.mAspectRatio = aspectRatio;
    }

    public float getAspectRatio() {
        return mAspectRatio;
    }

    private int mItemCount = 0;

    /**
     * The number of items in current layout
     *
     * @return the number of child views
     */
    @Override
    public int getItemCount() {
        return mItemCount;
    }

    @Override
    public void setItemCount(int itemCount) {
        this.mItemCount = itemCount;
    }

    /**
     * Retrieve next view and add it into layout, this is to make sure that view are added by order
     *
     * @param recycler    recycler generate views
     * @param layoutState current layout state
     * @param helper      helper to add views
     * @param result      chunk result to tell layoutManager whether layout process goes end
     * @return next view to render, null if no more view available
     */
    @Nullable
    public final View nextView(RecyclerView.Recycler recycler, LayoutStateWrapper layoutState, LayoutManagerHelper helper, LayoutChunkResult result) {
        View view = layoutState.next(recycler);
        if (view == null) {
            // if we are laying out views in scrap, this may return null which means there is
            // no more items to layout.
            if (DEBUG && !layoutState.hasScrapList()) {
                throw new RuntimeException("received null view when unexpected");
            }
            // if there is no more views can be retrieved, this layout process is finished
            result.mFinished = true;
            return null;
        }

        helper.addChildView(layoutState, view);
        return view;
    }


    @Override
    public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                             LayoutManagerHelper helper) {
        if (DEBUG) {
            Log.d(TAG, "call beforeLayout() on " + this.getClass().getSimpleName());
        }


        if (requireLayoutView()) {
            if (mLayoutView != null) {
                // TODO: recycle LayoutView
                // helper.detachChildView(mLayoutView);
            }
        } else {
            // if no layoutView is required, remove it
            if (mLayoutView != null) {
                if (mLayoutViewUnBindListener != null) {
                    mLayoutViewUnBindListener.onUnbind(mLayoutView, this);
                }
                helper.removeChildView(mLayoutView);
                mLayoutView = null;
            }
        }
    }

    /**
     * Tell whether the scrolled value is valid, if not, means it's a layout processing without scrolling
     *
     * @param scrolled value of how many pixels does scrolled
     * @return true means during a scrolling process, false means during a layout process.
     */
    protected boolean isValidScrolled(int scrolled) {
        return scrolled != Integer.MAX_VALUE && scrolled != Integer.MIN_VALUE;
    }


    @Override
    public void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                            int startPosition, int endPosition, int scrolled,
                            LayoutManagerHelper helper) {
        if (DEBUG) {
            Log.d(TAG, "call afterLayout() on " + this.getClass().getSimpleName());
        }


        if (requireLayoutView()) {
            if (isValidScrolled(scrolled) && mLayoutView != null) {
                // initial layout do reset
                mLayoutRegion.union(mLayoutView.getLeft(), mLayoutView.getTop(), mLayoutView.getRight(), mLayoutView.getBottom());
            }


            if (!mLayoutRegion.isEmpty()) {
                if (isValidScrolled(scrolled)) {
                    if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
                        mLayoutRegion.offset(0, -scrolled);
                    } else {
                        mLayoutRegion.offset(-scrolled, 0);
                    }
                }
                int contentWidth = helper.getContentWidth();
                int contentHeight = helper.getContentHeight();
                if (helper.getOrientation() == VirtualLayoutManager.VERTICAL ?
                        mLayoutRegion.intersects(0, -contentHeight / 4, contentWidth, contentHeight + contentHeight / 4) :
                        mLayoutRegion.intersects(-contentWidth / 4, 0, contentWidth + contentWidth / 4, contentHeight)) {

                    if (mLayoutView == null) {
                        mLayoutView = helper.generateLayoutView();
                        helper.addOffFlowView(mLayoutView, true);
                    }
                    //finally fix layoutRegion's height and with here to avoid visual blank
                    if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
                        mLayoutRegion.left = helper.getPaddingLeft() + mMarginLeft;
                        mLayoutRegion.right = helper.getContentWidth() - helper.getPaddingRight() - mMarginRight;
                    } else {
                        mLayoutRegion.top = helper.getPaddingTop() + mMarginTop;
                        mLayoutRegion.bottom = helper.getContentWidth() - helper.getPaddingBottom() - mMarginBottom;
                    }

                    bindLayoutView(mLayoutView);
                    return;
                } else {
                    mLayoutRegion.set(0, 0, 0, 0);
                    if (mLayoutView != null) {
                        mLayoutView.layout(0, 0, 0, 0);
                    }
                }
            }
        }

        if (mLayoutView != null) {
            if (mLayoutViewUnBindListener != null) {
                mLayoutViewUnBindListener.onUnbind(mLayoutView, this);
            }
            helper.removeChildView(mLayoutView);
            mLayoutView = null;
        }

    }

    @Override
    public void adjustLayout(int startPosition, int endPosition, LayoutManagerHelper helper) {
        if (requireLayoutView()) {
            View refer = null;
            Rect tempRect = new Rect();
            final OrientationHelperEx orientationHelper = helper.getMainOrientationHelper();
            for (int i = 0; i < helper.getChildCount(); i++) {
                refer = helper.getChildAt(i);
                int anchorPos = helper.getPosition(refer);
                if (getRange().contains(anchorPos)) {
                    if (refer.getVisibility() == View.GONE) {
                        tempRect.setEmpty();
                    } else {
                        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                            refer.getLayoutParams();
                        if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
                            tempRect.union(helper.getDecoratedLeft(refer) - params.leftMargin,
                                orientationHelper.getDecoratedStart(refer),
                                helper.getDecoratedRight(refer) + params.rightMargin,
                                orientationHelper.getDecoratedEnd(refer));
                        } else {
                            tempRect.union(orientationHelper.getDecoratedStart(refer),
                                helper.getDecoratedTop(refer) - params.topMargin, orientationHelper.getDecoratedEnd(refer),
                                helper.getDecoratedBottom(refer) + params.bottomMargin);
                        }
                    }
                }
            }
            if (!tempRect.isEmpty()) {
                mLayoutRegion.set(tempRect.left - mPaddingLeft, tempRect.top - mPaddingTop,
                    tempRect.right + mPaddingRight, tempRect.bottom + mPaddingBottom);
            } else {
                mLayoutRegion.setEmpty();
            }
            if (mLayoutView != null) {
                mLayoutView.layout(mLayoutRegion.left, mLayoutRegion.top, mLayoutRegion.right, mLayoutRegion.bottom);
            }
        }
    }

    /**
     * Called when {@link com.alibaba.android.vlayout.LayoutHelper} get dropped
     * Do default clean jobs defined by framework
     *
     * @param helper LayoutManagerHelper
     */
    @Override
    public final void clear(LayoutManagerHelper helper) {
        // remove LayoutViews if there is one
        if (mLayoutView != null) {
            if (mLayoutViewUnBindListener != null) {
                mLayoutViewUnBindListener.onUnbind(mLayoutView, this);
            }
            helper.removeChildView(mLayoutView);
            mLayoutView = null;
        }

        // call user defined
        onClear(helper);
    }

    /**
     * Called when {@link com.alibaba.android.vlayout.LayoutHelper} get dropped, do clean custom jobs
     *
     * @param helper
     */
    protected void onClear(LayoutManagerHelper helper) {

    }

    /**
     * @return
     */
    @Override
    public boolean requireLayoutView() {
        return mBgColor != 0 || mLayoutViewBindListener != null;
    }

    public abstract void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                                     LayoutStateWrapper layoutState, LayoutChunkResult result,
                                     LayoutManagerHelper helper);


    @Override
    public void doLayout(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        layoutViews(recycler, state, layoutState, result, helper);
    }

    /**
     * Helper function which do layout children and also update layoutRegion
     * but it won't consider margin in layout, so you need take care of margin if you apply margin to your layoutView
     *
     * @param child  child that will be laid
     * @param left   left position
     * @param top    top position
     * @param right  right position
     * @param bottom bottom position
     * @param helper layoutManagerHelper, help to lay child
     */
    protected void layoutChildWithMargin(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper) {
        layoutChildWithMargin(child, left, top, right, bottom, helper, false);
    }

    protected void layoutChildWithMargin(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper, boolean addLayoutRegionWithMargin) {
        helper.layoutChildWithMargins(child, left, top, right, bottom);
        if (requireLayoutView()) {
            if (addLayoutRegionWithMargin) {
                mLayoutRegion
                        .union(left - mPaddingLeft - mMarginLeft, top - mPaddingTop - mMarginTop,
                                right + mPaddingRight + mMarginRight,
                                bottom + mPaddingBottom + mMarginBottom);
            } else {
                mLayoutRegion.union(left - mPaddingLeft, top - mPaddingTop, right + mPaddingRight, bottom + mPaddingBottom);
            }
        }

    }

    /**
     * Helper function which do layout children and also update layoutRegion
     *
     * @param child  child that will be laid
     * @param left   left position
     * @param top    top position
     * @param right  right position
     * @param bottom bottom position
     * @param helper layoutManagerHelper, help to lay child
     */
    protected void layoutChild(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper) {
        layoutChild(child, left, top, right, bottom, helper, false);
    }

    protected void layoutChild(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper, boolean addLayoutRegionWithMargin) {
        helper.layoutChild(child, left, top, right, bottom);
        if (requireLayoutView()) {
            if (addLayoutRegionWithMargin) {
                mLayoutRegion
                        .union(left - mPaddingLeft - mMarginLeft, top - mPaddingTop - mMarginTop,
                                right + mPaddingRight + mMarginRight,
                                bottom + mPaddingBottom + mMarginBottom);
            } else {
                mLayoutRegion.union(left - mPaddingLeft, top - mPaddingTop, right + mPaddingRight, bottom + mPaddingBottom);
            }
        }

    }

    /**
     * Listener to handle LayoutViews, like bgImage
     */
    public interface LayoutViewBindListener {
        void onBind(View layoutView, BaseLayoutHelper baseLayoutHelper);
    }

    /**
     * Listener to handle LayoutViews, like bgImage
     */
    public interface LayoutViewUnBindListener {
        void onUnbind(View layoutView, BaseLayoutHelper baseLayoutHelper);
    }


    public interface LayoutViewHelper {

        /**
         * Implement it by maintaining a map between layoutView and image url or setting a unique tag to view. It's up to your choice.
         * @param layoutView view ready to be binded with an image
*        * @param id layoutView's identifier
         */
        void onBindViewSuccess(View layoutView, String id);
    }

    private LayoutViewUnBindListener mLayoutViewUnBindListener;

    private LayoutViewBindListener mLayoutViewBindListener;

    /**
     * Helper to decide whether call {@link LayoutViewBindListener#onBind(View, BaseLayoutHelper)}.
     * Here is a performance issue: {@link LayoutViewBindListener#onBind(View, BaseLayoutHelper)} is called during layout phase,
     * when binding image to it would cause view tree to relayout, then the same  {@link LayoutViewBindListener#onBind(View, BaseLayoutHelper)} would be called.
     * User should provide enough information to tell LayoutHelper whether image has been bind success.
     * If image has been successfully binded , no more dead loop happens.
     *
     * Of course you can handle this logic by yourself and ignore this helper.
     */
    public static class DefaultLayoutViewHelper implements LayoutViewBindListener, LayoutViewUnBindListener, LayoutViewHelper {

        private final LayoutViewBindListener mLayoutViewBindListener;

        private final LayoutViewUnBindListener mLayoutViewUnBindListener;

        public DefaultLayoutViewHelper(
            LayoutViewBindListener layoutViewBindListener,
            LayoutViewUnBindListener layoutViewUnBindListener) {
            mLayoutViewBindListener = layoutViewBindListener;
            mLayoutViewUnBindListener = layoutViewUnBindListener;
        }

        @Override
        public void onBindViewSuccess(View layoutView, String id) {
            layoutView.setTag(R.id.tag_layout_helper_bg, id);
        }

        @Override
        public void onBind(View layoutView, BaseLayoutHelper baseLayoutHelper) {
            if (layoutView.getTag(R.id.tag_layout_helper_bg) == null) {
                if (mLayoutViewBindListener != null) {
                    mLayoutViewBindListener.onBind(layoutView, baseLayoutHelper);
                }
            }
        }

        @Override
        public void onUnbind(View layoutView, BaseLayoutHelper baseLayoutHelper) {
            if (mLayoutViewUnBindListener != null) {
                mLayoutViewUnBindListener.onUnbind(layoutView, baseLayoutHelper);
            }
            layoutView.setTag(R.id.tag_layout_helper_bg, null);
        }
    }

    public void setLayoutViewHelper(DefaultLayoutViewHelper layoutViewHelper) {
        mLayoutViewBindListener = layoutViewHelper;
        mLayoutViewUnBindListener = layoutViewHelper;
    }

    /**
     * Better to use {@link #setLayoutViewHelper(DefaultLayoutViewHelper)}
     * @param bindListener
     */
    public void setLayoutViewBindListener(LayoutViewBindListener bindListener) {
        mLayoutViewBindListener = bindListener;
    }

    /**
     * Better to use {@link #setLayoutViewHelper(DefaultLayoutViewHelper)}
     * @param layoutViewUnBindListener
     */
    public void setLayoutViewUnBindListener(
            LayoutViewUnBindListener layoutViewUnBindListener) {
        mLayoutViewUnBindListener = layoutViewUnBindListener;
    }

    @Override
    public void bindLayoutView(@NonNull final View layoutView) {
        layoutView.measure(View.MeasureSpec.makeMeasureSpec(mLayoutRegion.width(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(mLayoutRegion.height(), View.MeasureSpec.EXACTLY));
        layoutView.layout(mLayoutRegion.left, mLayoutRegion.top, mLayoutRegion.right, mLayoutRegion.bottom);
        layoutView.setBackgroundColor(mBgColor);

        if (mLayoutViewBindListener != null) {
            mLayoutViewBindListener.onBind(layoutView, this);
        }

        // reset region rectangle
        mLayoutRegion.set(0, 0, 0, 0);
    }

    protected void handleStateOnResult(LayoutChunkResult result, View view) {
        if (view == null) {
            return;
        }

        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

        // Consume the available space if the view is not removed OR changed
        if (params.isItemRemoved() || params.isItemChanged()) {
            result.mIgnoreConsumed = true;
        }

        // used when search a focusable view
        result.mFocusable = result.mFocusable || view.isFocusable();

    }

    /**
     * Helper methods to handle focus states for views
     * @param result
     * @param views
     */
    protected void handleStateOnResult(LayoutChunkResult result, View[] views) {
        if (views == null) return;

        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            if (view == null) {
                continue;
            }
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

            // Consume the available space if the view is not removed OR changed
            if (params.isItemRemoved() || params.isItemChanged()) {
                result.mIgnoreConsumed = true;
            }

            // used when search a focusable view
            result.mFocusable = result.mFocusable || view.isFocusable();

            if (result.mFocusable && result.mIgnoreConsumed) {
                break;
            }
        }
    }

    protected int computeStartSpace(LayoutManagerHelper helper, boolean layoutInVertical, boolean isLayoutEnd, boolean isOverLapMargin) {
        int startSpace = 0;
        LayoutHelper lastLayoutHelper = null;
        if (helper instanceof VirtualLayoutManager) {
            lastLayoutHelper = ((VirtualLayoutManager) helper).findNeighbourNonfixLayoutHelper(this, isLayoutEnd);
        }
        MarginLayoutHelper lastMarginLayoutHelper = null;

        if (lastLayoutHelper != null && lastLayoutHelper instanceof MarginLayoutHelper) {
            lastMarginLayoutHelper = (MarginLayoutHelper) lastLayoutHelper;
        }
        if (lastLayoutHelper == this)
            return 0;

        if (!isOverLapMargin) {
            startSpace = layoutInVertical ? mMarginTop + mPaddingTop : mMarginLeft + mPaddingLeft;
        } else {
            int offset = 0;

            if (lastMarginLayoutHelper == null) {
                offset = layoutInVertical ? mMarginTop + mPaddingTop : mMarginLeft + mPaddingLeft;
            } else {
                offset = layoutInVertical
                        ? (isLayoutEnd ? calGap(lastMarginLayoutHelper.mMarginBottom, mMarginTop) : calGap(lastMarginLayoutHelper.mMarginTop, mMarginBottom))
                        : (isLayoutEnd ? calGap(lastMarginLayoutHelper.mMarginRight, mMarginLeft) : calGap(lastMarginLayoutHelper.mMarginLeft, mMarginRight));
            }
            //Log.e("huang", "computeStartSpace offset: " + offset + ", isLayoutEnd: " + isLayoutEnd + ", " + this);
            startSpace += layoutInVertical
                    ? (isLayoutEnd ? mPaddingTop : mPaddingBottom)
                    : (isLayoutEnd ? mPaddingLeft : mPaddingRight);

            startSpace += offset;
        }
        return startSpace;
    }

    protected int computeEndSpace(LayoutManagerHelper helper, boolean layoutInVertical, boolean isLayoutEnd, boolean isOverLapMargin) {
        int endSpace = layoutInVertical
                ? mMarginBottom + mPaddingBottom : mMarginLeft + mPaddingLeft;
        //Log.e("huang", "computeEndSpace offset: " + endSpace + ", isLayoutEnd: " + isLayoutEnd + ", " + this);
        //Log.e("huang", "===================\n\n");
        return endSpace;
    }

    private int calGap(int gap, int currGap) {
        if (gap < currGap) {
            return currGap - gap;
        } else {
            return 0;
        }
    }
}
