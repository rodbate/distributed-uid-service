package com.github.rodbate.uid.cluster.transport;

/**
 * User: jiangsongsong
 * Date: 2018/12/12
 * Time: 12:19
 */
public interface TransportServer<Processor extends TransportProcessor> {

    /**
     * start server
     */
    void start();

    /**
     * register processor
     *
     * @param key       processor key
     * @param processor processor
     */
    void registerProcessor(final String key, final Processor processor);

    /**
     * shutdown server
     */
    void shutdown();
}
