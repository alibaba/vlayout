package com.tmall.wireless.tangram.example.data;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.tmall.wireless.tangram.dataparser.concrete.Cell;
import com.tmall.wireless.tangram.support.TimerSupport;

/**
 * Created by villadora on 15/8/28.
 */
public class TimerCell extends Cell<TextView> {


    @Override
    public void bindView(@NonNull TextView view) {
        VirtualLayoutManager.LayoutParams layoutParams =
                new VirtualLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600);

        view.setText("" + pos);

        view.setLayoutParams(layoutParams);
        if (serviceManager != null) {
            final TimerSupport timerSupport = serviceManager.getService(TimerSupport.class);

            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {

                }

                @Override
                public void onViewDetachedFromWindow(View v) {

                }
            });

        }

        bindStyle(view);
    }
}
