package com.alibaba.android.vlayout.layout;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;

/**
 * Created by villadora on 15/10/28.
 */
public class StaggeredGridLayoutHelper extends BaseLayoutHelper {

    private int mLanes = 0;

    public StaggeredGridLayoutHelper(int lanes) {
        this.mLanes = lanes;

    }

    @Override
    public void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                            VirtualLayoutManager.LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {

    }


    @Override
    public void onScrollStateChanged(int state) {

    }

    @Override
    public void checkAnchorInfo(RecyclerView.State state, VirtualLayoutManager.AnchorInfoWrapper anchorInfo) {
        super.checkAnchorInfo(state, anchorInfo);
    }

    @Override
    public void onSaveState(Bundle bundle) {
        super.onSaveState(bundle);
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
    }

}
