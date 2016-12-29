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

package com.alibaba.android.vlayout;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.android.vlayout.extend.InnerRecycledViewPool;

/**
 * PagerAdapter which use RecycledPool, used for nested ViewPager.
 */
public abstract class RecyclablePagerAdapter<VH extends RecyclerView.ViewHolder> extends PagerAdapter {

    private RecyclerView.Adapter<VH> mAdapter;

    private InnerRecycledViewPool mRecycledViewPool;


    public RecyclablePagerAdapter(RecyclerView.Adapter<VH> adapter, RecyclerView.RecycledViewPool pool) {
        this.mAdapter = adapter;
        if (pool instanceof InnerRecycledViewPool) {
            this.mRecycledViewPool = (InnerRecycledViewPool) pool;
        } else {
            this.mRecycledViewPool = new InnerRecycledViewPool(pool);
        }
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
        //为了解决在轮播卡片的 item 复用过程中，itemView 的 layoutParams 复用造成 layout 错误,这里要提供一个新的 layoutParams。
        //主要是 ViewPager.LayoutParams.widthFactor出现错乱
        container.addView(holder.itemView, new ViewPager.LayoutParams());

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


