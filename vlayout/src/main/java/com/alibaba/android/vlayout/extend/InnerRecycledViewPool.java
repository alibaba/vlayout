package com.alibaba.android.vlayout.extend;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This ViewPool doesn't support multi thread
 */
public final class InnerRecycledViewPool extends RecyclerView.RecycledViewPool {

    private static final String TAG = "InnerRecycledViewPool";
    private static final int DEFAULT_MAX_SIZE = 5;

    private RecyclerView.RecycledViewPool mInnerPool;


    private Map<Integer, Integer> mScrapLength = new HashMap<>();
    private SparseIntArray mMaxScrap = new SparseIntArray();

    public InnerRecycledViewPool(RecyclerView.RecycledViewPool pool) {
        this.mInnerPool = pool;
    }


    public InnerRecycledViewPool() {
        this(new RecyclerView.RecycledViewPool());
    }

    public void clear() {
        Set<Integer> viewTypes = mScrapLength.keySet();
        for (int viewType : viewTypes) {
            RecyclerView.ViewHolder holder = mInnerPool.getRecycledView(viewType);
            while (holder != null) {
                destroyViewHolder(holder);
                holder = mInnerPool.getRecycledView(viewType);
            }
        }

        mScrapLength.clear();
        super.clear();
    }

    @Override
    public void setMaxRecycledViews(int viewType, int max) {
        RecyclerView.ViewHolder holder = mInnerPool.getRecycledView(viewType);
        while (holder != null) {
            destroyViewHolder(holder);
            holder = mInnerPool.getRecycledView(viewType);
        }

        this.mMaxScrap.put(viewType, max);
        this.mScrapLength.put(viewType, 0);
        super.setMaxRecycledViews(viewType, max);
    }

    @Override
    public RecyclerView.ViewHolder getRecycledView(int viewType) {
        RecyclerView.ViewHolder holder = mInnerPool.getRecycledView(viewType);
        if (holder != null) {
            int scrapHeapSize = mScrapLength.containsKey(viewType) ? this.mScrapLength.get(viewType) : 0;
            if (scrapHeapSize > 0)
                mScrapLength.put(viewType, scrapHeapSize - 1);
        }

        return holder;
    }


    public int size() {
        int count = 0;
        for (int val : mScrapLength.values()) {
            count += val;
        }

        return count;
    }

    /**
     * This will be only run in UI Thread
     *
     * @param scrap
     */
    @SuppressWarnings("unchecked")
    public void putRecycledView(RecyclerView.ViewHolder scrap) {
        int viewType = scrap.getItemViewType();
        if (mMaxScrap.indexOfKey(viewType) < 0) {
            mMaxScrap.put(viewType, DEFAULT_MAX_SIZE);
            setMaxRecycledViews(viewType, DEFAULT_MAX_SIZE);
        }

        int scrapHeapSize = mScrapLength.containsKey(viewType) ? this.mScrapLength.get(viewType) : 0;
        if (this.mMaxScrap.get(viewType) > scrapHeapSize) {
            mInnerPool.putRecycledView(scrap);
            mScrapLength.put(viewType, scrapHeapSize + 1);
        } else {
            destroyViewHolder(scrap);
        }
    }

    private void destroyViewHolder(RecyclerView.ViewHolder holder) {
        View view = holder.itemView;
        if (view instanceof Closeable) {
            try {
                ((Closeable) view).close();
            } catch (Exception e) {
                Log.w(TAG, Log.getStackTraceString(e), e);
            }
        }
    }
}
