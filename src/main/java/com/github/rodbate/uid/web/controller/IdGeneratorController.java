package com.github.rodbate.uid.web.controller;

import com.github.rodbate.uid.common.web.ApplicationContextAssertable;
import com.github.rodbate.uid.common.web.WebResponse;
import com.github.rodbate.uid.metric.GlobalMetrics;
import com.github.rodbate.uid.web.dto.request.LinkSecondaryIdToCookieIdRequest;
import com.github.rodbate.uid.web.dto.request.ListIdBySecondaryIdsRequest;
import com.github.rodbate.uid.web.dto.response.GetIdByCookieIdResponse;
import com.github.rodbate.uid.web.dto.response.GetIdResponse;
import com.github.rodbate.uid.web.dto.response.ListIdBySecondaryIdsResponse;
import com.github.rodbate.uid.web.service.IdGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

/**
 * User: jiangsongsong
 * Date: 2018/12/26
 * Time: 10:41
 */
@RestController
@RequestMapping(value = "/id_generator", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class IdGeneratorController extends ApplicationContextAssertable {

    @Autowired
    private IdGeneratorService idGeneratorService;

    /**
     * get global unique id
     *
     * @param bizType bizType
     * @return {@link GetIdResponse}
     */
    @GetMapping(value = "/get_id/v1")
    public Mono<WebResponse<GetIdResponse>> getId(@RequestParam("biz_type") String bizType) {
        return this.idGeneratorService.getId(bizType)
            .map(WebResponse::buildSuccessResponse)
            .name(GlobalMetrics.API_GET_ID_V1_METRIC_NAME)
            .metrics();
    }


    /**
     * get global unique id for cookie id
     *
     * @param cookieId cookieId
     * @return {@link GetIdByCookieIdResponse}
     */
    @GetMapping(value = "/get_id_by_cookie_id/v1")
    public Mono<WebResponse<GetIdByCookieIdResponse>> getIdByCookieId(@RequestParam("cookie_id") String cookieId) {
        return this.idGeneratorService.getIdByCookieId(cookieId)
            .map(WebResponse::buildSuccessResponse)
            .name(GlobalMetrics.API_GET_ID_BY_COOKIE_ID_V1_METRIC_NAME)
            .metrics();
    }


    /**
     * link secondary id and cookie id
     *
     * @param request {@link LinkSecondaryIdToCookieIdRequest}
     * @return void
     */
    @PostMapping(value = "/link_secondary_id_to_cookie_id/v1", consumes = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Mono<WebResponse<Object>> linkSecondaryIdToCookieId(@Valid @RequestBody Mono<LinkSecondaryIdToCookieIdRequest> request) {
        return this.idGeneratorService.linkSecondaryIdToCookieId(request)
            .then(Mono.just(WebResponse.buildSuccessResponse()))
            .name(GlobalMetrics.API_LINK_SECONDARY_ID_TO_COOKIE_ID_V1_METRIC_NAME)
            .metrics();
    }


    /**
     *
     *
     * @param request {@link ListIdBySecondaryIdsRequest}
     * @return {@link ListIdBySecondaryIdsResponse}
     */
    @PostMapping(value = "/list_id_by_secondary_ids/v1", consumes = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Mono<WebResponse<ListIdBySecondaryIdsResponse>> listIdBySecondaryIds(@Valid @RequestBody Mono<ListIdBySecondaryIdsRequest> request) {
        return this.idGeneratorService.listIdBySecondaryIds(request)
            .map(WebResponse::buildSuccessResponse)
            .name(GlobalMetrics.API_LIST_ID_BY_SECONDARY_IDS_V1_METRIC_NAME)
            .metrics();
    }
}
