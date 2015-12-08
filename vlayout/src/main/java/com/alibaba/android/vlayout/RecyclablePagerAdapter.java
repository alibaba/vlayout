package com.alibaba.android.vlayout;

import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.InnerRecycledViewPool;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by villadora on 15/12/7.
 */
public abstract class RecyclablePagerAdapter<VH extends RecyclerView.ViewHolder> extends PagerAdapter {

    private RecyclerView.Adapter<VH> mAdapter;

    private InnerRecycledViewPool mRecycledViewPool;


    public RecyclablePagerAdapter(RecyclerView.Adapter<VH> adapter, RecyclerView.RecycledViewPool pool) {
        this.mAdapter = adapter;
        this.mRecycledViewPool = new InnerRecycledViewPool(pool);
    }

    @Override
    public abstract int getCount();

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return o instanceof RecyclerView.ViewHolder && (((RecyclerView.ViewHolder) o).itemView == view);
    }

    /**
     * Get view from position
     *
     * @param container
     * @param position
     * @return
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int itemViewType = getItemViewType(position);
        RecyclerView.ViewHolder holder = mRecycledViewPool.getRecycledView(itemViewType);

        if (holder == null) {
            holder = mAdapter.createViewHolder(container, itemViewType);
        }


        onBindViewHolder((VH) holder, position);
        container.addView(holder.itemView);

        return holder;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object instanceof RecyclerView.ViewHolder) {
            RecyclerView.ViewHolder holder = (RecyclerView.ViewHolder) object;
            container.removeView(holder.itemView);
            mRecycledViewPool.putRecycledView(holder);
        }
    }


    public abstract void onBindViewHolder(VH viewHolder, int position);

    public abstract int getItemViewType(int position);
}


