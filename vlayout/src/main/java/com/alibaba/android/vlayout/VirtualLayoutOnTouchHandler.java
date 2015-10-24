package com.alibaba.android.vlayout;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;


class VirtualLayoutOnTouchHandler implements RecyclerView.OnItemTouchListener {

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }
}
