package com.alibaba.android.vlayout;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by villadora on 15/8/18.
 */
public abstract class LayoutHelperFinder implements Iterable<LayoutHelper> {

    abstract void setLayouts(@Nullable List<LayoutHelper> layouts);

    @Nullable
    protected abstract LayoutHelper getLayoutHelper(int position);

    @NonNull
    protected abstract List<LayoutHelper> getLayoutHelpers();


    protected abstract Iterable<LayoutHelper> reverse();

}
