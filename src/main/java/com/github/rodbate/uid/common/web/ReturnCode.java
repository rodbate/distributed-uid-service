package com.github.rodbate.uid.common.web;

import lombok.Getter;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 11:15
 */
public enum ReturnCode {

    OK(0, "success"),

    BAD_REQUEST(400, "bad request"),

    INTERNAL_SERVER_ERROR(500, "internal server error"),

    APPLICATION_SHUTDOWN(510, "application shutdown now"),

    SYSTEM_BUSY(550, "system busy, service {0}"),

    NOT_LEADER(1000, "current node is not leader"),

    NOT_FOUND_FOR_COOKIE_ID(10001, "not found record for cookie id: {0}");

    @Getter
    private final int code;
    @Getter
    private final String defaultMsg;

    ReturnCode(int code, String defaultMsg) {
        this.code = code;
        this.defaultMsg = defaultMsg;
    }


}
