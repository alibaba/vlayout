package com.alibaba.android.vlayout;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * LayoutHelperFinder provides as repository of LayoutHelpers
 */
public abstract class LayoutHelperFinder implements Iterable<LayoutHelper> {

    /**
     * Put layouts into the finder
     */
    abstract void setLayouts(@Nullable List<LayoutHelper> layouts);

    /**
     * Get layoutHelper at given position
     *
     * @param position
     * @return
     */
    @Nullable
    protected abstract LayoutHelper getLayoutHelper(int position);

    /**
     * Get all layoutHelpers
     *
     * @return
     */
    @NonNull
    protected abstract List<LayoutHelper> getLayoutHelpers();

    /**
     * Get iterator that in reverse order
     *
     * @return
     */
    protected abstract Iterable<LayoutHelper> reverse();

}
