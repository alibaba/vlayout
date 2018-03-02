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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * An implement of {@link LayoutHelperFinder} which finds layoutHelpers by position
 */
public class RangeLayoutHelperFinder extends LayoutHelperFinder {

    @NonNull
    private List<LayoutHelperItem> mLayoutHelperItems = new LinkedList<>();

    @NonNull
    private List<LayoutHelper> mLayoutHelpers = new LinkedList<>();

    @NonNull
    private List<LayoutHelper> mReverseLayoutHelpers =new LinkedList<>();

    @NonNull
    private Comparator<LayoutHelperItem> mLayoutHelperItemComparator = new Comparator<LayoutHelperItem>() {
        @Override
        public int compare(LayoutHelperItem lhs, LayoutHelperItem rhs) {
            return lhs.getStartPosition() - rhs.getStartPosition();
        }
    };

    @Override
    protected List<LayoutHelper> reverse() {
        return mReverseLayoutHelpers;
    }

    /**
     * @param layouts layoutHelpers that handled
     */
    @Override
    public void setLayouts(@Nullable List<LayoutHelper> layouts) {
        mLayoutHelpers.clear();
        mReverseLayoutHelpers.clear();
        mLayoutHelperItems.clear();
        if (layouts != null) {
            for (int i = 0, size = layouts.size(); i < size; i++) {
                LayoutHelper helper = layouts.get(i);
                mLayoutHelpers.add(helper);
                mLayoutHelperItems.add(new LayoutHelperItem(helper));
            }
            for (int i = layouts.size() - 1; i >= 0; i--) {
                mReverseLayoutHelpers.add(layouts.get(i));
            }

            Collections.sort(mLayoutHelperItems, mLayoutHelperItemComparator);
        }
    }

    @NonNull
    @Override
    protected List<LayoutHelper> getLayoutHelpers() {
        return mLayoutHelpers;
    }

    @Nullable
    @Override
    public LayoutHelper getLayoutHelper(int position) {
        final int count = mLayoutHelperItems.size();
        if (count == 0) {
            return null;
        }

        int s = 0, e = count - 1, m;
        LayoutHelperItem rs = null;

        // binary search range
        while (s <= e) {
            m = (s + e) / 2;
            rs = mLayoutHelperItems.get(m);
            if (rs.getStartPosition() > position) {
                e = m - 1;
            } else if (rs.getEndPosition() < position) {
                s = m + 1;
            } else if (rs.getStartPosition() <= position && rs.getEndPosition() >= position) {
                break;
            }

            rs = null;
        }

        return rs == null ? null : rs.layoutHelper;
    }

    static class LayoutHelperItem {

        LayoutHelperItem(LayoutHelper helper) {
            this.layoutHelper = helper;
        }

        LayoutHelper layoutHelper;

        public int getStartPosition() {
            return layoutHelper.getRange().getLower();
        }

        public int getEndPosition() {
            return layoutHelper.getRange().getUpper();
        }

    }
}
