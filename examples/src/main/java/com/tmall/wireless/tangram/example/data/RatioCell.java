package com.tmall.wireless.tangram.example.data;

import android.support.annotation.NonNull;

import com.tmall.wireless.tangram.dataparser.concrete.Cell;

public class RatioCell extends Cell<RatioViewCreator.RatioTextView> {
    @Override
    public void bindView(@NonNull RatioViewCreator.RatioTextView view) {
        view.ratio = 1.5;

        bindStyle(view);
        view.setBackgroundColor(0xff8a8a8a);
    }
}
