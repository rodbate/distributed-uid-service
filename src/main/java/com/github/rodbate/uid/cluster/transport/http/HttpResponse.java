package com.github.rodbate.uid.cluster.transport.http;

import com.github.rodbate.uid.cluster.transport.Response;
import com.github.rodbate.uid.common.web.WebResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * User: jiangsongsong
 * Date: 2018/12/25
 * Time: 14:09
 */
@Getter
@Setter
public class HttpResponse<T> implements Response {
    private boolean success;
    private WebResponse<T> data;
    private String error;
}
