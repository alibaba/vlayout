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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
  An implement of {@link LayoutHelperFinder} which finds layoutHelpers by position
 */
public class RangeLayoutHelperFinder extends LayoutHelperFinder {

    @NonNull
    private List<LayoutHelperItem> mLayoutHelperItems = new LinkedList<>();

    @NonNull
    private List<LayoutHelper> mLayoutHelpers = new LinkedList<>();

    @NonNull
    private Comparator<LayoutHelperItem> mLayoutHelperItemComparator = new Comparator<LayoutHelperItem>() {
        @Override
        public int compare(LayoutHelperItem lhs, LayoutHelperItem rhs) {
            return lhs.getStartPosition() - rhs.getStartPosition();
        }
    };

    private Comparator<LayoutHelper> mLayoutHelperComparator = new Comparator<LayoutHelper>() {
        @Override
        public int compare(LayoutHelper lhs, LayoutHelper rhs) {
            return lhs.getZIndex() - rhs.getZIndex();
        }
    };

    @Override
    public Iterator<LayoutHelper> iterator() {
        return Collections.unmodifiableList(mLayoutHelpers).iterator();
    }

    @Override
    protected Iterable<LayoutHelper> reverse() {
        final ListIterator<LayoutHelper> i = mLayoutHelpers.listIterator(mLayoutHelpers.size());
        return new Iterable<LayoutHelper>() {
            @Override
            public Iterator<LayoutHelper> iterator() {
                return new Iterator<LayoutHelper>() {
                    public boolean hasNext() {
                        return i.hasPrevious();
                    }

                    public LayoutHelper next() {
                        return i.previous();
                    }

                    public void remove() {
                        i.remove();
                    }
                };
            }
        };
    }

    /**
     * @param layouts layoutHelpers that handled
     */
    @Override
    public void setLayouts(@Nullable List<LayoutHelper> layouts) {
        mLayoutHelpers.clear();
        mLayoutHelperItems.clear();
        if (layouts != null) {
            for (LayoutHelper helper : layouts) {
                Range<Integer> acceptRange = helper.getRange();
                mLayoutHelpers.add(helper);
                mLayoutHelperItems.add(new LayoutHelperItem(acceptRange, helper));
            }

            Collections.sort(mLayoutHelperItems, mLayoutHelperItemComparator);

            Collections.sort(mLayoutHelpers, mLayoutHelperComparator);
        }
    }

    @NonNull
    @Override
    protected List<LayoutHelper> getLayoutHelpers() {
        return Collections.unmodifiableList(mLayoutHelpers);
    }

    @Nullable
    @Override
    protected LayoutHelper getLayoutHelper(int position) {
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
            } else if (rs.getStartPosition() <= position && rs.getEndPosition() >= position)
                break;

            rs = null;
        }

        return rs == null ? null : rs.layoutHelper;
    }


    static class LayoutHelperItem {

        LayoutHelperItem(Range<Integer> range, LayoutHelper helper) {
            this.range = range;
            this.layoutHelper = helper;
        }

        Range<Integer> range;
        LayoutHelper layoutHelper;

        public int getStartPosition() {
            return range.getLower();
        }

        public int getEndPosition() {
            return range.getUpper();
        }

    }
}
