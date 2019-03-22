package com.github.rodbate.uid.web.service;

import com.github.rodbate.uid.web.dto.request.LinkSecondaryIdToCookieIdRequest;
import com.github.rodbate.uid.web.dto.request.ListIdBySecondaryIdsRequest;
import com.github.rodbate.uid.web.dto.response.GetIdByCookieIdResponse;
import com.github.rodbate.uid.web.dto.response.GetIdResponse;
import com.github.rodbate.uid.web.dto.response.ListIdBySecondaryIdsResponse;
import reactor.core.publisher.Mono;

/**
 * User: jiangsongsong
 * Date: 2018/12/26
 * Time: 10:49
 */
public interface IdGeneratorService {

    /**
     * get next global unique id
     *
     * @param bizType bizType id
     * @return {@link GetIdResponse}
     */
    Mono<GetIdResponse> getId(String bizType);


    /**
     * get next global unique id for cookie id
     *
     * @param cookieId cookie id
     * @return {@link GetIdByCookieIdResponse}
     */
    Mono<GetIdByCookieIdResponse> getIdByCookieId(String cookieId);


    /**
     * link secondary id to cookie id
     *
     * @param request request
     * @return void
     */
    Mono<Void> linkSecondaryIdToCookieId(Mono<LinkSecondaryIdToCookieIdRequest> request);


    /**
     * query unique id for secondary ids
     *
     * @param request request
     * @return {@link ListIdBySecondaryIdsResponse}
     */
    Mono<ListIdBySecondaryIdsResponse> listIdBySecondaryIds(Mono<ListIdBySecondaryIdsRequest> request);
}
