/*
 * MIT License
 *
 * Copyright (c) 2016 Alibaba Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.alibaba.android.vlayout.example;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by villadora on 15/8/3.
 */
public class TestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_view);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);

        recyclerView.setLayoutManager(layoutManager);

        findViewById(R.id.jump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText position = (EditText) findViewById(R.id.position);
                if (!TextUtils.isEmpty(position.getText())) {
                    try {
                        int pos = Integer.parseInt(position.getText().toString());
                        recyclerView.scrollToPosition(pos);
                    } catch (Exception e) {
                        Log.e("VlayoutActivity", e.getMessage(), e);
                    }
                }
            }
        });


        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.set(4, 4, 4, 4);
            }
        });


        recyclerView.setAdapter(
                new RecyclerView.Adapter() {
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//                        TextView view = (TextView) LayoutInflater.from(TestActivity.this).inflate(R.layout.item, parent, false);
//                        FrameLayout frameLayout = new FrameLayout(TestActivity.this);
                        FrameLayout frameLayout = (FrameLayout) LayoutInflater.from(TestActivity.this).inflate(R.layout.item, parent, false);;
//                        frameLayout.addView(view);
                        return new MainViewHolder(frameLayout);
                    }

                    @Override
                    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 300);
                        layoutParams.height = (int) (200 + (position % 15) * 10);

                        holder.itemView.findViewById(R.id.title).setLayoutParams(layoutParams);
                        if (position == 30) {
                            StaggeredGridLayoutManager.LayoutParams lp = new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            lp.setFullSpan(true);
                            holder.itemView.setLayoutParams(lp);
                        } else {
                            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
                            if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                                ((StaggeredGridLayoutManager.LayoutParams) lp).setFullSpan(false);
                            }
                        }
                        ((TextView) holder.itemView.findViewById(R.id.title)).setText(Integer.toString(position));
                    }

                    @Override
                    public int getItemCount() {
                        return 60;
                    }
                });
    }


    static class MainViewHolder extends RecyclerView.ViewHolder {

        public MainViewHolder(View itemView) {
            super(itemView);
        }
    }
}
