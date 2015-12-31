package com.alibaba.android.vlayout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Created by villadora on 15/12/30.
 */
public class FixedLayout extends ViewGroup {

    @NonNull
    private RecyclerView.LayoutManager mLayoutManager;

    public FixedLayout(@NonNull RecyclerView.LayoutManager layoutManager, Context context) {
        super(context);
        this.mLayoutManager = layoutManager;
    }



    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return mLayoutManager.generateDefaultLayoutParams();
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return mLayoutManager.generateLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof RecyclerView.LayoutParams && mLayoutManager.checkLayoutParams((RecyclerView.LayoutParams) p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return mLayoutManager.generateLayoutParams(getContext(), attrs);
    }
}
