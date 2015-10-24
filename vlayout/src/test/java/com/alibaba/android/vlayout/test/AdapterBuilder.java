package com.alibaba.android.vlayout.test;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

/**
 * Created by villadora on 15/8/13.
 */
public class AdapterBuilder {
    public static AdapterBuilder newBuilder(Context context) {
        return new AdapterBuilder();
    }

    public RecyclerView.Adapter build() {
        return null;
    }
}
