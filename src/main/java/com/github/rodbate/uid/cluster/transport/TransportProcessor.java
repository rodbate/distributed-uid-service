package com.github.rodbate.uid.cluster.transport;

/**
 * User: jiangsongsong
 * Date: 2018/12/25
 * Time: 9:57
 */
public interface TransportProcessor<CONTEXT extends TransportContext, R> {

    /**
     * process
     *
     * @param context context
     * @return process ret data
     */
    R process(final CONTEXT context);
}
