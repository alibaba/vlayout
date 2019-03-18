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

package com.alibaba.android.vlayout;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Looper;
import androidx.recyclerview.widget.RecyclerView;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.android.vlayout.VirtualLayoutManager.HORIZONTAL;
import static com.alibaba.android.vlayout.VirtualLayoutManager.VERTICAL;

/**
 * Created by villadora on 16/2/24.
 */
public class VirtualLayoutManagerTest extends ActivityInstrumentationTestCase2<Activity> {

    private static final String TAG = "VLMTest";

    private static final boolean DEBUG = false;

    protected RecyclerView mRecyclerView;

    protected Throwable mainThreadException;

    protected WrappedLinearLayoutManager mLayoutManager;

    private TestAdapter mTestAdapter;

    public VirtualLayoutManagerTest() {
        super("com.tmall.wireless.tangram", Activity.class);
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }


    public void testGetFirstLastChildrenTest() throws Throwable {
        getFirstLastChildrenTest(new Config().orientation(VERTICAL));
    }


    public void getFirstLastChildrenTest(final Config config) throws Throwable {
        setupByConfig(config, true);
        Runnable viewInBoundsTest = new Runnable() {
            @Override
            public void run() {
                VisibleChildren visibleChildren = mLayoutManager.traverseAndFindVisibleChildren();
                final String boundsLog = mLayoutManager.getBoundsLog();
                assertEquals(config + ":\nfirst visible child should match traversal result\n"
                                + boundsLog, visibleChildren.firstVisiblePosition,
                        mLayoutManager.findFirstVisibleItemPosition()
                );
                assertEquals(
                        config + ":\nfirst fully visible child should match traversal result\n"
                                + boundsLog, visibleChildren.firstFullyVisiblePosition,
                        mLayoutManager.findFirstCompletelyVisibleItemPosition()
                );

                assertEquals(config + ":\nlast visible child should match traversal result\n"
                                + boundsLog, visibleChildren.lastVisiblePosition,
                        mLayoutManager.findLastVisibleItemPosition()
                );
                assertEquals(
                        config + ":\nlast fully visible child should match traversal result\n"
                                + boundsLog, visibleChildren.lastFullyVisiblePosition,
                        mLayoutManager.findLastCompletelyVisibleItemPosition()
                );
            }
        };
        runTestOnUiThread(viewInBoundsTest);
        // smooth scroll to end of the list and keep testing meanwhile. This will test pre-caching
        // case
        final int scrollPosition = config.mStackFromEnd ? 0 : mTestAdapter.getItemCount();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(scrollPosition);
            }
        });
        while (mLayoutManager.isSmoothScrolling() ||
                mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            runTestOnUiThread(viewInBoundsTest);
            Thread.sleep(400);
        }
        // delete all items
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(0, mTestAdapter.getItemCount());
        mLayoutManager.waitForLayout(2);
        // test empty case
        runTestOnUiThread(viewInBoundsTest);
        // set a new adapter with huge items to test full bounds check
        mLayoutManager.expectLayouts(1);
        final int totalSpace = mLayoutManager.mOrientationHelper.getTotalSpace();
        final TestAdapter newAdapter = new TestAdapter(100) {
            @Override
            public void onBindViewHolder(TestViewHolder holder,
                                         int position) {
                super.onBindViewHolder(holder, position);
                if (config.mOrientation == HORIZONTAL) {
                    holder.itemView.setMinimumWidth(totalSpace + 5);
                } else {
                    holder.itemView.setMinimumHeight(totalSpace + 5);
                }
            }
        };
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(newAdapter);
            }
        });
        mLayoutManager.waitForLayout(2);
        runTestOnUiThread(viewInBoundsTest);
    }


    void setupByConfig(Config config, boolean waitForFirstLayout) throws Throwable {
        mRecyclerView = new RecyclerView(getActivity());
        mRecyclerView.setHasFixedSize(true);
        mTestAdapter = config.mTestAdapter == null ? new TestAdapter(config.mItemCount)
                : config.mTestAdapter;
        mRecyclerView.setAdapter(mTestAdapter);
        mLayoutManager = new WrappedLinearLayoutManager(getActivity(), config.mOrientation,
                config.mReverseLayout);
        mLayoutManager.setStackFromEnd(config.mStackFromEnd);
        mLayoutManager.setRecycleChildrenOnDetach(config.mRecycleChildrenOnDetach);
        mRecyclerView.setLayoutManager(mLayoutManager);
        if (waitForFirstLayout) {
            waitForFirstLayout();
        }
    }


    void postExceptionToInstrumentation(Throwable t) {
        if (DEBUG) {
            Log.e(TAG, "captured exception on main thread", t);
        }
        if (mainThreadException != null) {
            Log.e(TAG, "receiving another main thread exception. dropping.", t);
        } else {
            mainThreadException = t;
        }

        if (mRecyclerView != null && mRecyclerView
                .getLayoutManager() instanceof WrappedLinearLayoutManager) {
            WrappedLinearLayoutManager lm = (WrappedLinearLayoutManager) mRecyclerView.getLayoutManager();
            // finish all layouts so that we get the correct exception
            while (lm.layoutLatch.getCount() > 0) {
                lm.layoutLatch.countDown();
            }
        }
    }


    private void waitForFirstLayout() throws Throwable {
        mLayoutManager.expectLayouts(1);
        setRecyclerView(mRecyclerView);
        mLayoutManager.waitForLayout(2);
    }


    public void setRecyclerView(final RecyclerView recyclerView) throws Throwable {
        setRecyclerView(recyclerView, true);
    }

    public void setRecyclerView(final RecyclerView recyclerView, boolean assignDummyPool)
            throws Throwable {
        mRecyclerView = recyclerView;
        if (assignDummyPool) {
            RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool() {
                @Override
                public RecyclerView.ViewHolder getRecycledView(int viewType) {
                    RecyclerView.ViewHolder viewHolder = super.getRecycledView(viewType);
                    if (viewHolder == null) {
                        return null;
                    }

                    ViewHolderHelper.addViewHolderFlag(viewHolder, 1); //  RecyclerView.ViewHolder.FLAG_BOUND
                    ViewHolderHelper.setField(viewHolder, "mPosition", 200);
                    ViewHolderHelper.setField(viewHolder, "mOldPosition", 300);
                    ViewHolderHelper.setField(viewHolder, "mPreLayoutPosition", 500);
                    return viewHolder;
                }

                @Override
                public void putRecycledView(RecyclerView.ViewHolder scrap) {
                    super.putRecycledView(scrap);
                }
            };
            mRecyclerView.setRecycledViewPool(pool);
        }

        // mAdapterHelper = recyclerView.mAdapterHelper;
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ViewGroup) getActivity().findViewById(android.R.id.content)).addView(recyclerView);
            }
        });
    }

    class TestViewHolder extends RecyclerView.ViewHolder {

        Item mBindedItem;

        public TestViewHolder(View itemView) {
            super(itemView);
            itemView.setFocusable(true);
        }

        @Override
        public String toString() {
            return super.toString() + " item:" + mBindedItem;
        }
    }

    static class Item {
        final static AtomicInteger idCounter = new AtomicInteger(0);
        final public int mId = idCounter.incrementAndGet();

        int mAdapterIndex;

        final String mText;

        Item(int adapterIndex, String text) {
            mAdapterIndex = adapterIndex;
            mText = text;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "mId=" + mId +
                    ", originalIndex=" + mAdapterIndex +
                    ", text='" + mText + '\'' +
                    '}';
        }
    }


    class WrappedLinearLayoutManager extends VirtualLayoutManager {

        CountDownLatch layoutLatch;

        OnLayoutListener mOnLayoutListener;

        public WrappedLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public void expectLayouts(int count) {
            layoutLatch = new CountDownLatch(count);
        }

        public void waitForLayout(long timeout) throws InterruptedException {
            waitForLayout(timeout, TimeUnit.SECONDS);
        }

        @Override
        public void removeAndRecycleView(View child, RecyclerView.Recycler recycler) {
            if (DEBUG) {
                Log.d(TAG, "recycling view " + mRecyclerView.getChildViewHolder(child));
            }
            super.removeAndRecycleView(child, recycler);
        }

        @Override
        public void removeAndRecycleViewAt(int index, RecyclerView.Recycler recycler) {
            if (DEBUG) {
                Log.d(TAG, "recycling view at" + mRecyclerView.getChildViewHolder(getChildAt(index)));
            }
            super.removeAndRecycleViewAt(index, recycler);
        }

        private void waitForLayout(long timeout, TimeUnit timeUnit) throws InterruptedException {
            layoutLatch.await(timeout * (DEBUG ? 100 : 1), timeUnit);
            assertEquals("all expected layouts should be executed at the expected time",
                    0, layoutLatch.getCount());
            getInstrumentation().waitForIdleSync();
        }

        public String getBoundsLog() {
            StringBuilder sb = new StringBuilder();
            sb.append("view bounds:[start:").append(mOrientationHelper.getStartAfterPadding())
                    .append(",").append(" end").append(mOrientationHelper.getEndAfterPadding());
            sb.append("\nchildren bounds\n");
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                sb.append("child (ind:").append(i).append(", pos:").append(getPosition(child))
                        .append("[").append("start:").append(
                        mOrientationHelper.getDecoratedStart(child)).append(", end:")
                        .append(mOrientationHelper.getDecoratedEnd(child)).append("]\n");
            }
            return sb.toString();
        }

        public void waitForAnimationsToEnd(int timeoutInSeconds) throws InterruptedException {
            RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
            if (itemAnimator == null) {
                return;
            }
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean running = itemAnimator.isRunning(
                    new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                        @Override
                        public void onAnimationsFinished() {
                            latch.countDown();
                        }
                    }
            );
            if (running) {
                latch.await(timeoutInSeconds, TimeUnit.SECONDS);
            }
        }

        public VisibleChildren traverseAndFindVisibleChildren() {
            int childCount = getChildCount();
            final VisibleChildren visibleChildren = new VisibleChildren();
            final int start = mOrientationHelper.getStartAfterPadding();
            final int end = mOrientationHelper.getEndAfterPadding();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                final int childStart = mOrientationHelper.getDecoratedStart(child);
                final int childEnd = mOrientationHelper.getDecoratedEnd(child);
                final boolean fullyVisible = childStart >= start && childEnd <= end;
                final boolean hidden = childEnd <= start || childStart >= end;
                if (hidden) {
                    continue;
                }
                final int position = getPosition(child);
                if (fullyVisible) {
                    if (position < visibleChildren.firstFullyVisiblePosition ||
                            visibleChildren.firstFullyVisiblePosition == RecyclerView.NO_POSITION) {
                        visibleChildren.firstFullyVisiblePosition = position;
                    }

                    if (position > visibleChildren.lastFullyVisiblePosition) {
                        visibleChildren.lastFullyVisiblePosition = position;
                    }
                }

                if (position < visibleChildren.firstVisiblePosition ||
                        visibleChildren.firstVisiblePosition == RecyclerView.NO_POSITION) {
                    visibleChildren.firstVisiblePosition = position;
                }

                if (position > visibleChildren.lastVisiblePosition) {
                    visibleChildren.lastVisiblePosition = position;
                }

            }
            return visibleChildren;
        }

        Rect getViewBounds(View view) {
            if (getOrientation() == HORIZONTAL) {
                return new Rect(
                        mOrientationHelper.getDecoratedStart(view),
                        mSecondaryOrientationHelper.getDecoratedStart(view),
                        mOrientationHelper.getDecoratedEnd(view),
                        mSecondaryOrientationHelper.getDecoratedEnd(view));
            } else {
                return new Rect(
                        mSecondaryOrientationHelper.getDecoratedStart(view),
                        mOrientationHelper.getDecoratedStart(view),
                        mSecondaryOrientationHelper.getDecoratedEnd(view),
                        mOrientationHelper.getDecoratedEnd(view));
            }

        }

        Map<Item, Rect> collectChildCoordinates() throws Throwable {
            final Map<Item, Rect> items = new LinkedHashMap<Item, Rect>();
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int childCount = getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = getChildAt(i);
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child
                                .getLayoutParams();
                        TestViewHolder vh = (TestViewHolder) ViewHolderHelper.getViewHolder(lp);
                        items.put(vh.mBindedItem, getViewBounds(child));
                    }
                }
            });
            return items;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                if (mOnLayoutListener != null) {
                    mOnLayoutListener.before(recycler, state);
                }
                super.onLayoutChildren(recycler, state);
                if (mOnLayoutListener != null) {
                    mOnLayoutListener.after(recycler, state);
                }
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
            layoutLatch.countDown();
        }


    }


    static class OnLayoutListener {
        void before(RecyclerView.Recycler recycler, RecyclerView.State state) {
        }

        void after(RecyclerView.Recycler recycler, RecyclerView.State state) {
        }
    }


    static class VisibleChildren {

        int firstVisiblePosition = RecyclerView.NO_POSITION;

        int firstFullyVisiblePosition = RecyclerView.NO_POSITION;

        int lastVisiblePosition = RecyclerView.NO_POSITION;

        int lastFullyVisiblePosition = RecyclerView.NO_POSITION;

        @Override
        public String toString() {
            return "VisibleChildren{" +
                    "firstVisiblePosition=" + firstVisiblePosition +
                    ", firstFullyVisiblePosition=" + firstFullyVisiblePosition +
                    ", lastVisiblePosition=" + lastVisiblePosition +
                    ", lastFullyVisiblePosition=" + lastFullyVisiblePosition +
                    '}';
        }
    }


    static class Config implements Cloneable {

        private static final int DEFAULT_ITEM_COUNT = 100;

        private boolean mStackFromEnd;

        TestAdapter mTestAdapter = null;

        int mOrientation = VERTICAL;

        boolean mReverseLayout = false;

        boolean mRecycleChildrenOnDetach = false;

        int mItemCount = DEFAULT_ITEM_COUNT;

        // TestAdapter mTestAdapter;

        Config(int orientation, boolean reverseLayout, boolean stackFromEnd) {
            mOrientation = orientation;
            mReverseLayout = reverseLayout;
            mStackFromEnd = stackFromEnd;
        }

        public Config() {

        }


        Config adapter(TestAdapter adapter) {
            mTestAdapter = adapter;
            return this;
        }


        Config recycleChildrenOnDetach(boolean recycleChildrenOnDetach) {
            mRecycleChildrenOnDetach = recycleChildrenOnDetach;
            return this;
        }

        Config orientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        Config stackFromBottom(boolean stackFromBottom) {
            mStackFromEnd = stackFromBottom;
            return this;
        }

        Config reverseLayout(boolean reverseLayout) {
            mReverseLayout = reverseLayout;
            return this;
        }

        public Config itemCount(int itemCount) {
            mItemCount = itemCount;
            return this;
        }

        // required by convention
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "Config{" +
                    "mStackFromEnd=" + mStackFromEnd +
                    ", mOrientation=" + mOrientation +
                    ", mReverseLayout=" + mReverseLayout +
                    ", mRecycleChildrenOnDetach=" + mRecycleChildrenOnDetach +
                    ", mItemCount=" + mItemCount +
                    '}';
        }
    }


    class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        List<Item> mItems;

        TestAdapter(int count) {
            mItems = new ArrayList<Item>(count);
            for (int i = 0; i < count; i++) {
                mItems.add(new Item(i, "Item " + i));
            }
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
            return new TestViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {
            final Item item = mItems.get(position);
            ((TextView) (holder.itemView)).setText(item.mText + "(" + item.mAdapterIndex + ")");
            holder.mBindedItem = item;
        }

        public void deleteAndNotify(final int start, final int count) throws Throwable {
            deleteAndNotify(new int[]{start, count});
        }

        /**
         * Deletes items in the given ranges.
         * <p>
         * Note that each operation affects the one after so you should offset them properly.
         * <p>
         * For example, if adapter has 5 items (A,B,C,D,E), and then you call this method with
         * <code>[1, 2],[2, 1]</code>, it will first delete items B,C and the new adapter will be
         * A D E. Then it will delete 2,1 which means it will delete E.
         */
        public void deleteAndNotify(final int[]... startCountTuples) throws Throwable {
            for (int[] tuple : startCountTuples) {
                tuple[1] = -tuple[1];
            }
            new AddRemoveRunnable(startCountTuples).runOnMainThread();
        }

        @Override
        public long getItemId(int position) {
            return hasStableIds() ? mItems.get(position).mId : super.getItemId(position);
        }

        public void offsetOriginalIndices(int start, int offset) {
            for (int i = start; i < mItems.size(); i++) {
                mItems.get(i).mAdapterIndex += offset;
            }
        }

        /**
         * @param start  inclusive
         * @param end    exclusive
         * @param offset
         */
        public void offsetOriginalIndicesBetween(int start, int end, int offset) {
            for (int i = start; i < end && i < mItems.size(); i++) {
                mItems.get(i).mAdapterIndex += offset;
            }
        }

        public void addAndNotify(final int start, final int count) throws Throwable {
            addAndNotify(new int[]{start, count});
        }

        public void addAndNotify(final int[]... startCountTuples) throws Throwable {
            new AddRemoveRunnable(startCountTuples).runOnMainThread();
        }

        public void dispatchDataSetChanged() throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        public void changeAndNotify(final int start, final int count) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyItemRangeChanged(start, count);
                }
            });
        }

        public void changePositionsAndNotify(final int... positions) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < positions.length; i += 1) {
                        TestAdapter.super.notifyItemRangeChanged(positions[i], 1);
                    }
                }
            });
        }

        /**
         * Similar to other methods but negative count means delete and position count means add.
         * <p>
         * For instance, calling this method with <code>[1,1], [2,-1]</code> it will first add an
         * item to index 1, then remove an item from index 2 (updated index 2)
         */
        public void addDeleteAndNotify(final int[]... startCountTuples) throws Throwable {
            new AddRemoveRunnable(startCountTuples).runOnMainThread();
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public void moveItems(boolean notifyChange, int[]... fromToTuples) throws Throwable {
            for (int i = 0; i < fromToTuples.length; i += 1) {
                int[] tuple = fromToTuples[i];
                moveItem(tuple[0], tuple[1], false);
            }
            if (notifyChange) {
                dispatchDataSetChanged();
            }
        }

        public void moveItem(final int from, final int to, final boolean notifyChange)
                throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Item item = mItems.remove(from);
                    mItems.add(to, item);
                    offsetOriginalIndices(from, to - 1);
                    item.mAdapterIndex = to;
                    if (notifyChange) {
                        notifyDataSetChanged();
                    }
                }
            });
        }


        private class AddRemoveRunnable implements Runnable {
            final int[][] mStartCountTuples;

            public AddRemoveRunnable(int[][] startCountTuples) {
                mStartCountTuples = startCountTuples;
            }

            public void runOnMainThread() throws Throwable {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    run();
                } else {
                    runTestOnUiThread(this);
                }
            }

            @Override
            public void run() {
                for (int[] tuple : mStartCountTuples) {
                    if (tuple[1] < 0) {
                        delete(tuple);
                    } else {
                        add(tuple);
                    }
                }
            }

            private void add(int[] tuple) {
                // offset others
                offsetOriginalIndices(tuple[0], tuple[1]);
                for (int i = 0; i < tuple[1]; i++) {
                    mItems.add(tuple[0], new Item(i, "new item " + i));
                }
                notifyItemRangeInserted(tuple[0], tuple[1]);
            }

            private void delete(int[] tuple) {
                final int count = -tuple[1];
                offsetOriginalIndices(tuple[0] + count, tuple[1]);
                for (int i = 0; i < count; i++) {
                    mItems.remove(tuple[0]);
                }
                notifyItemRangeRemoved(tuple[0], count);
            }
        }
    }

}


