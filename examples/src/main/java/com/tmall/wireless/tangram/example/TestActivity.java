package com.tmall.wireless.tangram.example;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        //layoutManager.setReverseLayout(true);

        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.set(4, 4, 4, 4);
            }
        });


        recyclerView.setAdapter(
                new RecyclerView.Adapter() {
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        TextView view = (TextView) LayoutInflater.from(TestActivity.this).inflate(R.layout.item, parent, false);
                        FrameLayout frameLayout = new FrameLayout(TestActivity.this);
                        frameLayout.addView(view);
                        return new MainViewHolder(frameLayout);
                    }

                    @Override
                    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 300);
                        layoutParams.height = (int) (200 + (position % 10) * 10);

                        holder.itemView.findViewById(R.id.title).setLayoutParams(layoutParams);

                        ((TextView) holder.itemView.findViewById(R.id.title)).setText(Integer.toString(position));
                    }

                    @Override
                    public int getItemCount() {
                        return 40;
                    }
                });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                //recyclerView.scrollToPosition(7);
                //recyclerView.getAdapter().notifyDataSetChanged();
            }
        }, 6000);
    }


    static class MainViewHolder extends RecyclerView.ViewHolder {

        public MainViewHolder(View itemView) {
            super(itemView);
        }
    }
}
