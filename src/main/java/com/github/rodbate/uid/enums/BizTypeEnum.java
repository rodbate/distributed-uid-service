package com.github.rodbate.uid.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * User: jiangsongsong
 * Date: 2018/12/10
 * Time: 14:55
 */
public enum BizTypeEnum {

    COOKIE_ID("cookie-id");

    @Getter
    private final String bizType;

    BizTypeEnum(String bizType) {
        this.bizType = bizType;
    }

    /**
     * @param bizType biz type
     * @return BizTypeEnum
     */
    public static BizTypeEnum fromBizType(String bizType) {
        return Arrays.stream(values()).filter(en -> en.bizType.equalsIgnoreCase(bizType)).findAny().orElseThrow(() -> new IllegalArgumentException("invalid bizType: " + bizType));
    }

}
