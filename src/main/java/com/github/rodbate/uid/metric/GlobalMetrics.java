package com.github.rodbate.uid.metric;

import com.github.rodbate.uid.allocation.IdAllocationManager;
import com.github.rodbate.uid.enums.BizTypeEnum;
import com.github.rodbate.uid.common.ImmutableConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * User: jiangsongsong
 * Date: 2018/12/27
 * Time: 15:28
 */
@Slf4j
@Component
public final class GlobalMetrics implements InitializingBean, ApplicationContextAware {

    public static final String API_GET_ID_V1_METRIC_NAME = "getIdV1";
    public static final String API_GET_ID_BY_COOKIE_ID_V1_METRIC_NAME = "getIdByCookieIdV1";
    public static final String API_LINK_SECONDARY_ID_TO_COOKIE_ID_V1_METRIC_NAME = "linkSecondaryIdToCookieIdV1";
    public static final String API_LIST_ID_BY_SECONDARY_IDS_V1_METRIC_NAME = "listIdBySecondaryIdsV1";
    public static final String ID_ALLOCATED_COUNTER_METRIC_NAME = "idAllocatedCount";
    public static final String ID_ALLOCATED_TOTAL_COUNTER_METRIC_NAME = "idAllocatedTotalCount";
    public static final String ID_ALLOCATED_CURRENT_ID_METRIC_NAME = "idAllocatedCurrentId";
    public static final String ID_ALLOCATED_MAX_CURRENT_ID_METRIC_NAME = "idAllocatedMaxCurrentId";
    public static final String BULK_INDEX_QUEUE_LENGTH_METRIC_NAME = "bulkIndexQueueLength";
    private static final String ID_ALLOCATED_METRICS_REDIS_PREFIX = "idAllocatedMetrics::";
    private static final String ID_ALLOCATED_TOTAL_COUNTER_METRIC_REDIS_PREFIX = ID_ALLOCATED_METRICS_REDIS_PREFIX + ID_ALLOCATED_TOTAL_COUNTER_METRIC_NAME + "::";
    private static final String ID_ALLOCATED_MAX_CURRENT_ID_METRIC_REDIS_PREFIX = ID_ALLOCATED_METRICS_REDIS_PREFIX + ID_ALLOCATED_MAX_CURRENT_ID_METRIC_NAME + "::";
    private static final Set<String> EXPORT_TO_JMX_METRICS = new HashSet<>();

    static {
        EXPORT_TO_JMX_METRICS.add(API_GET_ID_V1_METRIC_NAME);
        EXPORT_TO_JMX_METRICS.add(API_GET_ID_BY_COOKIE_ID_V1_METRIC_NAME);
        EXPORT_TO_JMX_METRICS.add(API_LINK_SECONDARY_ID_TO_COOKIE_ID_V1_METRIC_NAME);
        EXPORT_TO_JMX_METRICS.add(ID_ALLOCATED_COUNTER_METRIC_NAME);
        EXPORT_TO_JMX_METRICS.add(ID_ALLOCATED_TOTAL_COUNTER_METRIC_NAME);
        EXPORT_TO_JMX_METRICS.add(ID_ALLOCATED_CURRENT_ID_METRIC_NAME);
        EXPORT_TO_JMX_METRICS.add(ID_ALLOCATED_MAX_CURRENT_ID_METRIC_NAME);
        EXPORT_TO_JMX_METRICS.add(BULK_INDEX_QUEUE_LENGTH_METRIC_NAME);
    }


    private final MeterRegistry meterRegistry;
    private final Map<String /* biz type */, Counter> idAllocatedCounters = new HashMap<>();
    private final Map<String /* biz type */, Gauge> idAllocatedTotalCountGauges = new HashMap<>();
    private final Map<String /* biz type */, Gauge> idAllocatedCurrentIdGauges = new HashMap<>();
    private final Map<String /* biz type */, Gauge> idAllocatedMaxCurrentIdGauges = new HashMap<>();
    private final ScheduledExecutorService metricAggregationScheduler;
    private ApplicationContext applicationContext;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public GlobalMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.metricAggregationScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("metric-aggregation-scheduler"));
    }

    /**
     * whether underlying metric name export to jmx
     *
     * @param metricName metric name
     * @return true if metric export to jmx else false
     */
    public static boolean isExportedToJmx(String metricName) {
        return StringUtils.isNotBlank(metricName)
            && (EXPORT_TO_JMX_METRICS.contains(metricName) || EXPORT_TO_JMX_METRICS.stream().anyMatch(metricName::contains));
    }

    private void initMetrics(final IdAllocationManager idAllocationManager) {
        getBizTypes().forEach(bizType -> {
            this.idAllocatedCounters.put(bizType, Counter.builder(ID_ALLOCATED_COUNTER_METRIC_NAME).tag("bizType", bizType).register(this.meterRegistry));
            this.idAllocatedTotalCountGauges.put(bizType,
                Gauge.builder(ID_ALLOCATED_TOTAL_COUNTER_METRIC_NAME, () -> getAllocatedIdTotalCount(bizType)).tag("bizType", bizType).register(this.meterRegistry));
            this.idAllocatedCurrentIdGauges.put(bizType,
                Gauge.builder(ID_ALLOCATED_CURRENT_ID_METRIC_NAME, () -> idAllocationManager.getCurrentAllocatedId(bizType)).tag("bizType", bizType).register(this.meterRegistry));
            this.idAllocatedMaxCurrentIdGauges.put(bizType,
                Gauge.builder(ID_ALLOCATED_MAX_CURRENT_ID_METRIC_NAME, () -> getMaxCurrentAllocatedId(bizType)).tag("bizType", bizType).register(this.meterRegistry));
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Objects.requireNonNull(this.applicationContext, "application context");
        initMetrics(Objects.requireNonNull(this.applicationContext.getBean(IdAllocationManager.class), "IdAllocationManager"));
    }

    /**
     * get id allocated counter
     *
     * @param bizType biz type
     * @return counter
     */
    public Counter getIdAllocatedCounter(String bizType) {
        Counter counter = idAllocatedCounters.get(bizType);
        if (counter == null) {
            log.warn("null id allocated counter for bizType: {}", bizType);
        }
        return counter;
    }


    private double getMaxCurrentAllocatedId(String bizType) {
        String redisKey = ID_ALLOCATED_MAX_CURRENT_ID_METRIC_REDIS_PREFIX + bizType;
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        return opsForHash.values(redisKey).stream().mapToDouble(Double::valueOf).max().orElse(0);
    }


    private double getAllocatedIdTotalCount(String bizType) {
        String redisKey = ID_ALLOCATED_TOTAL_COUNTER_METRIC_REDIS_PREFIX + bizType;
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        return opsForHash.values(redisKey).stream().mapToDouble(Double::valueOf).sum();
    }

    private Set<String> getBizTypes() {
        return Arrays.stream(BizTypeEnum.values()).map(BizTypeEnum::getBizType).collect(Collectors.toSet());
    }

    @PostConstruct
    private void start() {
        this.metricAggregationScheduler.scheduleAtFixedRate(new MetricAggregationTask(), 0, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    private void shutdown() {
        this.metricAggregationScheduler.shutdown();
        try {
            boolean terminated = this.metricAggregationScheduler.awaitTermination(10, TimeUnit.SECONDS);
            if (!terminated) {
                this.metricAggregationScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("awaitTermination InterruptedException", e);
            this.metricAggregationScheduler.shutdownNow();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    private class MetricAggregationTask implements Runnable {

        @Override
        public void run() {
            if (log.isDebugEnabled()) {
                log.debug("MetricAggregationTask start");
            }

            try {
                HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();

                final String hashKey = ImmutableConfig.clusterTransportHost + ":" + ImmutableConfig.clusterTransportPort;
                idAllocatedCounters.forEach((bizType, counter) -> {
                    opsForHash.increment(ID_ALLOCATED_TOTAL_COUNTER_METRIC_REDIS_PREFIX + bizType, hashKey, counter.count());
                });


                idAllocatedCurrentIdGauges.forEach((bizType, gauge) -> {
                    opsForHash.put(ID_ALLOCATED_MAX_CURRENT_ID_METRIC_REDIS_PREFIX + bizType, hashKey, String.valueOf(gauge.value()));
                });

            } catch (Throwable ex) {
                log.error("MetricAggregationTask exception", ex);
            }


            if (log.isDebugEnabled()) {
                log.debug("MetricAggregationTask end");
            }
        }


    }
}
