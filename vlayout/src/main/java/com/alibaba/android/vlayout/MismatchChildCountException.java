package com.alibaba.android.vlayout;

/**
 * Throws when a layoutHelper's range is not match its itemCount
 */
public class MismatchChildCountException extends RuntimeException {


    public MismatchChildCountException(String msg) {
        super(msg);
    }

    public MismatchChildCountException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
