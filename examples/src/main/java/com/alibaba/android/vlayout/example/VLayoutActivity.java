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

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.RecyclablePagerAdapter;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutParams;
import com.alibaba.android.vlayout.extend.LayoutManagerCanScrollListener;
import com.alibaba.android.vlayout.extend.PerformanceMonitor;
import com.alibaba.android.vlayout.extend.ViewLifeCycleListener;
import com.alibaba.android.vlayout.layout.ColumnLayoutHelper;
import com.alibaba.android.vlayout.layout.FixLayoutHelper;
import com.alibaba.android.vlayout.layout.FloatLayoutHelper;
import com.alibaba.android.vlayout.layout.GridLayoutHelper;
import com.alibaba.android.vlayout.layout.LinearLayoutHelper;
import com.alibaba.android.vlayout.layout.OnePlusNLayoutHelper;
import com.alibaba.android.vlayout.layout.RangeGridLayoutHelper;
import com.alibaba.android.vlayout.layout.RangeGridLayoutHelper.GridRangeStyle;
import com.alibaba.android.vlayout.layout.ScrollFixLayoutHelper;
import com.alibaba.android.vlayout.layout.SingleLayoutHelper;
import com.alibaba.android.vlayout.layout.StaggeredGridLayoutHelper;
import com.alibaba.android.vlayout.layout.StickyLayoutHelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author villadora
 */
public class VLayoutActivity extends Activity {

    private static final boolean BANNER_LAYOUT = true;

    private static final boolean FIX_LAYOUT = true;

    private static final boolean LINEAR_LAYOUT = true;

    private static final boolean SINGLE_LAYOUT = true;

    private static final boolean FLOAT_LAYOUT = true;

    private static final boolean ONEN_LAYOUT = true;

    private static final boolean COLUMN_LAYOUT = true;

    private static final boolean GRID_LAYOUT = true;

    private static final boolean STICKY_LAYOUT = true;

    private static final boolean STAGGER_LAYOUT = true;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private TextView mFirstText;
    private TextView mLastText;

    private TextView mCountText;

    private TextView mTotalOffsetText;

    private Runnable trigger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        ;
        mFirstText = (TextView) findViewById(R.id.first);
        mLastText = (TextView) findViewById(R.id.last);
        mCountText = (TextView) findViewById(R.id.count);
        mTotalOffsetText = (TextView) findViewById(R.id.total_offset);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_view);

        final VirtualLayoutManager layoutManager = new VirtualLayoutManager(this);
        layoutManager.setPerformanceMonitor(new PerformanceMonitor() {

            long start;
            long end;

            @Override
            public void recordStart(String phase, View view) {
                start = System.currentTimeMillis();
            }

            @Override
            public void recordEnd(String phase, View view) {
                end = System.currentTimeMillis();
                Log.d("VLayoutActivity", view.getClass().getName() + " " + (end - start));
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int scrollState) {

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int i, int i2) {
                mFirstText.setText("First: " + layoutManager.findFirstVisibleItemPosition());
                mLastText.setText("Existing: " + MainViewHolder.existing + " Created: " + MainViewHolder.createdTimes);
                mCountText.setText("Count: " + recyclerView.getChildCount());
                mTotalOffsetText.setText("Total Offset: " + layoutManager.getOffsetToStart());
            }
        });

        recyclerView.setLayoutManager(layoutManager);

        // layoutManager.setReverseLayout(true);

        RecyclerView.ItemDecoration itemDecoration = new RecyclerView.ItemDecoration() {
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = ((LayoutParams) view.getLayoutParams()).getViewPosition();
                outRect.set(4, 4, 4, 4);
            }
        };


        final RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();

        recyclerView.setRecycledViewPool(viewPool);

        // recyclerView.addItemDecoration(itemDecoration);

        viewPool.setMaxRecycledViews(0, 20);

        layoutManager.setRecycleOffset(300);

        // viewLifeCycleListener should be used with setRecycleOffset()
        layoutManager.setViewLifeCycleListener(new ViewLifeCycleListener() {
            @Override
            public void onAppearing(View view) {
//                Log.e("ViewLifeCycleTest", "onAppearing: " + view);
            }

            @Override
            public void onDisappearing(View view) {
//                Log.e("ViewLifeCycleTest", "onDisappearing: " + view);
            }

            @Override
            public void onAppeared(View view) {
//                Log.e("ViewLifeCycleTest", "onAppeared: " + view);
            }

            @Override
            public void onDisappeared(View view) {
//                Log.e("ViewLifeCycleTest", "onDisappeared: " + view);
            }
        });

        layoutManager.setLayoutManagerCanScrollListener(new LayoutManagerCanScrollListener() {
            @Override
            public boolean canScrollVertically() {
                Log.i("vlayout", "canScrollVertically: ");
                return true;
            }

            @Override
            public boolean canScrollHorizontally() {
                Log.i("vlayout", "canScrollHorizontally: ");
                return true;
            }
        });

        final DelegateAdapter delegateAdapter = new DelegateAdapter(layoutManager, true);

        recyclerView.setAdapter(delegateAdapter);

        final List<DelegateAdapter.Adapter> adapters = new LinkedList<>();

        if (BANNER_LAYOUT) {
            adapters.add(new SubAdapter(this, new LinearLayoutHelper(), 1) {

                @Override
                public void onViewRecycled(MainViewHolder holder) {
                    if (holder.itemView instanceof ViewPager) {
                        ((ViewPager) holder.itemView).setAdapter(null);
                    }
                }

                @Override
                public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    if (viewType == 1)
                        return new MainViewHolder(
                                LayoutInflater.from(VLayoutActivity.this).inflate(R.layout.view_pager, parent, false));

                    return super.onCreateViewHolder(parent, viewType);
                }

                @Override
                public int getItemViewType(int position) {
                    return 1;
                }

                @Override
                protected void onBindViewHolderWithOffset(MainViewHolder holder, int position, int offsetTotal) {

                }

                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    if (holder.itemView instanceof ViewPager) {
                        ViewPager viewPager = (ViewPager) holder.itemView;

                        viewPager.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200));

                        // from position to get adapter
                        viewPager.setAdapter(new PagerAdapter(this, viewPool));
                    }
                }
            });
        }

        //{
        //    GridLayoutHelper helper = new GridLayoutHelper(10);
        //    helper.setAspectRatio(4f);
        //    helper.setGap(10);
        //    adapters.add(new SubAdapter(this, helper, 80));
        //}

        if (FLOAT_LAYOUT) {
            FloatLayoutHelper layoutHelper = new FloatLayoutHelper();
            layoutHelper.setAlignType(FixLayoutHelper.BOTTOM_RIGHT);
            layoutHelper.setDefaultLocation(100, 400);
            LayoutParams layoutParams = new LayoutParams(150, 150);
            adapters.add(new SubAdapter(this, layoutHelper, 1, layoutParams));
        }

        if (LINEAR_LAYOUT) {
            LinearLayoutHelper layoutHelper1 = new LinearLayoutHelper();
            layoutHelper1.setBgColor(Color.YELLOW);
            layoutHelper1.setAspectRatio(2.0f);
            layoutHelper1.setMargin(10, 10, 10, 10);
            layoutHelper1.setPadding(10, 10, 10, 10);
            LinearLayoutHelper layoutHelper2 = new LinearLayoutHelper();
            layoutHelper2.setAspectRatio(4.0f);
            layoutHelper2.setDividerHeight(10);
            layoutHelper2.setMargin(10, 0, 10, 10);
            layoutHelper2.setPadding(10, 0, 10, 10);
            layoutHelper2.setBgColor(0xFFF5A623);
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            adapters.add(new SubAdapter(this, layoutHelper1, 1) {
                @Override
                public void onBindViewHolder(final MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    final SubAdapter subAdapter = this;
                    //mainHandler.postDelayed(new Runnable() {
                    //    @Override
                    //    public void run() {
                    //        //delegateAdapter.removeAdapter(subAdapter);
                    //        //notifyItemRemoved(1);
                    //        holder.itemView.setVisibility(View.GONE);
                    //        notifyItemChanged(1);
                    //        layoutManager.runAdjustLayout();
                    //    }
                    //}, 2000L);
                }
            });
            adapters.add(new SubAdapter(this, layoutHelper2, 6) {

                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    if (position % 2 == 0) {
                        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300);
                        layoutParams.mAspectRatio = 5;
                        holder.itemView.setLayoutParams(layoutParams);
                    }
                }
            });
        }

        {
            RangeGridLayoutHelper layoutHelper = new RangeGridLayoutHelper(4);
            layoutHelper.setBgColor(Color.GREEN);
            layoutHelper.setWeights(new float[]{20f, 26.665f});
            layoutHelper.setPadding(15, 15, 15, 15);
            layoutHelper.setMargin(15, 50, 15, 150);
            layoutHelper.setHGap(10);
            layoutHelper.setVGap(10);
            GridRangeStyle rangeStyle = new GridRangeStyle();
            rangeStyle.setBgColor(Color.RED);
            rangeStyle.setSpanCount(2);
            rangeStyle.setWeights(new float[]{46.665f});
            rangeStyle.setPadding(15, 15, 15, 15);
            rangeStyle.setMargin(15, 15, 15, 15);
            rangeStyle.setHGap(5);
            rangeStyle.setVGap(5);
            layoutHelper.addRangeStyle(0, 7, rangeStyle);

            GridRangeStyle rangeStyle1 = new GridRangeStyle();
            rangeStyle1.setBgColor(Color.YELLOW);
            rangeStyle1.setSpanCount(2);
            rangeStyle1.setWeights(new float[]{46.665f});
            rangeStyle1.setPadding(15, 15, 15, 15);
            rangeStyle1.setMargin(15, 15, 15, 15);
            rangeStyle1.setHGap(5);
            rangeStyle1.setVGap(5);
            layoutHelper.addRangeStyle(8, 15, rangeStyle1);

            GridRangeStyle rangeStyle2 = new GridRangeStyle();
            rangeStyle2.setBgColor(Color.CYAN);
            rangeStyle2.setSpanCount(2);
            rangeStyle2.setWeights(new float[]{46.665f});
            rangeStyle2.setPadding(15, 15, 15, 15);
            rangeStyle2.setMargin(15, 15, 15, 15);
            rangeStyle2.setHGap(5);
            rangeStyle2.setVGap(5);
            layoutHelper.addRangeStyle(16, 22, rangeStyle2);
            GridRangeStyle rangeStyle3 = new GridRangeStyle();
            rangeStyle3.setBgColor(Color.DKGRAY);
            rangeStyle3.setSpanCount(1);
            rangeStyle3.setWeights(new float[]{46.665f});
            rangeStyle3.setPadding(15, 15, 15, 15);
            rangeStyle3.setMargin(15, 15, 15, 15);
            rangeStyle3.setHGap(5);
            rangeStyle3.setVGap(5);
            rangeStyle2.addChildRangeStyle(0, 2, rangeStyle3);
            GridRangeStyle rangeStyle4 = new GridRangeStyle();
            rangeStyle4.setBgColor(Color.BLUE);
            rangeStyle4.setSpanCount(2);
            rangeStyle4.setWeights(new float[]{46.665f});
            rangeStyle4.setPadding(15, 15, 15, 15);
            rangeStyle4.setMargin(15, 15, 15, 15);
            rangeStyle4.setHGap(5);
            rangeStyle4.setVGap(5);
            rangeStyle2.addChildRangeStyle(3, 6, rangeStyle4);

            GridRangeStyle rangeStyle5 = new GridRangeStyle();
            rangeStyle5.setBgColor(Color.RED);
            rangeStyle5.setSpanCount(2);
            rangeStyle5.setPadding(15, 15, 15, 15);
            rangeStyle5.setMargin(15, 15, 15, 15);
            rangeStyle5.setHGap(5);
            rangeStyle5.setVGap(5);
            layoutHelper.addRangeStyle(23, 30, rangeStyle5);
            GridRangeStyle rangeStyle6 = new GridRangeStyle();
            rangeStyle6.setBgColor(Color.MAGENTA);
            rangeStyle6.setSpanCount(2);
            rangeStyle6.setPadding(15, 15, 15, 15);
            rangeStyle6.setMargin(15, 15, 15, 15);
            rangeStyle6.setHGap(5);
            rangeStyle6.setVGap(5);
            rangeStyle5.addChildRangeStyle(0, 7, rangeStyle6);

            adapters.add(new SubAdapter(this, layoutHelper, 23));
        }

        {
            SingleLayoutHelper layoutHelper = new SingleLayoutHelper();
            layoutHelper.setBgColor(Color.BLUE);
            layoutHelper.setMargin(0, 30, 0, 200);
            adapters.add(new SubAdapter(this, layoutHelper, 1, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)));
        }

        if (STICKY_LAYOUT) {
            StickyLayoutHelper layoutHelper = new StickyLayoutHelper();
            //layoutHelper.setOffset(100);
            layoutHelper.setAspectRatio(4);
            adapters.add(new SubAdapter(this, layoutHelper, 1, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)));
        }

        //{
        //    final StaggeredGridLayoutHelper helper = new StaggeredGridLayoutHelper(3, 10);
        //    helper.setBgColor(0xFF86345A);
        //    adapters.add(new SubAdapter(this, helper, 4) {
        //
        //        @Override
        //        public void onBindViewHolder(MainViewHolder holder, int position) {
        //            super.onBindViewHolder(holder, position);
        //            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300);
        //            if (position % 2 == 0) {
        //                layoutParams.mAspectRatio = 1.0f;
        //            } else {
        //                layoutParams.height = 340 + position % 7 * 20;
        //            }
        //            holder.itemView.setLayoutParams(layoutParams);
        //        }
        //    });
        //}
        {

            final GridLayoutHelper helper = new GridLayoutHelper(3, 4);
            helper.setBgColor(0xFF86345A);
            adapters.add(new SubAdapter(this, helper, 4) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300);
                    holder.itemView.setLayoutParams(layoutParams);
                }
            });
        }

        {
            RangeGridLayoutHelper layoutHelper = new RangeGridLayoutHelper(4);
            layoutHelper.setBgColor(Color.GREEN);
            layoutHelper.setWeights(new float[]{20f, 26.665f});
            layoutHelper.setPadding(15, 15, 15, 15);
            layoutHelper.setMargin(15, 15, 15, 15);
            layoutHelper.setHGap(10);
            layoutHelper.setVGap(10);
            GridRangeStyle rangeStyle = new GridRangeStyle();
            rangeStyle.setBgColor(Color.RED);
            rangeStyle.setSpanCount(2);
            rangeStyle.setWeights(new float[]{46.665f});
            rangeStyle.setPadding(15, 15, 15, 15);
            rangeStyle.setMargin(15, 15, 15, 15);
            rangeStyle.setHGap(5);
            rangeStyle.setVGap(5);
            layoutHelper.addRangeStyle(4, 7, rangeStyle);
            GridRangeStyle rangeStyle1 = new GridRangeStyle();
            rangeStyle1.setBgColor(Color.YELLOW);
            rangeStyle1.setSpanCount(2);
            rangeStyle1.setWeights(new float[]{46.665f});
            rangeStyle1.setPadding(15, 15, 15, 15);
            rangeStyle1.setMargin(15, 15, 15, 15);
            rangeStyle1.setHGap(5);
            rangeStyle1.setVGap(5);
            layoutHelper.addRangeStyle(8, 11, rangeStyle1);
            adapters.add(new SubAdapter(this, layoutHelper, 16));

        }

        if (SINGLE_LAYOUT) {
            SingleLayoutHelper layoutHelper = new SingleLayoutHelper();
            layoutHelper.setBgColor(Color.rgb(135, 225, 90));
            layoutHelper.setAspectRatio(4);
            layoutHelper.setMargin(10, 20, 10, 20);
            layoutHelper.setPadding(10, 10, 10, 10);
            adapters.add(new SubAdapter(this, layoutHelper, 1, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)));
        }

        if (COLUMN_LAYOUT) {
            ColumnLayoutHelper layoutHelper = new ColumnLayoutHelper();
            layoutHelper.setBgColor(0xff00f0f0);
            layoutHelper.setWeights(new float[]{40.0f, Float.NaN, 40});
            adapters.add(new SubAdapter(this, layoutHelper, 5) {

                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    if (position == 0) {
                        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300);
                        layoutParams.mAspectRatio = 4;
                        holder.itemView.setLayoutParams(layoutParams);
                    } else {
                        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300);
                        layoutParams.mAspectRatio = Float.NaN;
                        holder.itemView.setLayoutParams(layoutParams);
                    }
                }

            });
        }

        if (ONEN_LAYOUT) {
            OnePlusNLayoutHelper helper = new OnePlusNLayoutHelper();
            helper.setBgColor(0xff876384);
            helper.setAspectRatio(4.0f);
            helper.setColWeights(new float[]{40f, 45f});
            helper.setMargin(10, 20, 10, 20);
            helper.setPadding(10, 10, 10, 10);
            adapters.add(new SubAdapter(this, helper, 2));
        }

        if (ONEN_LAYOUT) {
            OnePlusNLayoutHelper helper = new OnePlusNLayoutHelper();
            helper.setBgColor(0xffef8ba3);
            helper.setAspectRatio(2.0f);
            helper.setColWeights(new float[]{40f});
            helper.setRowWeight(30f);
            helper.setMargin(10, 20, 10, 20);
            helper.setPadding(10, 10, 10, 10);
            adapters.add(new SubAdapter(this, helper, 4) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    LayoutParams lp = (LayoutParams) holder.itemView.getLayoutParams();
                    if (position == 0) {
                        lp.rightMargin = 1;
                    } else if (position == 1) {

                    } else if (position == 2) {
                        lp.topMargin = 1;
                        lp.rightMargin = 1;
                    }
                }
            });
        }

        if (ONEN_LAYOUT) {
            adapters.add(new SubAdapter(this, new OnePlusNLayoutHelper(), 0));
            OnePlusNLayoutHelper helper = new OnePlusNLayoutHelper();
            helper.setBgColor(0xff87e543);
            helper.setAspectRatio(1.8f);
            helper.setColWeights(new float[]{33.33f, 50f, 40f});
            helper.setMargin(10, 20, 10, 20);
            helper.setPadding(10, 10, 10, 10);
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            adapters.add(new SubAdapter(this, helper, 3, lp) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    LayoutParams lp = (LayoutParams) holder.itemView.getLayoutParams();
                    if (position == 0) {
                        lp.rightMargin = 1;
                    }
                }
            });
        }

        if (COLUMN_LAYOUT) {
            adapters.add(new SubAdapter(this, new ColumnLayoutHelper(), 0));
            adapters.add(new SubAdapter(this, new ColumnLayoutHelper(), 4));
        }

        if (FIX_LAYOUT) {
            FixLayoutHelper layoutHelper = new FixLayoutHelper(10, 10);
            adapters.add(new SubAdapter(this, layoutHelper, 0));

            layoutHelper = new FixLayoutHelper(FixLayoutHelper.TOP_RIGHT, 20, 20);

            adapters.add(new SubAdapter(this, layoutHelper, 1) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200);
                    holder.itemView.setLayoutParams(layoutParams);
                }
            });
        }

        //if (STICKY_LAYOUT) {
        //    StickyLayoutHelper layoutHelper = new StickyLayoutHelper(false);
        //    adapters.add(new SubAdapter(this, layoutHelper, 0));
        //    layoutHelper = new StickyLayoutHelper(false);
        //    layoutHelper.setOffset(100);
        //    adapters.add(new SubAdapter(this, layoutHelper, 1, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)));
        //}

        if (GRID_LAYOUT) {
            GridLayoutHelper layoutHelper = new GridLayoutHelper(2);
            layoutHelper.setMargin(7, 0, 7, 0);
            layoutHelper.setWeights(new float[]{46.665f});
            layoutHelper.setHGap(3);
            adapters.add(new SubAdapter(this, layoutHelper, 2));

            layoutHelper = new GridLayoutHelper(4);
            layoutHelper.setWeights(new float[]{20f, 26.665f});
            layoutHelper.setMargin(7, 0, 7, 0);
            layoutHelper.setHGap(3);
            adapters.add(new SubAdapter(this, layoutHelper, 8));
        }


        if (GRID_LAYOUT) {
            adapters.add(new SubAdapter(this, new GridLayoutHelper(4), 0));

            GridLayoutHelper helper = new GridLayoutHelper(4);
            helper.setAspectRatio(4f);
            //helper.setColWeights(new float[]{40, 20, 30, 30});
            // helper.setMargin(0, 10, 0, 10);
            helper.setGap(10);
            adapters.add(new SubAdapter(this, helper, 80) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    LayoutParams lp = (LayoutParams) holder.itemView.getLayoutParams();
                    // lp.bottomMargin = 1;
                    // lp.rightMargin = 1;
                }
            });
        }

        if (FIX_LAYOUT) {
            adapters.add(new SubAdapter(this, new ScrollFixLayoutHelper(20, 20), 1) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    LayoutParams layoutParams = new LayoutParams(200, 200);
                    holder.itemView.setLayoutParams(layoutParams);
                }
            });
        }

        if (LINEAR_LAYOUT)
            adapters.add(new SubAdapter(this, new LinearLayoutHelper(), 10));

        if (GRID_LAYOUT) {
            GridLayoutHelper helper = new GridLayoutHelper(3);
            helper.setMargin(0, 10, 0, 10);
            adapters.add(new SubAdapter(this, helper, 3));
        }

        if (STAGGER_LAYOUT) {
            // adapters.add(new SubAdapter(this, new StaggeredGridLayoutHelper(2, 0), 0));
            final StaggeredGridLayoutHelper helper = new StaggeredGridLayoutHelper(2, 10);
            helper.setMargin(20, 10, 10, 10);
            helper.setPadding(10, 10, 20, 10);
            helper.setBgColor(0xFF86345A);
            adapters.add(new SubAdapter(this, helper, 27) {
                @Override
                public void onBindViewHolder(MainViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200);
                    if (position % 2 == 0) {
                        layoutParams.mAspectRatio = 1.0f;
                    } else {
                        layoutParams.height = 340 + position % 7 * 20;
                    }
                    holder.itemView.setLayoutParams(layoutParams);
                }
            });
        }

        if (COLUMN_LAYOUT) {
            // adapters.add(new SubAdapter(this, new ColumnLayoutHelper(), 3));
        }

        if (GRID_LAYOUT) {
            // adapters.add(new SubAdapter(this, new GridLayoutHelper(4), 24));
        }

        adapters.add(
                new FooterAdapter(recyclerView, VLayoutActivity.this, new GridLayoutHelper(1), 1));

        delegateAdapter.setAdapters(adapters);


        final Handler mainHandler = new Handler(Looper.getMainLooper());

        trigger = new Runnable() {
            @Override
            public void run() {
                //recyclerView.scrollToPosition(22);
                //recyclerView.getAdapter().notifyDataSetChanged();
                //mainHandler.postDelayed(trigger, 1000);
                //List<DelegateAdapter.Adapter> newAdapters = new ArrayList<>();
                //newAdapters.add((new SubAdapter(VLayoutActivity.this, new ColumnLayoutHelper(), 3)));
                //newAdapters.add((new SubAdapter(VLayoutActivity.this, new GridLayoutHelper(4), 24)));
                //delegateAdapter.addAdapter(0, new SubAdapter(VLayoutActivity.this, new ColumnLayoutHelper(), 3));
                //delegateAdapter.addAdapter(1, new SubAdapter(VLayoutActivity.this, new GridLayoutHelper(4), 24));
                //delegateAdapter.notifyDataSetChanged();
            }
        };

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
                } else {
                    recyclerView.requestLayout();
                }
                //FooterAdapter footer = (FooterAdapter)adapters.get(adapters.size() - 1);
                //footer.toggleFoot();
            }
        });


        mainHandler.postDelayed(trigger, 1000);

        mSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 2000L);
            }
        });
        setListenerToRootView();
    }

    boolean isOpened = false;

    public void setListenerToRootView() {
        final View activityRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > 100) { // 99% of the time the height diff will be due to a keyboard.
                    if (isOpened == false) {
                        //Do two things, make the view top visible and the editText smaller
                    }
                    isOpened = true;
                } else if (isOpened == true) {
                    isOpened = false;
                    final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_view);
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
            }
        });
    }

    static class FooterAdapter extends DelegateAdapter.Adapter<MainViewHolder> {

        private RecyclerView mRecyclerView;

        private Context mContext;

        private LayoutHelper mLayoutHelper;

        private LayoutParams mLayoutParams;
        private int mCount = 0;

        private boolean showFooter = false;

        public FooterAdapter(RecyclerView recyclerView, Context context, LayoutHelper layoutHelper, int count) {
            this(recyclerView, context, layoutHelper, count, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300));
        }

        public FooterAdapter(RecyclerView recyclerView, Context context, LayoutHelper layoutHelper, int count, @NonNull LayoutParams layoutParams) {
            this.mRecyclerView = recyclerView;
            this.mContext = context;
            this.mLayoutHelper = layoutHelper;
            this.mCount = count;
            this.mLayoutParams = layoutParams;
        }

        @Override
        public int getItemViewType(int position) {
            return 100;
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
            LayoutParams lp = (LayoutParams) holder.itemView.getLayoutParams();
            if (showFooter) {
                lp.height = 300;
            } else {
                lp.height = 0;
            }
            holder.itemView.setLayoutParams(lp);
        }


        @Override
        protected void onBindViewHolderWithOffset(MainViewHolder holder, int position, int offsetTotal) {
            ((TextView) holder.itemView.findViewById(R.id.title)).setText(Integer.toString(offsetTotal));
        }

        @Override
        public int getItemCount() {
            return mCount;
        }

        public void toggleFoot() {
            this.showFooter = !this.showFooter;
            mRecyclerView.getAdapter().notifyItemChanged(205);
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.scrollToPosition(205);
                    mRecyclerView.requestLayout();
                }
            });
        }

    }

    // RecyclableViewPager

    static class PagerAdapter extends RecyclablePagerAdapter<MainViewHolder> {
        public PagerAdapter(SubAdapter adapter, RecyclerView.RecycledViewPool pool) {
            super(adapter, pool);
        }

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public void onBindViewHolder(MainViewHolder viewHolder, int position) {
            // only vertical
            viewHolder.itemView.setLayoutParams(
                    new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ((TextView) viewHolder.itemView.findViewById(R.id.title)).setText("Banner: " + position);
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }
    }


    static class SubAdapter extends DelegateAdapter.Adapter<MainViewHolder> {

        private Context mContext;

        private LayoutHelper mLayoutHelper;


        private LayoutParams mLayoutParams;
        private int mCount = 0;


        public SubAdapter(Context context, LayoutHelper layoutHelper, int count) {
            this(context, layoutHelper, count, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300));
        }

        public SubAdapter(Context context, LayoutHelper layoutHelper, int count, @NonNull LayoutParams layoutParams) {
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
                    new LayoutParams(mLayoutParams));
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
