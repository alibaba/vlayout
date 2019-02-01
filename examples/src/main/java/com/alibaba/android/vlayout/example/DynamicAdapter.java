package com.alibaba.android.vlayout.example;

import android.support.v4.util.ArraySet;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.LayoutHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * <>
 *
 * @author lixi
 * @date 2019/1/31
 */
public class DynamicAdapter<VH extends RecyclerView.ViewHolder> extends DelegateAdapter.Adapter<VH> {

    public static final int CLASS_CAST_EXECPTION = -10086;
    private static final SparseArrayCompat<Set<String>> CACHE = new SparseArrayCompat<>();

    public static SparseArrayCompat<Set<String>> getCache() {
        return CACHE;
    }

    private static void addToCache(int viewType, Class clazz) {
        addToCache(viewType, clazz.getSimpleName());
    }


    private static void addToCache(int viewType, String clazzName) {
        SparseArrayCompat<Set<String>> cache = getCache();
        Set<String> history = cache.get(viewType);
        if (null == history) {
            history = new ArraySet<>();
        }
        history.add(clazzName);
        cache.put(viewType, history);
    }

    private int mItemCount;
    private LayoutHelper mLayoutHelper;
    private Class<VH> mClazz;

    public DynamicAdapter(LayoutHelper helper, int itemCount, Class<VH> clazz) {
        this.mItemCount = itemCount;
        this.mLayoutHelper = helper;
        this.mClazz = clazz;
    }

    @Override
    public int getItemCount() {
        return mItemCount;
    }

    @Override
    public LayoutHelper onCreateLayoutHelper() {
        return mLayoutHelper;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item, parent, false);

        itemView.setMinimumHeight((int) (parent.getResources().getDisplayMetrics().density * 72));

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
        );
        itemView.setLayoutParams(params);

        try {
            Constructor<VH> constructor = mClazz.getConstructor(View.class);
            return constructor.newInstance(itemView);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {

    }


    @Override
    protected void onBindViewHolderWithOffset(VH holder, int position, int offsetTotal) {
        super.onBindViewHolderWithOffset(holder, position, offsetTotal);
        addToCache(holder.getItemViewType(), mClazz);


        StringBuilder title = new StringBuilder(mClazz.getSimpleName())
                .append("\n")
                .append("[Position]:")
                .append(offsetTotal)
                .append("\n")
                .append("[ViewType]:")
                .append(holder.getItemViewType());

        if (!mClazz.isAssignableFrom(holder.getClass())) {
            addToCache(CLASS_CAST_EXECPTION, holder.getClass().getSimpleName() + " cast to " + mClazz.getSimpleName());

            title = title.append("\n").append(mClazz.getSimpleName())
                    .append(" cannot be cast to ").append(holder.getClass().getSimpleName());
        }

        ((TextView) holder.itemView.findViewById(R.id.title)).setText(title);
    }


}
