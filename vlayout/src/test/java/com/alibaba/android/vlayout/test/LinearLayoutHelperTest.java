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

package com.alibaba.android.vlayout.test;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;

import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.vlayout.BuildConfig;

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

