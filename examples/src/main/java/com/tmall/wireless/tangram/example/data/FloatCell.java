package com.tmall.wireless.tangram.example.data;

import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by villadora on 15/8/24.
 */
public class FloatCell extends TestCell {

    @Override
    public void bindView(@NonNull TextView view) {
        super.bindView(view);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.height = 120;
        params.width = 120;
        params.leftMargin = 20;
        params.rightMargin = 20;
        params.topMargin = 20;
        params.bottomMargin = 20;

        view.setBackgroundColor(0xffffff00);
    }
}
