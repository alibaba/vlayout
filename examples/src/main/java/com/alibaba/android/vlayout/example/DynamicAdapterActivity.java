package com.alibaba.android.vlayout.example;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ArrayAdapter;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.GridLayoutHelper;
import com.alibaba.android.vlayout.layout.LinearLayoutHelper;
import com.alibaba.android.vlayout.layout.OnePlusNLayoutHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * <>
 *
 * @author lixi
 * @date 2019/1/31
 */
public class DynamicAdapterActivity extends Activity {

    private List<DelegateAdapter.Adapter> mReadOnlyAdapters = Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dynamic_adapter_activity);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_view);
        final VirtualLayoutManager layoutManager = new VirtualLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        final DelegateAdapter delegateAdapter = new DelegateAdapter(layoutManager);
        recyclerView.setAdapter(delegateAdapter);

        List<DelegateAdapter.Adapter> adapters = new LinkedList<>();
        adapters.add(randomAdapter());
        adapters.add(randomAdapter());
        delegateAdapter.setAdapters(adapters);

        mReadOnlyAdapters = Collections.unmodifiableList(adapters);
        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<DelegateAdapter.Adapter> adapters = new ArrayList<>(mReadOnlyAdapters);
                adapters.add(randomAdapter());
                mReadOnlyAdapters = Collections.unmodifiableList(adapters);
                delegateAdapter.setAdapters(adapters);
            }
        });


        findViewById(R.id.remove_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mReadOnlyAdapters.isEmpty()) {
                    return;
                }

                List<DelegateAdapter.Adapter> adapters = new ArrayList<>(mReadOnlyAdapters);
                int index = adapters.size() / 2;
                adapters.remove(random.nextInt(Math.max(1, index)));
                mReadOnlyAdapters = Collections.unmodifiableList(adapters);
                delegateAdapter.setAdapters(adapters);
            }
        });

        final int popupWindowWidth = (int) (getResources().getDisplayMetrics().density * 220 + 0.5F);

        findViewById(R.id.display_history_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseArrayCompat<Set<String>> cache = DynamicAdapter.getCache();
                List<String> lists = new ArrayList<>();
                int size = cache.size();
                for (int i = 0; i < size; i++) {
                    int key = cache.keyAt(i);
                    if (key == DynamicAdapter.CLASS_CAST_EXECPTION) {
                        lists.add("ClassCastException:");
                        Set<String> history = cache.valueAt(i);
                        for (String clazz : history) {
                            lists.add("    " + clazz);
                        }
                    } else {
                        lists.add("Cantor ViewType:  " + key);
                        Set<String> history = cache.valueAt(i);
                        for (String clazz : history) {
                            lists.add("        " + clazz);
                        }
                    }
                }
                ListPopupWindow popupWindow = new ListPopupWindow(v.getContext());
                popupWindow.setAdapter(new ArrayAdapter<>(v.getContext(), android.R.layout.simple_list_item_1, lists));
                popupWindow.setAnchorView(v);
                popupWindow.setContentWidth(popupWindowWidth);
                popupWindow.show();

            }
        });

    }


    private final Class[] classes = {
            ViewHolder1.class,
            ViewHolder2.class,
            ViewHolder3.class,
            ViewHolder4.class,
            ViewHolder5.class,
            ViewHolder6.class,
            ViewHolder7.class,
            ViewHolder8.class,
            ViewHolder9.class,
            ViewHolder10.class,
            ViewHolder11.class,
            ViewHolder12.class,
            ViewHolder13.class,
            ViewHolder14.class,
            ViewHolder15.class,
            ViewHolder16.class,
            ViewHolder17.class,
            ViewHolder18.class,
            ViewHolder19.class,
            ViewHolder20.class
    };

    private DelegateAdapter.Adapter randomAdapter() {
        @SuppressWarnings("unchecked")
        Class<? extends RecyclerView.ViewHolder> holderClazz = classes[random.nextInt(classes.length)];

        return new DynamicAdapter<>(randomLayoutHelper(), Math.max(1, random.nextInt(5)), holderClazz);
    }


    private final Random random = new Random();

    private LayoutHelper randomLayoutHelper() {
        int index = random.nextInt(3);
        switch (index) {
            case 0:
                return new GridLayoutHelper(Math.max(2, random.nextInt(5)));
            case 1:
                return new OnePlusNLayoutHelper();

            // TODO 搭配 StaggeredGridLayoutHelper 使用时有可能导致界面显示异常（ remainingSpace 提前用完，导致后续界面不再绘制，滑动后正常显示 ）
            //
            // RANGE(2,2) LayoutState.mCurrentPosition=3
            // final int maxEnd = getMaxEnd(orientationHelper.getEndAfterPadding(), orientationHelper);
            // unusedSpace = maxEnd - offset - padding - margin (offset = 0, padding = 0, margin = 0)
            // result.mConsumed = unusedSpace
            // layoutState.mAvailable -= layoutChunkResultCache.mConsumed;
            // remainingSpace = layoutState.mAvailable = 0
            //
            // case 2:
            // return new StaggeredGridLayoutHelper(3);

            default: {
                return new LinearLayoutHelper();
            }
        }

    }


    public static class ViewHolder1 extends RecyclerView.ViewHolder {

        public ViewHolder1(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder2 extends RecyclerView.ViewHolder {

        public ViewHolder2(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder3 extends RecyclerView.ViewHolder {

        public ViewHolder3(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder4 extends RecyclerView.ViewHolder {

        public ViewHolder4(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder5 extends RecyclerView.ViewHolder {

        public ViewHolder5(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder6 extends RecyclerView.ViewHolder {

        public ViewHolder6(View itemView) {
            super(itemView);
        }
    }


    public static class ViewHolder7 extends RecyclerView.ViewHolder {

        public ViewHolder7(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder8 extends RecyclerView.ViewHolder {

        public ViewHolder8(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder9 extends RecyclerView.ViewHolder {

        public ViewHolder9(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder10 extends RecyclerView.ViewHolder {

        public ViewHolder10(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder11 extends RecyclerView.ViewHolder {

        public ViewHolder11(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder12 extends RecyclerView.ViewHolder {

        public ViewHolder12(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder13 extends RecyclerView.ViewHolder {

        public ViewHolder13(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder14 extends RecyclerView.ViewHolder {

        public ViewHolder14(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder15 extends RecyclerView.ViewHolder {

        public ViewHolder15(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder16 extends RecyclerView.ViewHolder {

        public ViewHolder16(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder17 extends RecyclerView.ViewHolder {

        public ViewHolder17(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder18 extends RecyclerView.ViewHolder {

        public ViewHolder18(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder19 extends RecyclerView.ViewHolder {

        public ViewHolder19(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolder20 extends RecyclerView.ViewHolder {

        public ViewHolder20(View itemView) {
            super(itemView);
        }
    }
}
