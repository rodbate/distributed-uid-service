package com.github.rodbate.uid.utils;

/**
 * User: jiangsongsong
 * Date: 2018/12/10
 * Time: 12:02
 */
@FunctionalInterface
public interface ExceptionSupplier<T> {
    /**
     * supplier with throw exception
     *
     * @return value
     * @throws Exception exception
     */
    T get() throws Exception;
}
