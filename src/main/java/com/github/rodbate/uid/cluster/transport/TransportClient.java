package com.github.rodbate.uid.cluster.transport;

import java.io.IOException;

/**
 * User: jiangsongsong
 * Date: 2018/12/12
 * Time: 12:19
 */
public interface TransportClient<REQUEST extends Request, RESPONSE extends Response> {

    /**
     * start client
     */
    void start();

    /**
     * send request
     *
     * @param request request
     * @return response
     * @throws IOException io exception
     */
    RESPONSE send(final REQUEST request) throws IOException;

    /**
     * shutdown client
     */
    void shutdown();
}
