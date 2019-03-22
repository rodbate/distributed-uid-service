package com.github.rodbate.uid.common;

import lombok.Getter;

import java.util.Arrays;

/**
 * application environment
 * <p>
 * User: jiangsongsong
 * Date: 2018/12/10
 * Time: 11:33
 */
public enum ApplicationEnv {

    DEV("dev"),

    TEST("test"),

    PROD("prod");

    @Getter
    private final String env;

    ApplicationEnv(String env) {
        this.env = env;
    }

    /**
     * @param env environment v
     * @return ApplicationEnv
     */
    public static ApplicationEnv fromEnv(String env) {
        return Arrays.stream(values()).filter(en -> en.env.equalsIgnoreCase(env)).findAny().orElseThrow(() -> new IllegalArgumentException("no such env: " + env));
    }
}
