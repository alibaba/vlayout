package com.alibaba.android.vlayout.layout;

import java.lang.reflect.Array;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.OrientationHelperEx;
import com.alibaba.android.vlayout.Range;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.BaseLayoutHelper.DefaultLayoutViewHelper;
import com.alibaba.android.vlayout.layout.BaseLayoutHelper.LayoutViewBindListener;
import com.alibaba.android.vlayout.layout.BaseLayoutHelper.LayoutViewUnBindListener;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * Created by longerian on 2017/5/10.
 *
 * @author longerian
 * @date 2017/05/10
 */

public class RangeStyle<T extends RangeStyle> {

    private static final boolean DEBUG = false;

    private static final String TAG = "RangeStyle";

    protected BaseLayoutHelper mLayoutHelper;

    protected T mParent;

    private int mOriginStartOffset = 0;

    private int mOriginEndOffset = 0;

    protected Range<Integer> mRange;

    //TODO update data structure
    protected ArrayMap<Range<Integer>, T> mChildren = new ArrayMap<>();

    protected int mPaddingLeft;

    protected int mPaddingRight;

    protected int mPaddingTop;

    protected int mPaddingBottom;

    protected int mMarginLeft;

    protected int mMarginRight;

    protected int mMarginTop;

    protected int mMarginBottom;

    protected Rect mLayoutRegion = new Rect();

    private View mLayoutView;

    private int mBgColor;

    private LayoutViewUnBindListener mLayoutViewUnBindListener;

    private LayoutViewBindListener mLayoutViewBindListener;

    public RangeStyle(BaseLayoutHelper layoutHelper) {
        mLayoutHelper = layoutHelper;
    }

    public RangeStyle() {
    }

    public void addChildRangeStyle(int start, int end, T rangeStyle) {
        if (start <= end && rangeStyle != null) {
            rangeStyle.setParent(this);
            rangeStyle.setOriginStartOffset(start);
            rangeStyle.setOriginEndOffset(end);
            rangeStyle.setRange(start, end);
            mChildren.put(rangeStyle.getRange(), rangeStyle);
        }
    }

    public void setParent(T rangeStyle) {
        this.mParent = rangeStyle;
    }

    /**
     * set paddings for this style
     * @param leftPadding left padding
     * @param topPadding top padding
     * @param rightPadding right padding
     * @param bottomPadding bottom padding
     */
    public void setPadding(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        mPaddingLeft = leftPadding;
        mPaddingRight = rightPadding;
        mPaddingTop = topPadding;
        mPaddingBottom = bottomPadding;
    }

    /**
     * Set margins for this style
     *
     * @param leftMargin left margin
     * @param topMargin top margin
     * @param rightMargin right margin
     * @param bottomMargin bottom margin
     */
    public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        this.mMarginLeft = leftMargin;
        this.mMarginTop = topMargin;
        this.mMarginRight = rightMargin;
        this.mMarginBottom = bottomMargin;
    }

    /**
     * Get total margin in horizontal dimension
     *
     * @return
     */
    protected int getHorizontalMargin() {
        return mMarginLeft + mMarginRight;
    }

    /**
     * Get total margin in vertical dimension
     *
     * @return
     */
    protected int getVerticalMargin() {
        return mMarginTop + mMarginBottom;
    }

    /**
     * Get total padding in horizontal dimension
     * @return
     */
    protected int getHorizontalPadding() {
        return mPaddingLeft + mPaddingRight;
    }

    /**
     * Get total padding in vertical dimension
     * @return
     */
    protected int getVerticalPadding() {
        return mPaddingTop + mPaddingBottom;
    }

    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    public int getPaddingRight() {
        return mPaddingRight;
    }

    public int getPaddingTop() {
        return mPaddingTop;
    }

    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    public int getMarginLeft() {
        return mMarginLeft;
    }

    public int getMarginRight() {
        return mMarginRight;
    }

    public int getMarginTop() {
        return mMarginTop;
    }

    public int getMarginBottom() {
        return mMarginBottom;
    }

    public void setPaddingLeft(int paddingLeft) {
        mPaddingLeft = paddingLeft;
    }

    public void setPaddingRight(int paddingRight) {
        mPaddingRight = paddingRight;
    }

    public void setPaddingTop(int paddingTop) {
        mPaddingTop = paddingTop;
    }

    public void setPaddingBottom(int paddingBottom) {
        mPaddingBottom = paddingBottom;
    }

    public void setMarginLeft(int marginLeft) {
        mMarginLeft = marginLeft;
    }

    public void setMarginRight(int marginRight) {
        mMarginRight = marginRight;
    }

    public void setMarginTop(int marginTop) {
        mMarginTop = marginTop;
    }

    public void setMarginBottom(int marginBottom) {
        mMarginBottom = marginBottom;
    }

    /**
     * Get total horizontal margin include its ancestor's and itself's.
     * @return
     */
    public int getFamilyHorizontalMargin() {
        return (mParent != null ? mParent.getFamilyHorizontalMargin() : 0) + getHorizontalMargin();
    }

    /**
     * Get total vertical margin include its ancestor's and itself's.
     * @return
     */
    public int getFamilyVerticalMargin() {
        return (mParent != null ? mParent.getFamilyVerticalMargin() : 0) + getVerticalMargin();
    }

    /**
     * Get total horizontal padding include its ancestor's and itself's.
     * @return
     */
    public int getFamilyHorizontalPadding() {
        return (mParent != null ? mParent.getFamilyHorizontalPadding() : 0) + getHorizontalPadding();
    }

    /**
     * Get total vertical padding include its ancestor's and itself's.
     * @return
     */
    public int getFamilyVerticalPadding() {
        return (mParent != null ? mParent.getFamilyVerticalPadding() : 0) + getVerticalPadding();
    }

    public int getFamilyPaddingLeft() {
        return (mParent != null ? mParent.getFamilyPaddingLeft() : 0) + mPaddingLeft;
    }

    public int getFamilyPaddingRight() {
        return (mParent != null ? mParent.getFamilyPaddingRight() : 0) + mPaddingRight;
    }

    public int getFamilyPaddingTop() {
        return (mParent != null ? mParent.getFamilyPaddingTop() : 0) + mPaddingTop;
    }

    public int getFamilyPaddingBottom() {
        return (mParent != null ? mParent.getFamilyPaddingBottom() : 0) + mPaddingBottom;
    }

    public int getFamilyMarginLeft() {
        return (mParent != null ? mParent.getFamilyMarginLeft() : 0) + mMarginLeft;
    }

    public int getFamilyMarginRight() {
        return (mParent != null ? mParent.getFamilyMarginRight() : 0) + mMarginRight;
    }

    public int getFamilyMarginTop() {
        return (mParent != null ? mParent.getFamilyMarginTop() : 0) + mMarginTop;
    }

    public int getFamilyMarginBottom() {
        return (mParent != null ? mParent.getFamilyMarginBottom() : 0) + mMarginBottom;
    }

    /**
     * Get total horizontal margin of its ancestor's.
     * @return
     */
    public int getAncestorHorizontalMargin() {
        return (mParent != null ? mParent.getAncestorHorizontalMargin() + mParent.getHorizontalMargin() : 0);
    }

    /**
     * Get total vertical margin of its ancestor's.
     * @return
     */
    public int getAncestorVerticalMargin() {
        return (mParent != null ? mParent.getAncestorVerticalMargin() + mParent.getVerticalMargin(): 0);
    }

    /**
     * Get total horizontal padding of its ancestor's.
     * @return
     */
    public int getAncestorHorizontalPadding() {
        return (mParent != null ? mParent.getAncestorHorizontalPadding() + mParent.getHorizontalPadding() : 0);
    }

    /**
     * Get total vertical padding of its ancestor's.
     * @return
     */
    public int getAncestorVerticalPadding() {
        return (mParent != null ? mParent.getAncestorVerticalPadding() + mParent.getVerticalPadding() : 0);
    }

    public int getAncestorPaddingLeft() {
        return (mParent != null ? mParent.getAncestorPaddingLeft() + mParent.getPaddingLeft() : 0);
    }

    public int getAncestorPaddingRight() {
        return (mParent != null ? mParent.getAncestorPaddingRight() + mParent.getPaddingRight() : 0);
    }

    public int getAncestorPaddingTop() {
        return (mParent != null ? mParent.getAncestorPaddingTop() + mParent.getPaddingTop() : 0);
    }

    public int getAncestorPaddingBottom() {
        return (mParent != null ? mParent.getAncestorPaddingBottom() + mParent.getPaddingBottom() : 0);
    }

    public int getAncestorMarginLeft() {
        return (mParent != null ? mParent.getAncestorMarginLeft() + mParent.getMarginLeft() : 0);
    }

    public int getAncestorMarginRight() {
        return (mParent != null ? mParent.getAncestorMarginRight() + mParent.getMarginRight() : 0);
    }

    public int getAncestorMarginTop() {
        return (mParent != null ? mParent.getAncestorMarginTop() + mParent.getMarginTop() : 0);
    }

    public int getAncestorMarginBottom() {
        return (mParent != null ? mParent.getAncestorMarginBottom() + mParent.getMarginBottom() : 0);
    }

    public int getOriginStartOffset() {
        return mOriginStartOffset;
    }

    public int getOriginEndOffset() {
        return mOriginEndOffset;
    }

    public void setOriginStartOffset(int originStartOffset) {
        mOriginStartOffset = originStartOffset;
    }

    public void setOriginEndOffset(int originEndOffset) {
        mOriginEndOffset = originEndOffset;
    }

    public Range<Integer> getRange() {
        return mRange;
    }

    public BaseLayoutHelper getLayoutHelper() {
        if (mLayoutHelper != null) {
            return mLayoutHelper;
        }
        if (mParent != null) {
            return mParent.getLayoutHelper();
        }
        return null;
    }

    public boolean isChildrenEmpty() {
        return mChildren.isEmpty();
    }

    public boolean isRoot() {
        return mParent == null;
    }

    public boolean isOutOfRange(int position) {
        return mRange != null ? !mRange.contains(position) : true;
    }

    public boolean isFirstPosition(int position) {
        return mRange != null ? mRange.getLower().intValue() == position : false;
    }

    public boolean isLastPosition(int position) {
        return mRange != null ? mRange.getUpper().intValue() == position : false;
    }

    /**
     * @param start offset relative to its parent
     * @param end offset relative to its parent
     */
    public void setRange(int start, int end) {
        mRange = Range.create(start, end);
        if (!mChildren.isEmpty()) {
            SimpleArrayMap<Range<Integer>, T> newMap = new SimpleArrayMap<>();
            for (int i = 0, size = mChildren.size(); i < size; i++) {
                T rangeStyle = mChildren.valueAt(i);
                int newStart = rangeStyle.getOriginStartOffset() + start;
                int newEnd = rangeStyle.getOriginEndOffset() + start;
                Range<Integer> newRange = Range.create(newStart, newEnd);
                newMap.put(newRange, rangeStyle);
                rangeStyle.setRange(newStart, newEnd);
            }
            mChildren.clear();
            mChildren.putAll(newMap);
        }
    }

    public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
        LayoutManagerHelper helper) {
        if (!isChildrenEmpty()) {
            for (int i = 0, size = mChildren.size(); i < size; i++) {
                RangeStyle rangeStyle = mChildren.valueAt(i);
                rangeStyle.beforeLayout(recycler, state, helper);
            }
        }
        if (requireLayoutView()) {
            if (mLayoutView != null) {
                // helper.detachChildView(mLayoutView);
            }
        } else {
            // if no layoutView is required, remove it
            if (mLayoutView != null) {
                if (mLayoutViewUnBindListener != null) {
                    mLayoutViewUnBindListener.onUnbind(mLayoutView, getLayoutHelper());
                }
                helper.removeChildView(mLayoutView);
                mLayoutView = null;
            }
        }

    }

    private boolean isValidScrolled(int scrolled) {
        return scrolled != Integer.MAX_VALUE && scrolled != Integer.MIN_VALUE;
    }

    public void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
        int startPosition, int endPosition, int scrolled,
        LayoutManagerHelper helper) {

        if (!isChildrenEmpty()) {
            for (int i = 0, size = mChildren.size(); i < size; i++) {
                RangeStyle rangeStyle = mChildren.valueAt(i);
                rangeStyle.afterLayout(recycler, state, startPosition, endPosition, scrolled, helper);
            }
        }
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

                unionChildRegion(this);

                int contentWidth = helper.getContentWidth();
                int contentHeight = helper.getContentHeight();
                if (helper.getOrientation() == VirtualLayoutManager.VERTICAL ?
                    mLayoutRegion.intersects(0, -contentHeight / 4, contentWidth, contentHeight + contentHeight / 4) :
                    mLayoutRegion.intersects(-contentWidth / 4, 0, contentWidth + contentWidth / 4, contentHeight)) {

                    if (mLayoutView == null) {
                        mLayoutView = helper.generateLayoutView();
                        helper.addBackgroundView(mLayoutView, true);
                    }
                    //finally fix layoutRegion's height and with here to avoid visual blank
                    if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
                        mLayoutRegion.left = helper.getPaddingLeft() + getFamilyMarginLeft()
                            + getAncestorPaddingLeft();
                        mLayoutRegion.right = helper.getContentWidth() - helper.getPaddingRight()
                            - getFamilyMarginRight() - getAncestorPaddingRight();
                    } else {
                        mLayoutRegion.top = helper.getPaddingTop() + getFamilyMarginTop() + getAncestorPaddingTop();
                        mLayoutRegion.bottom = helper.getContentWidth() - helper.getPaddingBottom()
                            - getFamilyMarginBottom() - getAncestorPaddingBottom();
                    }
                    bindLayoutView(mLayoutView);
                    hideLayoutViews(helper);
                    return;
                } else {
                    mLayoutRegion.set(0, 0, 0, 0);
                    if (mLayoutView != null) {
                        mLayoutView.layout(0, 0, 0, 0);
                    }
                    hideLayoutViews(helper);
                }
            }
        }
        hideLayoutViews(helper);
        if (isRoot()) {
            removeChildViews(helper, this);
        }
    }

    private void unionChildRegion(RangeStyle<T> rangeStyle) {
        if (!rangeStyle.isChildrenEmpty()) {
            for (int i = 0, size = rangeStyle.mChildren.size(); i < size; i++) {
                RangeStyle childRangeStyle = rangeStyle.mChildren.valueAt(i);
                unionChildRegion(childRangeStyle);
                if (childRangeStyle.mLayoutView != null) {
                    rangeStyle.mLayoutRegion.union(childRangeStyle.mLayoutView.getLeft(), childRangeStyle.mLayoutView.getTop(),
                            childRangeStyle.mLayoutView.getRight(), childRangeStyle.mLayoutView.getBottom());
                }
            }
        }
    }

    private void removeChildViews(LayoutManagerHelper helper, RangeStyle<T> rangeStyle) {
        if (!rangeStyle.isChildrenEmpty()) {
            for (int i = 0, size = rangeStyle.mChildren.size(); i < size; i++) {
                RangeStyle childRangeStyle = rangeStyle.mChildren.valueAt(i);
                removeChildViews(helper, childRangeStyle);
            }
        }

        if (rangeStyle.mLayoutView != null) {
            if (rangeStyle.mLayoutViewUnBindListener != null) {
                rangeStyle.mLayoutViewUnBindListener.onUnbind(rangeStyle.mLayoutView, getLayoutHelper());
            }
            helper.removeChildView(rangeStyle.mLayoutView);
            rangeStyle.mLayoutView = null;
        }
    }

    public void adjustLayout(int startPosition, int endPosition, LayoutManagerHelper helper) {
        if (!isChildrenEmpty()) {
            for (int i = 0, size = mChildren.size(); i < size; i++) {
                RangeStyle rangeStyle = mChildren.valueAt(i);
                rangeStyle.adjustLayout(startPosition, endPosition, helper);
            }
        }
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

    private void hideLayoutViews(LayoutManagerHelper helper) {
        if (isRoot()) {
            hideChildLayoutViews(helper, this);

            if (mLayoutView != null) {
                helper.hideView(mLayoutView);
            }
        }
    }

    private void hideChildLayoutViews(LayoutManagerHelper helper, RangeStyle<T> rangeStyle) {
        for (int i = 0, size = rangeStyle.mChildren.size(); i < size; i++) {
            RangeStyle childRangeStyle = rangeStyle.mChildren.valueAt(i);
            if (!childRangeStyle.isChildrenEmpty()) {
                hideChildLayoutViews(helper, childRangeStyle);
            }

            if (childRangeStyle.mLayoutView != null) {
                helper.hideView(childRangeStyle.mLayoutView);
            }
        }
    }

    public boolean requireLayoutView() {
        boolean self = mBgColor != 0 || mLayoutViewBindListener != null;
        if (!isChildrenEmpty()) {
            self |= requireChildLayoutView(this);
        }
        return self;
    }

    private boolean requireChildLayoutView(RangeStyle<T> rangeStyle) {
        boolean self = rangeStyle.mBgColor != 0 || rangeStyle.mLayoutViewBindListener != null;

        for (int i = 0, size = rangeStyle.mChildren.size(); i < size; i++) {
            RangeStyle childRangeStyle = rangeStyle.mChildren.valueAt(i);
            if (!childRangeStyle.isChildrenEmpty()) {
                self |= requireChildLayoutView(childRangeStyle);
            } else {
                return childRangeStyle.requireLayoutView();
            }
        }
        return self;
    }

    public void bindLayoutView(@NonNull final View layoutView) {
        layoutView.measure(View.MeasureSpec.makeMeasureSpec(mLayoutRegion.width(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(mLayoutRegion.height(), View.MeasureSpec.EXACTLY));
        layoutView.layout(mLayoutRegion.left, mLayoutRegion.top, mLayoutRegion.right, mLayoutRegion.bottom);
        layoutView.setBackgroundColor(mBgColor);

        if (mLayoutViewBindListener != null) {
            mLayoutViewBindListener.onBind(layoutView, getLayoutHelper());
        }

        // reset region rectangle
        mLayoutRegion.set(0, 0, 0, 0);
    }

    public void setLayoutViewHelper(DefaultLayoutViewHelper layoutViewHelper) {
        mLayoutViewBindListener = layoutViewHelper;
        mLayoutViewUnBindListener = layoutViewHelper;
    }

    public void setLayoutViewBindListener(LayoutViewBindListener bindListener) {
        mLayoutViewBindListener = bindListener;
    }

    public void setLayoutViewUnBindListener(
        LayoutViewUnBindListener layoutViewUnBindListener) {
        mLayoutViewUnBindListener = layoutViewUnBindListener;
    }

    public void setBgColor(int bgColor) {
        this.mBgColor = bgColor;
    }

    public void onClear(LayoutManagerHelper helper) {
        clearChild(helper, this);
    }

    private void clearChild(LayoutManagerHelper helper, RangeStyle<T> rangeStyle) {
        if (rangeStyle.mLayoutView != null) {
            if (rangeStyle.mLayoutViewUnBindListener != null) {
                rangeStyle.mLayoutViewUnBindListener.onUnbind(rangeStyle.mLayoutView, getLayoutHelper());
            }
            helper.removeChildView(rangeStyle.mLayoutView);
            rangeStyle.mLayoutView = null;
        }

        if (rangeStyle.mChildren.isEmpty()) {
            return;
        }

        for (int i = 0, size = rangeStyle.mChildren.size(); i < size; i++) {
            RangeStyle childRangeStyle = rangeStyle.mChildren.valueAt(i);
            clearChild(helper, childRangeStyle);
        }
    }

    public void onClearChildMap() {
        mChildren.clear();
    }

    public void layoutChild(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper, boolean addLayoutRegionWithMargin) {
        helper.layoutChildWithMargins(child, left, top, right, bottom);
        fillLayoutRegion(left, top, right, bottom, addLayoutRegionWithMargin);
    }

    protected void fillLayoutRegion(int left, int top, int right, int bottom, boolean addLayoutRegionWithMargin) {
        if (addLayoutRegionWithMargin) {
            mLayoutRegion
                .union(left - mPaddingLeft - mMarginLeft, top - mPaddingTop - mMarginTop,
                    right + mPaddingRight + mMarginRight,
                    bottom + mPaddingBottom + mMarginBottom);
        } else {
            mLayoutRegion.union(left - mPaddingLeft, top - mPaddingTop, right + mPaddingRight,
                bottom + mPaddingBottom);
        }
        if (mParent != null) {
            mParent.fillLayoutRegion(left - mPaddingLeft - mMarginLeft, top - mPaddingTop - mMarginLeft, right + mPaddingRight + mMarginRight,
                bottom + mPaddingBottom + mMarginBottom, addLayoutRegionWithMargin);
        }
    }

    private static class RangeMap<T> {

        private final static int CAPACITY = 64;

        private Class<T> mClass;

        private int lastIndex = -1;

        private int[] mOffsetMap = new int[CAPACITY];

        private T[] mCardMap = (T[])Array.newInstance(mClass, CAPACITY);

        public RangeMap(Class<T> type) {
            this.mClass = type;
        }

        public void addChild(int startOffset, int endOffset, T t) {
            int index = lastIndex + 1;
            if (index < mCardMap.length) {
                mCardMap[index] = t;
            } else {
                int oldLength = mCardMap.length;
                T[] newCardMap = (T[])Array.newInstance(mClass, oldLength * 2);
                System.arraycopy(mCardMap, 0, newCardMap, 0, oldLength);
                mCardMap = newCardMap;
                mCardMap[oldLength] = t;
                index = oldLength;

                oldLength = mOffsetMap.length;
                int[] newOffsetMap = new int[oldLength * 2];
                System.arraycopy(mOffsetMap, 0, newOffsetMap, 0, oldLength);
                mOffsetMap = newOffsetMap;
            }
            lastIndex = index;
            for (int i = startOffset; i <= endOffset; i++) {
                mOffsetMap[i] = index;
            }
        }

        public T getChild(int offset) {
            return mCardMap[mOffsetMap[offset]];
        }

    }

}
