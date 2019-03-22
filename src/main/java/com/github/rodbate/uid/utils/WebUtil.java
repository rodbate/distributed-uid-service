package com.github.rodbate.uid.utils;

import com.github.rodbate.uid.common.ImmutableConfig;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 14:20
 */
public final class WebUtil {

    private WebUtil() {
        throw new IllegalStateException("NO INSTANCE");
    }


    /**
     * assert application context closed or not
     */
    public static void assertApplicationContext() {
        if (ImmutableConfig.applicationClosed) {
            throw new AssertionError("application context closed");
        }
    }
}
