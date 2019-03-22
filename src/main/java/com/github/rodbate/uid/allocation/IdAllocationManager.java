package com.github.rodbate.uid.allocation;

import com.alibaba.fastjson.JSON;
import com.github.rodbate.bigdata.idgenerator.cluster.transport.http.*;
import com.github.rodbate.uid.cluster.transport.http.*;
import com.github.rodbate.uid.enums.BizTypeEnum;
import com.github.rodbate.uid.web.dao.DistIdAllocationDao;
import com.github.rodbate.uid.cluster.Node;
import com.github.rodbate.uid.cluster.NodeInfo;
import com.github.rodbate.uid.cluster.NotLeaderException;
import com.github.rodbate.uid.cluster.transport.TransportClient;
import com.github.rodbate.uid.cluster.transport.TransportServer;
import com.globalegrow.bigdata.idgenerator.cluster.transport.http.*;
import com.github.rodbate.uid.common.web.ReturnCode;
import com.github.rodbate.uid.common.web.WebResponse;
import com.github.rodbate.uid.exceptions.IdGeneratorServiceException;
import com.github.rodbate.uid.metric.GlobalMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.netty.http.server.HttpServerRequest;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: jiangsongsong
 * Date: 2018/12/12
 * Time: 15:05
 */
@Slf4j
@Component
public class IdAllocationManager {

    private final AtomicBoolean initLock = new AtomicBoolean(false);
    private final Map<String /* biz type */, Object /* lock */> bizTypeToLockTable = new HashMap<>();
    private final Object allocateLock = new Object();
    private final Map<String /* biz type */, Pair<Long /* current allocate id*/, Long /* max allocatable id */>> bizTypeToAllocatedIdTable = new HashMap<>();
    private final Node node;
    private final DistIdAllocationDao distIdAllocationDao;

    //transport
    private final TransportServer<?> transportServer;
    private final TransportClient<HttpRequest, HttpResponse<Long>> transportClient;

    //metric executor
    private final ExecutorService metricExecutor;
    private final BlockingQueue<Runnable> metricExecutorQueue = new LinkedBlockingQueue<>(100000);


    @Autowired
    private Scheduler reactorServiceScheduler;

    @Autowired
    private GlobalMetrics globalMetrics;

    public IdAllocationManager(final Node node, final DistIdAllocationDao distIdAllocationDao) {
        this.node = node;
        this.distIdAllocationDao = distIdAllocationDao;
        this.initLocks();

        this.metricExecutor = new ThreadPoolExecutor(
            1,
            1,
            1,
            TimeUnit.MINUTES,
            metricExecutorQueue,
            new NamedThreadFactory("id-allocation-metric-executor"),
            new ThreadPoolExecutor.DiscardPolicy()
        );

        //init transport
        this.transportServer = initTransportServer();
        this.transportClient = new HttpTransportClient();
    }

    private void initLocks() {
        if (this.initLock.compareAndSet(false, true)) {
            for (BizTypeEnum bizTypeEnum : BizTypeEnum.values()) {
                this.bizTypeToLockTable.put(bizTypeEnum.getBizType(), new Object());
            }
        }
    }


    /**
     * get current allocated id for metrics
     *
     * @param bizType biz type
     * @return current id
     */
    public double getCurrentAllocatedId(String bizType) {
        final Object lock = getLock(bizType);
        synchronized (lock) {
            Pair<Long, Long> pair = this.bizTypeToAllocatedIdTable.get(bizType);
            if (pair != null) {
                return pair.getValue0();
            }
        }
        return 0;
    }


    private TransportServer<?> initTransportServer() {
        HttpTransportServer server = new HttpTransportServer(this.node);
        server.registerProcessor(HttpTransportConstant.GET_ALLOCATED_ID_API_TEMPLATE, new GetAllocatedIdProcessor());
        return server;
    }

    /**
     * allocate id
     *
     * @param bizType biz type
     * @param step    step
     * @return id
     */
    public long nextId(final String bizType, final int step) {
        final Object lock = getLock(bizType);
        synchronized (lock) {
            Pair<Long, Long> pair = this.bizTypeToAllocatedIdTable.get(bizType);
            long currentAllocateId;
            long nextAllocateId;
            long maxAllocatableId;
            if (pair == null) {
                //fetch max id from repository
                currentAllocateId = fetchAllocatedMaxId(bizType, step);
                nextAllocateId = currentAllocateId + 1;
                maxAllocatableId = currentAllocateId + step - 1;
            } else {
                currentAllocateId = pair.getValue0();
                nextAllocateId = currentAllocateId + 1;
                maxAllocatableId = pair.getValue1();
                if (nextAllocateId > maxAllocatableId) {
                    nextAllocateId = fetchAllocatedMaxId(bizType, step);
                    maxAllocatableId = nextAllocateId + step - 1;
                }
            }
            this.bizTypeToAllocatedIdTable.put(bizType, Pair.with(nextAllocateId, maxAllocatableId));

            this.metricExecutor.execute(() -> {
                //metric
                Counter counter = globalMetrics.getIdAllocatedCounter(bizType);
                if (counter != null) {
                    counter.increment();
                }
            });

            return currentAllocateId;
        }
    }


    /**
     * allocate next id async
     *
     * @param bizType biz type
     * @param step    step
     * @return id
     */
    public Mono<Long> nextIdAsync(final String bizType, final int step) {
        return Mono.fromCallable(() -> nextId(bizType, step)).publishOn(reactorServiceScheduler);
    }


    private long fetchAllocatedMaxId(final String bizType, final int step) {
        long id;
        if (this.node.isLeader()) {
            id = incr(bizType, step);
        } else {
            //transport
            final HttpRequest request = new HttpRequest();
            request.setApi(String.format("%s/%s/%d", HttpTransportConstant.GET_ALLOCATED_ID_API, bizType, step));
            NodeInfo leader = this.node.getLeader();
            request.setRemoteHost(leader.getHost());
            request.setRemotePort(leader.getPort());
            Long allocatedId = null;
            Throwable ex = null;
            int retryTime = 0;
            while (retryTime < 3) {
                ++retryTime;
                try {
                    HttpResponse<Long> response = this.transportClient.send(request);
                    log.info("transport client response: {}", JSON.toJSONString(response));
                    if (response.isSuccess() && response.getData().getCode() == ReturnCode.OK.getCode()) {
                        allocatedId = response.getData().getData();
                        break;
                    }
                } catch (IOException e) {
                    log.error("transport client send io exception", e);
                    ex = e;
                } catch (Throwable e) {
                    log.error("transport client send exception", e);
                    ex = e;
                }
            }

            if (allocatedId == null) {
                if (ex != null) {
                    throw new IdGeneratorServiceException(ReturnCode.INTERNAL_SERVER_ERROR, ex);
                } else {
                    throw new IdGeneratorServiceException(ReturnCode.INTERNAL_SERVER_ERROR);
                }
            }
            id = allocatedId;
        }
        return id;
    }


    private long fetchAllocatedIdLocally(final String bizType, final int step) {
        if (!this.node.isLeader()) {
            throw new NotLeaderException(this.node.getMyNode());
        }
        return incr(bizType, step);
    }

    private long incr(final String bizType, final int step) {
        synchronized (this.allocateLock) {
            return this.distIdAllocationDao.incr(bizType, step);
        }
    }

    private Object getLock(final String bizType) {
        final Object lock = this.bizTypeToLockTable.get(bizType);
        if (lock == null) {
            throw new IllegalStateException(String.format("cannot get lock for bizType=%s", bizType));
        }
        return lock;
    }

    @PostConstruct
    private void start() {
        this.transportServer.start();
        this.transportClient.start();
    }

    @PreDestroy
    private void shutdown() {
        this.transportClient.shutdown();
        this.transportServer.shutdown();

        this.metricExecutor.shutdown();
        try {
            boolean terminated = this.metricExecutor.awaitTermination(10, TimeUnit.SECONDS);
            if (!terminated) {
                this.metricExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
            this.metricExecutor.shutdownNow();
        }
    }


    private class GetAllocatedIdProcessor implements HttpTransportProcessor {

        @Override
        public Publisher<Void> process(HttpTransportContext context) {
            HttpServerRequest request = context.getHttpServerRequest();
            String bizType = request.param(HttpTransportConstant.PARAM_BIZ_TYPE_NAME);
            String stepStr = request.param(HttpTransportConstant.PARAM_STEP_NAME);

            WebResponse<?> response;
            if (StringUtils.isBlank(bizType) || StringUtils.isBlank(stepStr)) {
                response = WebResponse.buildErrorResponse(
                    ReturnCode.BAD_REQUEST,
                    String.format("param %s require not null", StringUtils.isBlank(bizType) ? HttpTransportConstant.PARAM_BIZ_TYPE_NAME : HttpTransportConstant.PARAM_STEP_NAME)
                );
            } else {
                int step = -1;
                try {
                    step = Integer.valueOf(stepStr);
                } catch (NumberFormatException ex) {
                    log.error("invalid param step: " + stepStr, ex);
                }
                if (step <= 0) {
                    response = WebResponse.buildErrorResponse(
                        ReturnCode.BAD_REQUEST,
                        String.format("param [%s=%s] invalid", HttpTransportConstant.PARAM_STEP_NAME, stepStr));
                } else {
                    try {
                        response = WebResponse.buildSuccessResponse(fetchAllocatedIdLocally(bizType, step));
                    } catch (NotLeaderException ex) {
                        log.error(ex.getMessage(), ex);
                        response = WebResponse.buildErrorResponse(ReturnCode.NOT_LEADER, "not leader", ex);
                    }
                }
            }
            context.getHttpServerResponse().chunkedTransfer(false);
            return context.getHttpServerResponse().sendString(Flux.just(JSON.toJSONString(response)));
        }
    }
}
