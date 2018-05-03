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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.android.vlayout.layout.SingleLayoutHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static android.support.v7.widget.RecyclerView.NO_ID;

/**
 * Adapter delegates its responsibility to sub adapters
 *
 * @author villadora
 * @since 1.0.0
 */
public class DelegateAdapter extends VirtualLayoutAdapter<RecyclerView.ViewHolder> {

    @Nullable
    private AtomicInteger mIndexGen;

    private int mIndex = 0;

    private final boolean mHasConsistItemType;

    private SparseArray<Adapter> mItemTypeAry = new SparseArray<>();

    @NonNull
    private final List<Pair<AdapterDataObserver, Adapter>> mAdapters = new ArrayList<>();

    private int mTotal = 0;

    private final SparseArray<Pair<AdapterDataObserver, Adapter>> mIndexAry = new SparseArray<>();

    private long[] cantorReverse = new long[2];

    /**
     * Delegate Adapter merge multi sub adapters, default is thread-unsafe
     *
     * @param layoutManager layoutManager
     */
    public DelegateAdapter(VirtualLayoutManager layoutManager) {
        this(layoutManager, false, false);
    }

    /**
     * @param layoutManager      layoutManager
     * @param hasConsistItemType whether sub adapters itemTypes are consistent
     */
    public DelegateAdapter(VirtualLayoutManager layoutManager, boolean hasConsistItemType) {
        this(layoutManager, hasConsistItemType, false);
    }

    /**
     * @param layoutManager      layoutManager
     * @param hasConsistItemType whether sub adapters itemTypes are consistent
     * @param threadSafe         tell whether your adapter is thread-safe or not
     */
    DelegateAdapter(VirtualLayoutManager layoutManager, boolean hasConsistItemType, boolean threadSafe) {
        super(layoutManager);
        if (threadSafe) {
            mIndexGen = new AtomicInteger(0);
        }

        mHasConsistItemType = hasConsistItemType;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (mHasConsistItemType) {
            Adapter adapter = mItemTypeAry.get(viewType);
            if (adapter != null) {
                return adapter.onCreateViewHolder(parent, viewType);
            }

            return null;
        }


        // reverse Cantor Function
        Cantor.reverseCantor(viewType, cantorReverse);

        int index = (int)cantorReverse[1];
        int subItemType = (int)cantorReverse[0];

        Adapter adapter = findAdapterByIndex(index);
        if (adapter == null) {
            return null;
        }

        return adapter.onCreateViewHolder(parent, subItemType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
        if (pair == null) {
            return;
        }

        pair.second.onBindViewHolder(holder, position - pair.first.mStartPosition);
        pair.second.onBindViewHolderWithOffset(holder, position - pair.first.mStartPosition, position);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
        if (pair == null) {
            return;
        }
        pair.second.onBindViewHolder(holder, position - pair.first.mStartPosition, payloads);
        pair.second.onBindViewHolderWithOffset(holder, position - pair.first.mStartPosition, position, payloads);

    }

    @Override
    public int getItemCount() {
        return mTotal;
    }

    /**
     * Big integer of itemType returned by delegated adapter may lead to failed
     *
     * @param position item position
     * @return integer represent item view type
     */
    @Override
    public int getItemViewType(int position) {
        Pair<AdapterDataObserver, Adapter> p = findAdapterByPosition(position);
        if (p == null) {
            return RecyclerView.INVALID_TYPE;
        }

        int subItemType = p.second.getItemViewType(position - p.first.mStartPosition);

        if (subItemType < 0) {
            // negative integer, invalid, just return
            return subItemType;
        }

        if (mHasConsistItemType) {
            mItemTypeAry.put(subItemType, p.second);
            return subItemType;
        }


        int index = p.first.mIndex;

        return (int) Cantor.getCantor(subItemType, index);
    }


    @Override
    public long getItemId(int position) {
        Pair<AdapterDataObserver, Adapter> p = findAdapterByPosition(position);

        if (p == null) {
            return NO_ID;
        }

        long itemId = p.second.getItemId(position - p.first.mStartPosition);

        if (itemId < 0) {
            return NO_ID;
        }

        int index = p.first.mIndex;
        /*
         * Now we have a pairing function problem, we use cantor pairing function for itemId.
         */
        return Cantor.getCantor(index, itemId);
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        // do nothing
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        int position = holder.getPosition();
        if (position >= 0) {
            Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
            if (pair != null) {
                pair.second.onViewRecycled(holder);
            }
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int position = holder.getPosition();
        if (position >= 0) {
            Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
            if (pair != null) {
                pair.second.onViewAttachedToWindow(holder);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getPosition();
        if (position >= 0) {
            Pair<AdapterDataObserver, Adapter> pair = findAdapterByPosition(position);
            if (pair != null) {
                pair.second.onViewDetachedFromWindow(holder);
            }
        }
    }


    /**
     * You can not set layoutHelpers to delegate adapter
     */
    @Deprecated
    @Override
    public void setLayoutHelpers(List<LayoutHelper> helpers) {
        throw new UnsupportedOperationException("DelegateAdapter doesn't support setLayoutHelpers directly");
    }


    public void setAdapters(@Nullable List<Adapter> adapters) {
        clear();

        if (adapters == null) {
            adapters = Collections.emptyList();
        }

        List<LayoutHelper> helpers = new LinkedList<>();

        boolean hasStableIds = true;
        mTotal = 0;

        Pair<AdapterDataObserver, Adapter> pair;
        for (Adapter adapter : adapters) {
            // every adapter has an unique index id
            AdapterDataObserver observer = new AdapterDataObserver(mTotal, mIndexGen == null ? mIndex++ : mIndexGen.incrementAndGet());
            adapter.registerAdapterDataObserver(observer);
            hasStableIds = hasStableIds && adapter.hasStableIds();
            LayoutHelper helper = adapter.onCreateLayoutHelper();

            helper.setItemCount(adapter.getItemCount());
            mTotal += helper.getItemCount();
            helpers.add(helper);
            pair = Pair.create(observer, adapter);
            mIndexAry.put(observer.mIndex, pair);
            mAdapters.add(pair);
        }

        if (!hasObservers()) {
            super.setHasStableIds(hasStableIds);
        }
        super.setLayoutHelpers(helpers);
    }

    /**
     * Add adapters in <code>position</code>
     *
     * @param position the index where adapters added
     * @param adapters adapters
     */
    public void addAdapters(int position, @Nullable List<Adapter> adapters) {
        if (adapters == null || adapters.size() == 0) {
            return;
        }
        if (position < 0) {
            position = 0;
        }

        if (position > mAdapters.size()) {
            position = mAdapters.size();
        }

        List<Adapter> newAdapter = new ArrayList<>();
        Iterator<Pair<AdapterDataObserver, Adapter>> itr = mAdapters.iterator();
        while (itr.hasNext()) {
            Pair<AdapterDataObserver, Adapter> pair = itr.next();
            Adapter theOrigin = pair.second;
            newAdapter.add(theOrigin);
        }
        for (Adapter adapter : adapters) {
            newAdapter.add(position, adapter);
            position++;
        }
        setAdapters(newAdapter);
    }

    /**
     * Append adapters to the end
     *
     * @param adapters adapters will be appended
     */
    public void addAdapters(@Nullable List<Adapter> adapters) {
        addAdapters(mAdapters.size(), adapters);
    }

    public void addAdapter(int position, @Nullable Adapter adapter) {
        addAdapters(position, Collections.singletonList(adapter));
    }

    public void addAdapter(@Nullable Adapter adapter) {
        addAdapters(Collections.singletonList(adapter));
    }

    public void removeFirstAdapter() {
        if (mAdapters != null && !mAdapters.isEmpty()) {
            Adapter targetAdatper = mAdapters.get(0).second;
            removeAdapter(targetAdatper);
        }
    }

    public void removeLastAdapter() {
        if (mAdapters != null && !mAdapters.isEmpty()) {
            Adapter targetAdatper = mAdapters.get(mAdapters.size() - 1).second;
            removeAdapter(targetAdatper);
        }
    }

    public void removeAdapter(int adapterIndex) {
        if (adapterIndex >= 0 && adapterIndex < mAdapters.size()) {
            Adapter targetAdatper = mAdapters.get(adapterIndex).second;
            removeAdapter(targetAdatper);
        }
    }

    public void removeAdapter(@Nullable Adapter targetAdapter) {
        if (targetAdapter == null) {
            return;
        }
        removeAdapters(Collections.singletonList(targetAdapter));
    }

    public void removeAdapters(@Nullable List<Adapter> targetAdapters) {
        if (targetAdapters == null || targetAdapters.isEmpty()) {
            return;
        }
        List<LayoutHelper> helpers = new LinkedList<>(super.getLayoutHelpers());
        for (int i = 0, size = targetAdapters.size(); i < size; i++) {
            Adapter one = targetAdapters.get(i);
            Iterator<Pair<AdapterDataObserver, Adapter>> itr = mAdapters.iterator();
            while (itr.hasNext()) {
                Pair<AdapterDataObserver, Adapter> pair = itr.next();
                Adapter theOther = pair.second;
                if (theOther.equals(one)) {
                    theOther.unregisterAdapterDataObserver(pair.first);
                    final int position = findAdapterPositionByIndex(pair.first.mIndex);
                    if (position >= 0 && position < helpers.size()) {
                        helpers.remove(position);
                    }
                    itr.remove();
                    break;
                }
            }
        }

        List<Adapter> newAdapter = new ArrayList<>();
        Iterator<Pair<AdapterDataObserver, Adapter>> itr = mAdapters.iterator();
        while (itr.hasNext()) {
            newAdapter.add(itr.next().second);
        }
        setAdapters(newAdapter);
    }

    public void clear() {
        mTotal = 0;
        mIndex = 0;
        if (mIndexGen != null) {
            mIndexGen.set(0);
        }
        mLayoutManager.setLayoutHelpers(null);

        for (Pair<AdapterDataObserver, Adapter> p : mAdapters) {
            p.second.unregisterAdapterDataObserver(p.first);
        }


        mItemTypeAry.clear();
        mAdapters.clear();
        mIndexAry.clear();
    }

    public int getAdaptersCount() {
        return mAdapters == null ? 0 : mAdapters.size();
    }

    /**
     * @param absoultePosition
     * @return the relative position in sub adapter by the absoulte position in DelegaterAdapter. Return -1 if no sub adapter founded.
     */
    public int findOffsetPosition(int absoultePosition) {
        Pair<AdapterDataObserver, Adapter> p = findAdapterByPosition(absoultePosition);
        if (p == null) {
            return -1;
        }
        int subAdapterPosition = absoultePosition - p.first.mStartPosition;
        return subAdapterPosition;
    }

    @Nullable
    public Pair<AdapterDataObserver, Adapter> findAdapterByPosition(int position) {
        final int count = mAdapters.size();
        if (count == 0) {
            return null;
        }

        int s = 0, e = count - 1, m;
        Pair<AdapterDataObserver, Adapter> rs = null;

        // binary search range
        while (s <= e) {
            m = (s + e) / 2;
            rs = mAdapters.get(m);
            int endPosition = rs.first.mStartPosition + rs.second.getItemCount() - 1;

            if (rs.first.mStartPosition > position) {
                e = m - 1;
            } else if (endPosition < position) {
                s = m + 1;
            } else if (rs.first.mStartPosition <= position && endPosition >= position) {
                break;
            }

            rs = null;
        }

        return rs;
    }


    public int findAdapterPositionByIndex(int index) {
        Pair<AdapterDataObserver, Adapter> rs = mIndexAry.get(index);
        return rs == null ? -1 : mAdapters.indexOf(rs);
    }

    public Adapter findAdapterByIndex(int index) {
        Pair<AdapterDataObserver, Adapter> rs = mIndexAry.get(index);
        return rs.second;
    }

    protected class AdapterDataObserver extends RecyclerView.AdapterDataObserver {
        int mStartPosition;

        int mIndex = -1;

        public AdapterDataObserver(int startPosition, int index) {
            this.mStartPosition = startPosition;
            this.mIndex = index;
        }

        public void updateStartPositionAndIndex(int startPosition, int index) {
            this.mStartPosition = startPosition;
            this.mIndex = index;
        }

        public int getStartPosition() {
            return mStartPosition;
        }

        public int getIndex() {
            return mIndex;
        }

        private boolean updateLayoutHelper() {
            if (mIndex < 0) {
                return false;
            }

            final int idx = findAdapterPositionByIndex(mIndex);
            if (idx < 0) {
                return false;
            }

            Pair<AdapterDataObserver, Adapter> p = mAdapters.get(idx);
            List<LayoutHelper> helpers = new LinkedList<>(getLayoutHelpers());
            LayoutHelper helper = helpers.get(idx);

            if (helper.getItemCount() != p.second.getItemCount()) {
                // if itemCount changed;
                helper.setItemCount(p.second.getItemCount());

                mTotal = mStartPosition + p.second.getItemCount();

                for (int i = idx + 1; i < mAdapters.size(); i++) {
                    Pair<AdapterDataObserver, Adapter> pair = mAdapters.get(i);
                    // update startPosition for adapters in following
                    pair.first.mStartPosition = mTotal;
                    mTotal += pair.second.getItemCount();
                }

                // set helpers to refresh range
                DelegateAdapter.super.setLayoutHelpers(helpers);
            }
            return true;
        }

        @Override
        public void onChanged() {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemRangeRemoved(mStartPosition + positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemRangeInserted(mStartPosition + positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemMoved(mStartPosition + fromPosition, mStartPosition + toPosition);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemRangeChanged(mStartPosition + positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            if (!updateLayoutHelper()) {
                return;
            }
            notifyItemRangeChanged(mStartPosition + positionStart, itemCount, payload);
        }
    }

    /**
     * return an adapter that only contains one item, and using SimpleLayoutHelper
     *
     * @param view the only view, no binding is required
     * @return adapter
     */
    public static Adapter<? extends RecyclerView.ViewHolder> simpleAdapter(@NonNull View view) {
        return new SimpleViewAdapter(view);
    }

    /**
     * Return an adapter that only contains on item and using given layoutHelper
     *
     * @param view         the only view, no binding is required
     * @param layoutHelper layoutHelper that adapter used
     * @return adapter
     */
    public static Adapter<? extends RecyclerView.ViewHolder> simpleAdapter(@NonNull View view, @NonNull LayoutHelper layoutHelper) {
        return new SimpleViewAdapter(view, layoutHelper);
    }


    static class SimpleViewHolder extends RecyclerView.ViewHolder {

        public SimpleViewHolder(View view) {
            super(view);
        }
    }

    static class SimpleViewAdapter extends Adapter<RecyclerView.ViewHolder> {

        private View mView;

        private LayoutHelper mLayoutHelper;

        public SimpleViewAdapter(@NonNull View view, @NonNull LayoutHelper layoutHelper) {
            this.mView = view;
            this.mLayoutHelper = layoutHelper;
        }

        public SimpleViewAdapter(@NonNull View view) {
            this(view, new SingleLayoutHelper());
        }

        @Override
        public LayoutHelper onCreateLayoutHelper() {
            return mLayoutHelper;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SimpleViewHolder(mView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }


    public static abstract class Adapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
        public abstract LayoutHelper onCreateLayoutHelper();

        protected void onBindViewHolderWithOffset(VH holder, int position, int offsetTotal) {

        }

        protected void onBindViewHolderWithOffset(VH holder, int position, int offsetTotal, List<Object> payloads) {
            onBindViewHolderWithOffset(holder, position, offsetTotal);
        }
    }

}
