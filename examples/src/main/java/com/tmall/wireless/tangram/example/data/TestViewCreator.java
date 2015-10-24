package com.tmall.wireless.tangram.example.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tmall.wireless.tangram.dataparser.IViewCreator;
import com.tmall.wireless.tangram.example.R;

/**
 * Created by villadora on 15/8/24.
 */
public class TestViewCreator implements IViewCreator<TextView> {
    @Override
    public TextView create(@NonNull Context context, ViewGroup parent) {
        return (TextView) LayoutInflater.from(context).inflate(R.layout.item, parent, false);
    }
}
