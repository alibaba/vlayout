package com.tmall.wireless.tangram.example;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.ColumnLayoutHelper;
import com.alibaba.android.vlayout.layout.FixLayoutHelper;
import com.alibaba.android.vlayout.layout.GridLayoutHelper;
import com.alibaba.android.vlayout.layout.LinearLayoutHelper;
import com.alibaba.android.vlayout.layout.OnePlusNLayoutHelper;
import com.alibaba.android.vlayout.layout.StickyLayoutHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * @author villadora
 */
public class VLayoutActivity extends Activity {

    private TextView mFirstText;
    private TextView mLastText;

    private TextView mCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mFirstText = (TextView) findViewById(R.id.first);
        mLastText = (TextView) findViewById(R.id.last);
        mCountText = (TextView) findViewById(R.id.count);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_view);

        final VirtualLayoutManager layoutManager = new VirtualLayoutManager(this);

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int scrollState) {

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int i, int i2) {
                mFirstText.setText("First: " + layoutManager.findFirstVisibleItemPosition());
                mLastText.setText("Last: " + layoutManager.findLastVisibleItemPosition());
                mCountText.setText("Count: " + recyclerView.getChildCount());
            }
        });


        recyclerView.setLayoutManager(layoutManager);

        // layoutManager.setReverseLayout(true);

        RecyclerView.ItemDecoration itemDecoration = new RecyclerView.ItemDecoration() {
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = ((VirtualLayoutManager.LayoutParams) view.getLayoutParams()).getViewPosition();
                outRect.set(4, 4, 4, 4);
            }
        };


        recyclerView.addItemDecoration(itemDecoration);


        DelegateAdapter delegateAdapter = new DelegateAdapter(layoutManager);

        recyclerView.setAdapter(delegateAdapter);

        List<DelegateAdapter.Adapter> adapters = new LinkedList<>();

        adapters.add(new SubAdapter(this, new LinearLayoutHelper(), 5));

        adapters.add(new SubAdapter(this, new StickyLayoutHelper(), 1));
        adapters.add(new SubAdapter(this, new ColumnLayoutHelper(), 3));
        adapters.add(new SubAdapter(this, new OnePlusNLayoutHelper(), 4) {
            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                if (position == 0) {
                    holder.itemView.setLayoutParams(new VirtualLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 450));
                }
            }
        });
        adapters.add(new SubAdapter(this, new ColumnLayoutHelper(), 4));

        adapters.add(new SubAdapter(this, new FixLayoutHelper(), 1) {
            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                VirtualLayoutManager.LayoutParams layoutParams = new VirtualLayoutManager.LayoutParams(200, 200);
                layoutParams.topMargin = 10;
                layoutParams.leftMargin = 10;
                holder.itemView.setLayoutParams(layoutParams);
                holder.itemView.setBackgroundColor(0xffffcc00);
            }
        });
        adapters.add(new SubAdapter(this, new StickyLayoutHelper(false), 1));
        adapters.add(new SubAdapter(this, new GridLayoutHelper(4), 16));


        adapters.add(new SubAdapter(this, new LinearLayoutHelper(), 10));


        delegateAdapter.setAdapters(adapters);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // recyclerView.scrollToPosition(22);
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        }, 6000);
    }

    static class SubAdapter extends DelegateAdapter.Adapter<MainViewHolder> {

        private Context mContext;

        private LayoutHelper mLayoutHelper;


        private VirtualLayoutManager.LayoutParams mLayoutParams;
        private int mCount = 0;


        public SubAdapter(Context context, LayoutHelper layoutHelper, int count) {
            this(context, layoutHelper, count, new VirtualLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300));
        }

        public SubAdapter(Context context, LayoutHelper layoutHelper, int count, VirtualLayoutManager.LayoutParams layoutParams) {
            this.mContext = context;
            this.mLayoutHelper = layoutHelper;
            this.mCount = count;
            this.mLayoutParams = layoutParams;
        }

        @Override
        public LayoutHelper onCreateLayoutHelper() {
            return mLayoutHelper;
        }

        @Override
        public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item, parent, false));
        }

        @Override
        public void onBindViewHolder(MainViewHolder holder, int position) {
            // only vertical
            holder.itemView.setLayoutParams(
                    new VirtualLayoutManager.LayoutParams(mLayoutParams));
            // ((TextView) holder.itemView).setText(Integer.toString(position));
        }

        @Override
        protected void onBindViewHolderWithOffset(MainViewHolder holder, int position, int offsetTotal) {
            ((TextView) holder.itemView).setText(Integer.toString(offsetTotal));
        }

        @Override
        public int getItemCount() {
            return mCount;
        }
    }

    static class MainViewHolder extends RecyclerView.ViewHolder {

        public MainViewHolder(View itemView) {
            super(itemView);
        }
    }
}
