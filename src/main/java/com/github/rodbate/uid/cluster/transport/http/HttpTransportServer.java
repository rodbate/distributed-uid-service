package com.github.rodbate.uid.cluster.transport.http;

import com.alibaba.fastjson.JSON;
import com.github.rodbate.uid.cluster.Node;
import com.github.rodbate.uid.cluster.transport.TransportServer;
import com.github.rodbate.uid.common.ImmutableConfig;
import com.github.rodbate.uid.common.web.ReturnCode;
import com.github.rodbate.uid.common.web.WebResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: jiangsongsong
 * Date: 2018/12/12
 * Time: 12:20
 */
@Slf4j
public class HttpTransportServer implements TransportServer<HttpTransportProcessor> {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Map<String, HttpTransportProcessor> processors = new HashMap<>();
    private final HttpServer httpServer;
    private final Node node;
    private DisposableServer disposableServer;

    public HttpTransportServer(final Node node) {
        this.node = node;
        this.httpServer = createServer();
    }


    private HttpServer createServer() {
        return HttpServer.create()
            .host(ImmutableConfig.clusterTransportHost)
            .port(ImmutableConfig.clusterTransportPort)
            .wiretap(true);

    }

    @Override
    public void start() {
        if (this.running.compareAndSet(false, true)) {
            this.disposableServer = this.httpServer
                .route(routes -> {
                    this.processors.forEach((api, processor) -> {
                        routes.get(api, (request, response) -> {
                            try {
                                response.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
                                return processor.process(new HttpTransportContext(api, node, request, response));
                            } catch (Throwable ex) {
                                log.error(String.format("process api[%s] exception", api), ex);
                                WebResponse resp = WebResponse.buildErrorResponse(ReturnCode.INTERNAL_SERVER_ERROR, ReturnCode.INTERNAL_SERVER_ERROR.getDefaultMsg(), ex);
                                response.chunkedTransfer(false);
                                return response.sendString(Flux.just(JSON.toJSONString(resp)));
                            }
                        });
                    });
                })
                .bind()
                .block();
        }
    }

    @Override
    public void registerProcessor(final String key, final HttpTransportProcessor processor) {
        if (this.running.get()) {
            throw new RuntimeException("http transport server has already started, please register processor before started");
        }
        if (this.stopped.get()) {
            throw new RuntimeException("http transport server has already stopped");
        }
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(processor, "processor");
        this.processors.put(key, processor);
    }

    @Override
    public void shutdown() {
        if (this.stopped.compareAndSet(false, true)) {
            this.disposableServer.disposeNow(Duration.ofSeconds(5));
            this.running.set(false);
        }
    }

}
