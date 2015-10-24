package com.tmall.wireless.tangram.example.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tmall.wireless.tangram.dataparser.IViewCreator;

/**
 * Created by villadora on 15/8/28.
 */
public class RatioViewCreator implements IViewCreator<RatioViewCreator.RatioTextView> {
    @Override
    public RatioTextView create(@NonNull Context context, ViewGroup parent) {
        return new RatioTextView(context);
    }

    static class RatioTextView extends TextView {

        public double ratio = 1.0;

        public RatioTextView(Context context) {
            super(context);
        }

        public RatioTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public RatioTextView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            int width = getMeasuredWidth();
            setMeasuredDimension(width, (int) (width * ratio));
        }
    }
}
