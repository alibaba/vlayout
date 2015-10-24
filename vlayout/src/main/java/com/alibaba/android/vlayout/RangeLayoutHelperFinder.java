package com.alibaba.android.vlayout;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class RangeLayoutHelperFinder extends LayoutHelperFinder {

    @NonNull
    private List<LayoutHelperItem> mLayoutHelperItems = new LinkedList<>();

    @NonNull
    private List<LayoutHelper> mLayoutHelpers = new LinkedList<>();

    @Override
    public Iterator<LayoutHelper> iterator() {
        return Collections.unmodifiableList(mLayoutHelpers).iterator();
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

            Collections.sort(mLayoutHelperItems, new Comparator<LayoutHelperItem>() {
                @Override
                public int compare(LayoutHelperItem lhs, LayoutHelperItem rhs) {
                    return lhs.getStartPosition() - rhs.getStartPosition();
                }
            });

            Collections.sort(mLayoutHelpers, new Comparator<LayoutHelper>() {
                @Override
                public int compare(LayoutHelper lhs, LayoutHelper rhs) {
                    return lhs.getZIndex() - rhs.getZIndex();
                }
            });
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
        final int helperCount = mLayoutHelperItems.size();
        if (helperCount == 0) {
            return null;
        }

        int s = 0, e = helperCount - 1, m;
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
