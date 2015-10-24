package com.alibaba.android.vlayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * A flat LayoutView which used to render VirtualLayout in VirtualLayoutManager
 *
 * @since 1.0.0
 */
public class LayoutView extends View {

    public LayoutView(Context context) {
        super(context);
    }

    public LayoutView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LayoutView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LayoutView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
