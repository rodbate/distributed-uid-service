package com.github.rodbate.uid.cluster.transport.http;

import com.github.rodbate.uid.cluster.transport.TransportContext;
import com.github.rodbate.uid.cluster.Node;
import lombok.Getter;
import lombok.Setter;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * User: jiangsongsong
 * Date: 2018/12/25
 * Time: 10:14
 */
@Getter
@Setter
public class HttpTransportContext implements TransportContext {
    private String api;
    private Node node;
    private HttpServerRequest httpServerRequest;
    private HttpServerResponse httpServerResponse;

    public HttpTransportContext() {
    }

    public HttpTransportContext(String api, Node node, HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        this.api = api;
        this.node = node;
        this.httpServerRequest = httpServerRequest;
        this.httpServerResponse = httpServerResponse;
    }
}
