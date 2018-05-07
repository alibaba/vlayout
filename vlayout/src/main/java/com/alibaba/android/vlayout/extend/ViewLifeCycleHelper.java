package com.alibaba.android.vlayout.extend;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.view.View;

import com.alibaba.android.vlayout.VirtualLayoutManager;

public class ViewLifeCycleHelper {
    public enum STATUS {
        APPEARING,
        APPEARED,
        DISAPPEARING,
        DISAPPEARED
    }

    private ArrayMap<View, STATUS> mViewStatusMap = new ArrayMap<>();

    private ViewLifeCycleListener mViewLifeCycleListener;

    private VirtualLayoutManager mVirtualLayoutManager;

    private int scrHeight;

    public ViewLifeCycleHelper(VirtualLayoutManager virtualLayoutManager, @NonNull ViewLifeCycleListener mViewLifeCycleListener) {
        this.mViewLifeCycleListener = mViewLifeCycleListener;
        this.mVirtualLayoutManager = virtualLayoutManager;
    }

    public void checkViewStatusInScreen() {
        for (int i = 0; i < mVirtualLayoutManager.getChildCount(); i++) {
            View view = mVirtualLayoutManager.getChildAt(i);
            if (scrHeight == 0) {
                scrHeight = view.getContext().getResources().getDisplayMetrics().heightPixels;
            }
//            Log.e("huang", "checkViewStatusInScreen: view=" + view.getTag(R.id.tag_layout_helper_bg) + " rect=" + rect.toString());

            if (mVirtualLayoutManager.getVirtualLayoutDirection() == VirtualLayoutManager.LayoutState.LAYOUT_END) {
                if (view.getTop() < 0 && view.getBottom() > 0 && isViewReadyDisAppear(view)) {
//                    Log.e("huang", "LAYOUT_END checkViewDisAppear: " + rect.toString() + "view=" + view.getTag(R.id.tag_layout_helper_bg));
                    setViewDisappearing(view);
                } else if (view.getTop() < scrHeight && view.getBottom() > scrHeight && isViewReadyAppear(view)) {
//                    Log.e("huang", "LAYOUT_END checkViewAppear: " + rect.toString() + "view=" + view.getTag(R.id.tag_layout_helper_bg));
                    setViewAppearing(view);
                }
            } else {
                if (view.getTop() < 0 && view.getBottom() > 0 && isViewReadyAppear(view)) {
//                    Log.e("huang", "LAYOUT_START checkViewAppear: " + rect.toString() + "view=" + view.getTag(R.id.tag_layout_helper_bg));
                    setViewAppearing(view);
                } else if (view.getTop() < scrHeight && view.getBottom() > scrHeight && isViewReadyDisAppear(view)) {
//                    Log.e("huang", "LAYOUT_START checkViewDisAppear: " + rect.toString() + "view=" + view.getTag(R.id.tag_layout_helper_bg));
                    setViewDisappearing(view);
                }
            }

            if (view.getTop() > 0 && view.getBottom() < scrHeight) {
                // fully in screen

                if (isViewReadyAppear(view)) {
                    // if not appeared, call appear
                    setViewAppearing(view);
//                    Log.e("huang", "Add appear: " + rect.toString() + " view=" + view.getTag(R.id.tag_layout_helper_bg));

                } else {
                    if (getViewStatus(view) != STATUS.APPEARED) {
                        setViewAppeared(view);
//                        Log.e("huang", "Appeared: " + rect.toString() + " view=" + view.getTag(R.id.tag_layout_helper_bg));
                    }
                }
            } else if (view.getBottom() < 0 || view.getTop() > scrHeight) {
                // not in screen
                if (isViewReadyDisAppear(view)) {
                    // if not disappeared, call disappear
                    setViewDisappearing(view);
//                    Log.e("huang", "Add disappear:" + rect.toString() + " view=" + view.getTag(R.id.tag_layout_helper_bg));

                } else {
                    if (getViewStatus(view) != STATUS.DISAPPEARED) {
                        setViewDisappeared(view);
//                        Log.e("huang", "Disappeared: " + rect.toString() + " view=" + view.getTag(R.id.tag_layout_helper_bg));
                    }
                }
            }
        }
    }

    private STATUS getViewStatus(View view) {
        if (!mViewStatusMap.containsKey(view)) {
            mViewStatusMap.put(view, STATUS.DISAPPEARED);
            return STATUS.DISAPPEARED;
        }
        return mViewStatusMap.get(view);
    }

    private void setViewstatus(View view, STATUS status) {
        mViewStatusMap.put(view, status);
    }

    private boolean isViewReadyAppear(View view) {
        return getViewStatus(view) == STATUS.DISAPPEARED;
    }

    private void setViewAppearing(View view) {
        if (getViewStatus(view) == STATUS.APPEARING) {
            return;
        }

        setViewstatus(view, STATUS.APPEARING);
        if (null != mViewLifeCycleListener) {
            mViewLifeCycleListener.onAppearing(view);
        }
    }

    private void setViewAppeared(View view) {
        if (getViewStatus(view) == STATUS.APPEARED) {
            return;
        }
        setViewstatus(view, STATUS.APPEARED);
        if (null != mViewLifeCycleListener) {
            mViewLifeCycleListener.onAppeared(view);
        }
    }

    private boolean isViewReadyDisAppear(View view) {
        return getViewStatus(view) == STATUS.APPEARED;
    }

    private void setViewDisappearing(View view) {
        if (getViewStatus(view) == STATUS.DISAPPEARING) {
            return;
        }

        setViewstatus(view, STATUS.DISAPPEARING);
        if (null != mViewLifeCycleListener) {
            mViewLifeCycleListener.onDisappearing(view);
        }
    }

    private void setViewDisappeared(View view) {
        if (getViewStatus(view) == STATUS.DISAPPEARED) {
            return;
        }
        setViewstatus(view, STATUS.DISAPPEARED);
        if (null != mViewLifeCycleListener) {
            mViewLifeCycleListener.onDisappeared(view);
        }
    }
}
