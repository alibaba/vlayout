package com.alibaba.android.vlayout.extend;

import androidx.annotation.Keep;
import android.view.View;

/**
 * Add callback during measure and layout, help you to monitor your view's performance.<br />
 * Designed as Class instead of Interface is able to extend api in future. <br />
 *
 * Created by longerian on 2018/5/16.
 *
 * @author longerian
 * @date 2018/05/16
 */
public class PerformanceMonitor {

    /**
     * Record the start time
     * @param phase
     * @param viewType
     */
    @Keep
    public void recordStart(String phase, String viewType) {

    }

    /**
     * Record the end time
     * @param phase
     * @param viewType
     */
    @Keep
    public void recordEnd(String phase, String viewType) {

    }

    /**
     * Record the start time
     * @param phase
     * @param view
     */
    @Keep
    public void recordStart(String phase, View view) {

    }

    /**
     * Record the end time
     * @param phase
     * @param view
     */
    @Keep
    public void recordEnd(String phase, View view) {

    }

}
