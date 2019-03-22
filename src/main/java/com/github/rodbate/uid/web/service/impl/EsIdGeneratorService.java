package com.github.rodbate.uid.web.service.impl;

import com.github.rodbate.uid.enums.BizTypeEnum;
import com.github.rodbate.uid.utils.CommonUtils;
import com.github.rodbate.uid.web.config.DistIdStepConfig;
import com.github.rodbate.uid.web.service.IdGeneratorService;
import com.github.rodbate.uid.allocation.IdAllocationManager;
import com.github.rodbate.uid.common.AbstractServiceThread;
import com.github.rodbate.uid.common.ImmutableConfig;
import com.github.rodbate.uid.common.web.ApplicationContextAssertable;
import com.github.rodbate.uid.common.web.ReturnCode;
import com.github.rodbate.uid.exceptions.IdGeneratorServiceException;
import com.github.rodbate.uid.web.dto.request.LinkSecondaryIdToCookieIdRequest;
import com.github.rodbate.uid.web.dto.request.ListIdBySecondaryIdsRequest;
import com.github.rodbate.uid.web.dto.response.GetIdByCookieIdResponse;
import com.github.rodbate.uid.web.dto.response.GetIdResponse;
import com.github.rodbate.uid.web.dto.response.ListIdBySecondaryIdsResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.github.rodbate.uid.common.Constants.CPU_CORE_NUM;
import static com.github.rodbate.uid.metric.GlobalMetrics.BULK_INDEX_QUEUE_LENGTH_METRIC_NAME;

/**
 * User: jiangsongsong
 * Date: 2018/12/29
 * Time: 15:03
 */
@Slf4j
public class EsIdGeneratorService extends ApplicationContextAssertable implements IdGeneratorService {

    private static final String REDIS_HASH_PREFIX_KEY = "id-generator-guid-";
    private static final String COOKIE_ID_INDEX_NAME = "id_generator_service_global_unique_id_cookie";
    private static final int ID_INDEX_SHARD_COUNT = 5;
    private static final String ID_TYPE = "uniqueId";
    private static final String COOKIE_ID_FIELD_NAME = "cookieId";
    private static final int DELETE_REDIS_KEY_TASK_DELAY_MILLIS = 2000;
    private final BulkIndexService bulkIndexService;
    private final ScheduledExecutorService deleteCookieIdsFromRedisScheduler =
        Executors.newScheduledThreadPool(CPU_CORE_NUM, new NamedThreadFactory("deleteCookieIdsFromRedisScheduler"));
    private final AtomicInteger deleteCookieIdTaskTime = new AtomicInteger(0);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean shutdowning = new AtomicBoolean(false);
    private final AtomicBoolean shutdownCompleted = new AtomicBoolean(false);
    @Autowired
    private IdAllocationManager idAllocationManager;
    @Autowired
    private DistIdStepConfig distIdStepConfig;
    @Autowired
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private Client esClient;
    @Autowired
    private Scheduler reactorServiceScheduler;

    public EsIdGeneratorService(MeterRegistry meterRegistry) {
        Gauge.builder(BULK_INDEX_QUEUE_LENGTH_METRIC_NAME, this::getIndexQueueLength).register(meterRegistry);
        this.bulkIndexService = new BulkIndexService();
    }


    @Override
    public Mono<GetIdResponse> getId(String bizType) {
        return idAllocationManager.nextIdAsync(bizType, distIdStepConfig.getStepWithCheckBizType(bizType)).map(id -> new GetIdResponse(id.toString()));
    }

    @Override
    public Mono<GetIdByCookieIdResponse> getIdByCookieId(final String cookieId) {
        if (ImmutableConfig.applicationClosed) {
            log.info("applicationClosed: {}", ImmutableConfig.applicationClosed);
            return Mono.error(new IdGeneratorServiceException(ReturnCode.APPLICATION_SHUTDOWN));
        }
        Objects.requireNonNull(cookieId, "cookieId");
        return Mono.fromCallable(() -> queryOne(COOKIE_ID_FIELD_NAME, cookieId))
            .publishOn(reactorServiceScheduler)
            .switchIfEmpty(idGenAndPersist(cookieId))
            .map(GetIdByCookieIdResponse::new);
    }

    @Override
    public Mono<Void> linkSecondaryIdToCookieId(Mono<LinkSecondaryIdToCookieIdRequest> request) {
        return request.flatMap(req -> Mono.fromRunnable(() -> {
            String id = queryOne(COOKIE_ID_FIELD_NAME, req.getCookieId());
            if (id == null) {
                throw new IdGeneratorServiceException(ReturnCode.NOT_FOUND_FOR_COOKIE_ID, req.getCookieId());
            }

            final Map<String, Object> toUpdateFields = new HashMap<>();
            Optional.ofNullable(req.getSecondaryIds()).orElse(Collections.emptyList())
                .forEach(entry -> {
                    toUpdateFields.put(entry.getSecondaryIdName(), entry.getSecondaryIdValue());
                });

            //update mapping
            updateIndexMapping(toUpdateFields.keySet());

            //update fields
            update(id, toUpdateFields);

        }).publishOn(reactorServiceScheduler).then(Mono.empty()));
    }

    @Override
    public Mono<ListIdBySecondaryIdsResponse> listIdBySecondaryIds(Mono<ListIdBySecondaryIdsRequest> request) {
        return request.flatMap(req -> Mono.fromCallable(() -> queryList(getSecondaryIdName(req.getSecondaryIdName()), new HashSet<>(req.getSecondaryIdValues()))).publishOn(reactorServiceScheduler))
            .map(map -> map.entrySet().stream().map(entry -> new ListIdBySecondaryIdsResponse.Entry(entry.getKey(), entry.getValue())).collect(Collectors.toList()))
            .map(ListIdBySecondaryIdsResponse::new);
    }

    private String getSecondaryIdName(String secondaryIdName) {
        return StringUtils.isBlank(secondaryIdName) ? COOKIE_ID_FIELD_NAME : secondaryIdName;
    }

    //with redis and es
    private Mono<String> idGenAndPersist(String cookieId) {
        final String redisKey = getRedisHashKey(BizTypeEnum.COOKIE_ID);
        return reactiveStringRedisTemplate.<String, String>opsForHash()
            .get(redisKey, cookieId)
            .switchIfEmpty(ifRedisNotExist(cookieId, redisKey));
    }


    private Mono<String> ifRedisNotExist(final String cookieId, final String redisKey) {
        return idAllocationManager.nextIdAsync(BizTypeEnum.COOKIE_ID.getBizType(), distIdStepConfig.getStep(BizTypeEnum.COOKIE_ID))
            .flatMap(uid -> reactiveStringRedisTemplate.<String, String>opsForHash().putIfAbsent(redisKey, cookieId, String.valueOf(uid))
                .flatMap(rs -> {
                    if (rs) {
                        //index to es
                        this.bulkIndexService.offerIndex(new IndexEntry(String.valueOf(uid), cookieId));
                        return Mono.just(String.valueOf(uid));
                    } else {
                        //get from redis
                        return reactiveStringRedisTemplate.opsForValue().get(redisKey);
                    }
                }));
    }

    private String getRedisHashKey(BizTypeEnum bizTypeEnum) {
        return REDIS_HASH_PREFIX_KEY + bizTypeEnum.getBizType();
    }

    private void updateIndexMapping(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName");
        boolean acknowledged = esClient.admin().indices().preparePutMapping(COOKIE_ID_INDEX_NAME)
            .setType(ID_TYPE)
            .setSource(fieldName, "type=keyword")
            .execute().actionGet().isAcknowledged();
        if (!acknowledged) {
            throw new RuntimeException(String.format("failed to update index mapping: {index=%s, field=%s}", COOKIE_ID_INDEX_NAME, fieldName));
        }
    }

    private void updateIndexMapping(Set<String> fieldNames) {
        Objects.requireNonNull(fieldNames, "fieldNames");
        int size;
        if ((size = fieldNames.size()) > 0) {
            List<Object> sources = new ArrayList<>(size * 2);
            fieldNames.forEach(name -> {
                sources.add(name);
                sources.add("type=keyword");
            });

            boolean acknowledged = esClient.admin().indices().preparePutMapping(COOKIE_ID_INDEX_NAME)
                .setType(ID_TYPE)
                .setSource(sources.toArray())
                .execute().actionGet().isAcknowledged();
            if (!acknowledged) {
                throw new RuntimeException(String.format("failed to update index mapping: {index=%s, field=%s}", COOKIE_ID_INDEX_NAME, CommonUtils.collectionToString(fieldNames)));
            }
        }
    }

    private void index(String id, Map<String, Object> fields) {
        String respId = esClient.prepareIndex(COOKIE_ID_INDEX_NAME, ID_TYPE, id)
            .setSource(fields)
            .execute()
            .actionGet()
            .getId();
        if (!id.equals(respId)) {
            throw new RuntimeException(String.format("index exception, {requestId=%s, responseId=%s}", id, respId));
        }
    }


    private void indexBulk(List<IndexEntry> entries) {
        if (entries.size() > 0) {
            final Set<String> cookieIds = new HashSet<>(entries.size());
            BulkRequestBuilder bulk = esClient.prepareBulk();
            entries.forEach(entry -> {
                cookieIds.add(entry.cookieId);
                Map<String, Object> map = MapBuilder.<String, Object>newMapBuilder().put(COOKIE_ID_FIELD_NAME, entry.cookieId).map();
                bulk.add(esClient.prepareUpdate(COOKIE_ID_INDEX_NAME, ID_TYPE, entry.docId).setDoc(map).setUpsert(map));
            });
            if (bulk.execute().actionGet().hasFailures()) {
                throw new RuntimeException("failed index bulk request");
            }

            scheduleDeleteCookieIdsTask(() -> {
                try {
                    stringRedisTemplate.opsForHash().delete(getRedisHashKey(BizTypeEnum.COOKIE_ID), cookieIds.toArray());
                } finally {
                    deleteCookieIdTaskTime.decrementAndGet();
                }
            });
            deleteCookieIdTaskTime.incrementAndGet();
        }
    }

    private void scheduleDeleteCookieIdsTask(Runnable task) {
        Objects.requireNonNull(task, "task");
        deleteCookieIdsFromRedisScheduler.schedule(task, DELETE_REDIS_KEY_TASK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    private double getIndexQueueLength() {
        return this.bulkIndexService.getQueueLength();
    }

    private void update(String id, Map<String, Object> fields) {
        String respId = esClient.prepareUpdate(COOKIE_ID_INDEX_NAME, ID_TYPE, id)
            .setDoc(fields)
            .execute()
            .actionGet()
            .getId();
        if (!id.equals(respId)) {
            throw new RuntimeException(String.format("update exception, {requestId=%s, responseId=%s}", id, respId));
        }
    }

    private String queryOne(String fieldName, String fieldValue) {
        SearchResponse searchResponse = esClient.prepareSearch(COOKIE_ID_INDEX_NAME)
            .setTypes(ID_TYPE)
            .setQuery(QueryBuilders.termQuery(fieldName, fieldValue))
            .execute()
            .actionGet();

        String id = null;
        long totalHits = searchResponse.getHits().totalHits;
        if (totalHits <= 0) {
            if (log.isDebugEnabled()) {
                log.debug("not found record for {fieldName={}, fieldValue={}}", fieldName, fieldValue);
            }
        } else {
            if (totalHits > 1) {
                log.error("found more than one record[totalHits={}] for {fieldName={}, fieldValue={}}", totalHits, fieldName, fieldValue);
            }
            id = searchResponse.getHits().getAt(0).getId();
        }
        return id;
    }


    private Map<String /* field value */, String /* doc id */> queryList(String fieldName, Set<String> fieldValues) {
        Objects.requireNonNull(fieldName, "fieldName");
        Objects.requireNonNull(fieldName, "fieldValues");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        fieldValues.stream().map(fieldValue -> QueryBuilders.termQuery(fieldName, fieldValue)).forEach(boolQueryBuilder::should);

        SearchResponse response = esClient.prepareSearch(COOKIE_ID_INDEX_NAME)
            .setTypes(ID_TYPE)
            .setQuery(boolQueryBuilder)
            .setFrom(0)
            .setSize(fieldValues.size())
            .execute()
            .actionGet();

        Map<String, String> data = new HashMap<>();
        Arrays.stream(response.getHits().getHits()).forEach(sh -> data.put(sh.getSourceAsMap().get(fieldName).toString(), sh.getId()));
        return data;
    }


    private void createIdIndexIfNotExist() {
        boolean exists = esClient.admin().indices().prepareExists(COOKIE_ID_INDEX_NAME).execute().actionGet().isExists();
        if (!exists) {
            Map<String, String> settings = new HashMap<>();
            settings.put("index.number_of_shards", String.valueOf(ID_INDEX_SHARD_COUNT));
            esClient.admin().indices().prepareCreate(COOKIE_ID_INDEX_NAME)
                .setSettings(settings)
                .addMapping(ID_TYPE, COOKIE_ID_FIELD_NAME, "type=keyword")
                .execute().actionGet();
        }
    }


    @PostConstruct
    private void start() {
        if (started.compareAndSet(false, true)) {
            createIdIndexIfNotExist();
            this.bulkIndexService.start();
        }
    }

    @Deprecated
    private void init() {
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        String redisHashKey = getRedisHashKey(BizTypeEnum.COOKIE_ID);
        Long size = opsForHash.size(redisHashKey);
        if (size != null && size > 0) {
            log.info("there are {} cookie id in redis, transfer into es start", size);
            if (size <= 10000) {
                Map<String, String> entries = opsForHash.entries(redisHashKey);
                List<IndexEntry> indexEntries = entries.entrySet().stream().map(entry -> new IndexEntry(entry.getValue(), entry.getKey())).collect(Collectors.toList());
                indexBulk(indexEntries);
            } else {
                final int batchSize = 5000;
                try (Cursor<Map.Entry<String, String>> cursor = opsForHash.scan(redisHashKey, ScanOptions.scanOptions().count(batchSize).match("*").build())) {
                    List<IndexEntry> indexEntries = new LinkedList<>();
                    while (cursor.hasNext()) {
                        if (indexEntries.size() >= batchSize) {
                            log.info("transfer {} cookie id to es in loop", indexEntries.size());
                            indexBulk(indexEntries);
                            indexEntries.clear();
                        }
                        Map.Entry<String, String> entry = cursor.next();
                        indexEntries.add(new IndexEntry(entry.getValue(), entry.getKey()));
                    }

                    if (indexEntries.size() > 0) {
                        log.info("transfer {} cookie id to es end", indexEntries.size());
                        indexBulk(indexEntries);
                        indexEntries.clear();
                    }
                } catch (Throwable ex) {
                    log.error(String.format("scan hash key %s exception", redisHashKey), ex);
                }

            }
            log.info("there are {} cookie id in redis, transfer into es end", size);
        }
    }

    @PreDestroy
    private void shutdown() {
        if (this.shutdowning.compareAndSet(false, true)) {
            this.bulkIndexService.shutdown();

            int size;
            while ((size = this.deleteCookieIdTaskTime.get()) > 0) {
                log.info("[shutdown] - deleteCookieIdTaskTime: {} > 0, sleep 1s", size);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }

            this.deleteCookieIdsFromRedisScheduler.shutdown();
            try {
                boolean terminated = this.deleteCookieIdsFromRedisScheduler.awaitTermination(5, TimeUnit.SECONDS);
                if (!terminated) {
                    this.deleteCookieIdsFromRedisScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("InterruptedException", e);
                this.deleteCookieIdsFromRedisScheduler.shutdownNow();
            }

            this.shutdownCompleted.set(true);
        }
    }

    private static class IndexEntry {
        String docId;
        String cookieId;

        IndexEntry(String docId, String cookieId) {
            this.docId = docId;
            this.cookieId = cookieId;
        }
    }

    private class BulkIndexService extends AbstractServiceThread {

        private static final int INDEX_SERVICE_INTERVAL_MILLIS = 100;
        private static final int INDEX_SERVICE_MAX_BATCH_SIZE = 500;
        private final BlockingQueue<IndexEntry> indexEntries = new LinkedBlockingQueue<>(500000);

        void offerIndex(IndexEntry indexEntry) {
            Objects.requireNonNull(indexEntry, "indexEntry");
            if (!indexEntries.offer(indexEntry)) {
                //rollback
                stringRedisTemplate.opsForHash().delete(getRedisHashKey(BizTypeEnum.COOKIE_ID), indexEntry.cookieId);
                throw new IdGeneratorServiceException(ReturnCode.SYSTEM_BUSY, getServiceName());
            }
        }

        long getQueueLength() {
            return indexEntries.size();
        }

        @Override
        public String getServiceName() {
            return "BulkIndexService";
        }

        @Override
        public void run() {
            log.info(getServiceName() + " start ... ");

            final List<IndexEntry> entries = new LinkedList<>();
            while (!isStopped()) {
                doIndex(entries);
                this.waitForRunning(INDEX_SERVICE_INTERVAL_MILLIS);
            }
            while (!indexEntries.isEmpty()) {
                log.info("{} process before end, current queue size: {}", getServiceName(), indexEntries.size());
                doIndex(entries);
            }
            log.info(getServiceName() + " end ... ");
        }


        private void doIndex(final List<IndexEntry> entries) {
            indexEntries.drainTo(entries, INDEX_SERVICE_MAX_BATCH_SIZE);
            boolean success = false;
            int retryTimes = 0;
            while (retryTimes < 3) {
                retryTimes++;
                try {
                    indexBulk(entries);
                    success = true;
                    break;
                } catch (Throwable ex) {
                    //retry
                    log.error("bulk index to es exception", ex);
                }
            }
            if (!success) {
                log.error("{}  failed to index bulk", getServiceName());//notify
            }
            entries.clear();
        }
    }
}
