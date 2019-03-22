package com.github.rodbate.uid.web.service.impl;

import com.github.rodbate.uid.enums.BizTypeEnum;
import com.github.rodbate.uid.web.config.DistIdStepConfig;
import com.github.rodbate.uid.web.service.IdGeneratorService;
import com.github.rodbate.uid.allocation.IdAllocationManager;
import com.github.rodbate.uid.web.dto.request.LinkSecondaryIdToCookieIdRequest;
import com.github.rodbate.uid.web.dto.request.ListIdBySecondaryIdsRequest;
import com.github.rodbate.uid.web.dto.response.GetIdByCookieIdResponse;
import com.github.rodbate.uid.web.dto.response.GetIdResponse;
import com.github.rodbate.uid.web.dto.response.ListIdBySecondaryIdsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

/**
 * User: jiangsongsong
 * Date: 2018/12/26
 * Time: 10:52
 */
@Slf4j
public class RedisIdGeneratorService implements IdGeneratorService {

    @Autowired
    private IdAllocationManager idAllocationManager;

    @Autowired
    private DistIdStepConfig distIdStepConfig;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;


    @Override
    public Mono<GetIdResponse> getId(String bizType) {
        return idAllocationManager.nextIdAsync(bizType, distIdStepConfig.getStepWithCheckBizType(bizType)).map(id -> new GetIdResponse(id.toString()));
    }

    @Override
    public Mono<GetIdByCookieIdResponse> getIdByCookieId(String cookieId) {
        return redisTemplate.opsForValue()
            .get(cookieId)
            .switchIfEmpty(setIfAbsent(cookieId))
            .map(GetIdByCookieIdResponse::new);
    }

    @Override
    public Mono<Void> linkSecondaryIdToCookieId(Mono<LinkSecondaryIdToCookieIdRequest> request) {
        return Mono.error(new UnsupportedOperationException("unsupported api"));
    }

    @Override
    public Mono<ListIdBySecondaryIdsResponse> listIdBySecondaryIds(Mono<ListIdBySecondaryIdsRequest> request) {
        return Mono.error(new UnsupportedOperationException("unsupported api"));
    }


    private Mono<String> setIfAbsent(final String cookieId) {
        final String bizType = BizTypeEnum.COOKIE_ID.getBizType();
        return idAllocationManager.nextIdAsync(bizType, distIdStepConfig.getStep(BizTypeEnum.COOKIE_ID))
            .flatMap(uid -> redisTemplate.opsForValue().setIfAbsent(cookieId, String.valueOf(uid))
                .flatMap(rs -> {
                    if (rs) {
                        //set uid -> cookie id
                        return redisTemplate.opsForValue().set(String.valueOf(uid), cookieId)
                            .flatMap(rs1 -> {
                                if (rs1) {
                                    return Mono.just(String.valueOf(uid));
                                } else {
                                    return Mono.error(new RuntimeException(String.format("failed to set [key=%d, value=%s] to redis", uid, cookieId)));
                                }
                            });
                    } else {
                        //get
                        return redisTemplate.opsForValue().get(cookieId);
                    }
                }));
    }
}
