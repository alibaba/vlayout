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

import androidx.recyclerview.widget.RecyclerView;

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
