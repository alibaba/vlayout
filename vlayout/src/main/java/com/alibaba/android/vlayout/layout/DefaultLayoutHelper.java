package com.alibaba.android.vlayout.layout;

import com.alibaba.android.vlayout.LayoutHelper;

/**
 * Created by villadora on 15/8/12.
 */
public class DefaultLayoutHelper extends LinearLayoutHelper {

    public static LayoutHelper newHelper(int itemCount) {
        DefaultLayoutHelper helper = new DefaultLayoutHelper();
        helper.setItemCount(itemCount);
        return helper;
    }

    @Override
    public boolean isOutOfRange(int position) {
        return false;
    }
}
