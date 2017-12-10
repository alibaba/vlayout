package com.alibaba.android.vlayout;

/**
 * Created by longerian on 2017/12/10.
 *
 * @author longerian
 * @date 2017/12/10
 */

public class Cantor {

    /**
     * @param k1
     * @param k2
     * @return cantor pair for k1 and k2
     */
    public static long getCantor(long k1, long k2) {
        return (k1 + k2) * (k1 + k2 + 1) / 2 + k2;
    }

    /**
     * reverse cantor pair to origin number k1 and k2, k1 is stored in result[0], and k2 is stored in result[1]
     * @param cantor a computed cantor number
     * @param result the array to store output values
     */
    public static void reverseCantor(long cantor, long[] result) {
        if (result == null || result.length < 2) {
            result = new long[2];
        }
        // reverse Cantor Function
        int w = (int) (Math.floor(Math.sqrt(8 * cantor + 1) - 1) / 2);
        int t = (w * w + w) / 2;

        int k2 = (int)(cantor - t);
        int k1 = w - k2;
        result[0] = k1;
        result[1] = k2;
    }

}
