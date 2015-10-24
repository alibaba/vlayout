package com.alibaba.android.vlayout;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Created by villadora on 15/8/6.
 */
class VirtualLayoutView extends RecyclerView {

    private VirtualLayoutAdapter mAdapter;

    public VirtualLayoutView(Context context) {
        super(context);
    }

    public VirtualLayoutView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VirtualLayoutView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);

    }
}
