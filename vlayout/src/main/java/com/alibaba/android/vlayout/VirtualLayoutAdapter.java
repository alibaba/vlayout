package com.alibaba.android.vlayout;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.List;

/**
 * Adapter used in VirtualLayoutManager
 */
public abstract class VirtualLayoutAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    @NonNull
    protected VirtualLayoutManager mLayoutManager;

    public VirtualLayoutAdapter(@NonNull VirtualLayoutManager layoutManager) {
        this.mLayoutManager = layoutManager;
    }

    public void setLayoutHelpers(List<LayoutHelper> helpers) {
        this.mLayoutManager.setLayoutHelpers(helpers);
    }

    @NonNull
    public List<LayoutHelper> getLayoutHelpers() {
        return this.mLayoutManager.getLayoutHelpers();
    }

}
