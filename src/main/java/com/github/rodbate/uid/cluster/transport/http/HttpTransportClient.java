package com.github.rodbate.uid.cluster.transport.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.rodbate.uid.utils.OkHttpClientUtils;
import com.github.rodbate.uid.cluster.transport.TransportClient;
import com.github.rodbate.uid.common.web.WebResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * User: jiangsongsong
 * Date: 2018/12/12
 * Time: 12:21
 */
@Slf4j
public class HttpTransportClient implements TransportClient<HttpRequest, HttpResponse<Long>> {


    @Override
    public void start() {

    }

    @Override
    public HttpResponse<Long> send(HttpRequest request) throws IOException {
        Request req = new Request.Builder()
            .url(buildUrl(request))
            .get()
            .build();
        HttpResponse<Long> httpResponse = new HttpResponse<>();
        Response response = OkHttpClientUtils.getHttpClient().newCall(req).execute();
        httpResponse.setSuccess(response.isSuccessful());
        if (response.body() != null) {
            if (response.isSuccessful()) {
                httpResponse.setData(JSON.parseObject(response.body().string(), new TypeReference<WebResponse<Long>>() {
                }));
            } else {
                httpResponse.setError(response.body().string());
            }
        }
        return httpResponse;
    }


    private String buildUrl(HttpRequest request) {
        return "http://" + request.getRemoteHost() + ":" + request.getRemotePort() + request.getApi();
    }


    @Override
    public void shutdown() {

    }
}
