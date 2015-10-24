package com.alibaba.android.vlayout.test;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;

import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.tmall.wireless.tangram.vlayout.BuildConfig;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

/**
 * Created by villadora on 15/8/13.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class LinearLayoutHelperTest {

    private Activity mActivity;

    private RecyclerView mRecyclerView;

    private VirtualLayoutManager mLayoutManager;

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mRecyclerView = new RecyclerView(mActivity);
        mLayoutManager = new VirtualLayoutManager(mActivity);
    }

    @Test
    public void test_findFirstCompletelyVisibleItem() {
        RecyclerView.Adapter adapter = AdapterBuilder.newBuilder(mActivity).build();
        // Utils.setupLayoutTest(null, adapter, mLayoutManager, mRecyclerView);

        Assert.assertEquals(0, mLayoutManager.findFirstCompletelyVisibleItemPosition());
    }

}

