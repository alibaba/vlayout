package com.alibaba.android.vlayout;

import android.support.v7.widget.RecyclerView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by villadora on 16/2/24.
 */
public class ViewHolderHelper {

    private static Field vhField = null;


    public static void setField(RecyclerView.ViewHolder holder, String fieldName, Object value) {
        try {
            Field f = RecyclerView.LayoutParams.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(holder, value);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public static RecyclerView.ViewHolder getViewHolder(RecyclerView.LayoutParams params) {
        try {
            if (vhField == null) {
                vhField = RecyclerView.LayoutParams.class.getDeclaredField("mViewHolder");
                vhField.setAccessible(true);
            }

            //vhField.set(params, holder);

            return (RecyclerView.ViewHolder) vhField.get(params);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }


    private static Method vhSetFlags;

    public static void addViewHolderFlag(RecyclerView.ViewHolder holder, int flag) {
        try {
            if (vhSetFlags == null) {
                vhSetFlags = RecyclerView.ViewHolder.class.getDeclaredMethod("addFlags", int.class);
                vhSetFlags.setAccessible(true);
            }

            vhSetFlags.invoke(holder, flag);
        } catch (Exception e) {

        }
    }
}
