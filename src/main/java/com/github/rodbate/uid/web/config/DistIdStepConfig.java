package com.github.rodbate.uid.web.config;

import com.github.rodbate.uid.enums.BizTypeEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 10:32
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dist-id.config")
public class DistIdStepConfig {
    private static final String DEFAULT_KEY = "default";
    private Map<String /* biz type */, Integer> step;


    /**
     * get step config by biz type
     *
     * @param bizTypeEnum biz type
     * @return step
     */
    public int getStep(final BizTypeEnum bizTypeEnum) {
        Objects.requireNonNull(bizTypeEnum, "bizTypeEnum require not null");
        return getStep0(bizTypeEnum.getBizType());
    }

    /**
     * check biz type and get step by the valid biz type
     *
     * @param bizType biz type
     * @return step
     */
    public int getStepWithCheckBizType(String bizType) {
        BizTypeEnum bizTypeEnum = BizTypeEnum.fromBizType(bizType);
        return getStep(bizTypeEnum);
    }


    /**
     * do not check the biz type, if invalid then return the default step config
     *
     * @param bizType biz type
     * @return step
     */
    public int getStepWithNotCheckBizType(String bizType) {
        BizTypeEnum bizTypeEnum = null;
        try {
            bizTypeEnum = BizTypeEnum.fromBizType(bizType);
        } catch (IllegalArgumentException ignored) {
        }
        if (bizTypeEnum == null) {
            bizType = DEFAULT_KEY;
        } else {
            bizType = bizTypeEnum.getBizType();
        }
        return getStep0(bizType);
    }

    private int getStep0(final String bizType) {
        return this.step.getOrDefault(bizType, this.step.getOrDefault(DEFAULT_KEY, 10000));
    }

}
