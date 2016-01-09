package com.alibaba.android.vlayout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * Created by villadora on 16/1/9.
 */
public interface LayoutViewFactory {

    View generateLayoutView(@NonNull final Context context);
}
