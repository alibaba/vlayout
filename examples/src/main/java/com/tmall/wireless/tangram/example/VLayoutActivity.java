package com.tmall.wireless.tangram.example;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.ColumnLayoutHelper;
import com.alibaba.android.vlayout.layout.FixLayoutHelper;
import com.alibaba.android.vlayout.layout.GridLayoutHelper;
import com.alibaba.android.vlayout.layout.LinearLayoutHelper;
import com.alibaba.android.vlayout.layout.OnePlusNLayoutHelper;
import com.alibaba.android.vlayout.layout.StaggeredGridLayoutHelper;
import com.alibaba.android.vlayout.layout.StickyLayoutHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * @author villadora
 */
public class VLayoutActivity extends Activity {

    private static final boolean FIX_LAYOUT = false;

    private static final boolean LINEAR_LAYOUT = false;

    private static final boolean ONEN_LAYOUT = false;

    private static final boolean COLUMN_LAYOUT = true;

    private static final boolean GRID_LAYOUT = false;

    private static final boolean STICKY_LAYOUT = false;

    private static final boolean STAGGER_LAYOUT = true;

    private TextView mFirstText;
    private TextView mLastText;

    private TextView mCountText;

    private Runnable trigger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mFirstText = (TextView) findViewById(R.id.first);
        mLastText = (TextView) findViewById(R.id.last);
        mCountText = (TextView) findViewById(R.id.count);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_view);

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
                }else {
                    recyclerView.requestLayout();
                }
            }
        });


        final VirtualLayoutManager layoutManager = new VirtualLayoutManager(this);

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int scrollState) {

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int i, int i2) {
                mFirstText.setText("First: " + layoutManager.findFirstVisibleItemPosition());
                mLastText.setText("Existing: " + MainViewHolder.existing + " Created: " + MainViewHolder.createdTimes);
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


        RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();

        recyclerView.setRecycledViewPool(viewPool);

        // recyclerView.addItemDecoration(itemDecoration);

        viewPool.setMaxRecycledViews(0, 20);

        final DelegateAdapter delegateAdapter = new DelegateAdapter(layoutManager, true);

        recyclerView.setAdapter(delegateAdapter);

        List<DelegateAdapter.Adapter> adapters = new LinkedList<>();

        if (LINEAR_LAYOUT)
            adapters.add(new SubAdapter(this, new LinearLayoutHelper(), 5));

        if (STICKY_LAYOUT)
            adapters.add(new SubAdapter(this, new StickyLayoutHelper(), 1, new VirtualLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)));


        ColumnLayoutHelper layoutHelper = new ColumnLayoutHelper();
        // layoutHelper.setBgColor(0xff00f0f0);
        layoutHelper.setWeights(new float[]{40.0f, Float.NaN, 40});
        if (COLUMN_LAYOUT)
            adapters.add(new SubAdapter(this, layoutHelper, 3));

        if (ONEN_LAYOUT)
            adapters.add(new SubAdapter(this, new OnePlusNLayoutHelper(), 4) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    if (position == 0) {
                        holder.itemView.setLayoutParams(new VirtualLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 450));
                    }
                }
            });

        if (COLUMN_LAYOUT)
            adapters.add(new SubAdapter(this, new ColumnLayoutHelper(), 4));

        if (FIX_LAYOUT) {
            adapters.add(new SubAdapter(this, new FixLayoutHelper(), 1) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    VirtualLayoutManager.LayoutParams layoutParams = new VirtualLayoutManager.LayoutParams(200, 200);
                    layoutParams.topMargin = 10;
                    layoutParams.leftMargin = 10;
                    holder.itemView.setLayoutParams(layoutParams);
                }
            });
        }

        if (STICKY_LAYOUT)
            adapters.add(new SubAdapter(this, new StickyLayoutHelper(false), 1, new VirtualLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)));


        if (GRID_LAYOUT)
            adapters.add(new SubAdapter(this, new GridLayoutHelper(4), 20));

        if (LINEAR_LAYOUT)
            adapters.add(new SubAdapter(this, new LinearLayoutHelper(), 10));

        if (STAGGER_LAYOUT) {
            adapters.add(new SubAdapter(this, new StaggeredGridLayoutHelper(2, 0), 27) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    VirtualLayoutManager.LayoutParams layoutParams = new VirtualLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200);
                    layoutParams.height = 240 + position % 7 * 20;
                    holder.itemView.setLayoutParams(layoutParams);
                }
            });
        }

        if (GRID_LAYOUT) {
            adapters.add(new SubAdapter(this, new GridLayoutHelper(3), 45));
        }


        delegateAdapter.setAdapters(adapters);


        final Handler mainHandler = new Handler(Looper.getMainLooper());

        trigger = new Runnable() {
            @Override
            public void run() {
                // recyclerView.scrollToPosition(22);
                // recyclerView.getAdapter().notifyDataSetChanged();
                recyclerView.requestLayout();
                // mainHandler.postDelayed(trigger, 1000);
            }
        };


        mainHandler.postDelayed(trigger, 1000);
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
        }

        @Override
        protected void onBindViewHolderWithOffset(MainViewHolder holder, int position, int offsetTotal) {
            ((TextView) holder.itemView.findViewById(R.id.title)).setText(Integer.toString(offsetTotal));
        }

        @Override
        public int getItemCount() {
            return mCount;
        }
    }

    static class MainViewHolder extends RecyclerView.ViewHolder {

        public static volatile int existing = 0;
        public static int createdTimes = 0;

        public MainViewHolder(View itemView) {
            super(itemView);
            createdTimes++;
            existing++;
        }

        @Override
        protected void finalize() throws Throwable {
            existing--;
            super.finalize();
        }
    }
}
