package com.github.rodbate.uid.cluster.transport.http;

import com.github.rodbate.uid.cluster.transport.Request;
import lombok.Getter;
import lombok.Setter;

/**
 * User: jiangsongsong
 * Date: 2018/12/25
 * Time: 14:08
 */
@Getter
@Setter
public class HttpRequest implements Request {
    private String api;
    private String remoteHost;
    private int remotePort;
}
