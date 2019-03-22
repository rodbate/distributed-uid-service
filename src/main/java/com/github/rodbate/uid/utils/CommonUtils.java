package com.github.rodbate.uid.utils;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * User: jiangsongsong
 * Date: 2018/12/24
 * Time: 12:22
 */
public final class CommonUtils {

    public static final String EMPTY_STRING = "";

    private CommonUtils() {
    }

    /**
     * convert collection to string
     *
     * @param collection collection
     * @return collection string
     */
    public static String collectionToString(final Collection<?> collection) {
        return collectionToString(collection, ", ", "[", "]");
    }

    /**
     * convert collection to string
     *
     * @param collection collection
     * @param delimiter  delimiter
     * @param prefix     prefix
     * @param suffix     suffix
     * @return collection string
     */
    public static String collectionToString(final Collection<?> collection, String delimiter, String prefix, String suffix) {
        if (collection == null || collection.size() == 0) {
            return EMPTY_STRING;
        }
        return collection.stream().map(Object::toString).collect(Collectors.joining(delimiter, prefix, suffix));
    }
}
