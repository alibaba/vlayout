package com.tmall.wireless.tangram.example.data;

import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.tmall.wireless.tangram.dataparser.concrete.Cell;


public class TestCell extends Cell<TextView> {

    @Override
    public void bindView(@NonNull TextView view) {
        VirtualLayoutManager.LayoutParams layoutParams =
                new VirtualLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300);

        view.setText(id + " pos: " + pos);

        view.setLayoutParams(layoutParams);

        bindStyle(view);

        if (style != null && style.bgColor != 0) {
            view.setBackgroundColor(style.bgColor);
        } else if (pos > 57) {
            view.setBackgroundColor(0x66cc0000 + (pos - 50) * 128);
        } else if (pos % 2 == 0) {
            view.setBackgroundColor(0xaa00ff00);
        } else {
            view.setBackgroundColor(0xccff00ff);
        }
    }
}
