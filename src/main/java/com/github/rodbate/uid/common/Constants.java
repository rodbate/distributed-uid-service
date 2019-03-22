package com.github.rodbate.uid.common;

/**
 * 常量定义
 * <p>
 * User: jiangsongsong
 * Date: 2018/12/10
 * Time: 11:21
 */
public final class Constants {

    public static final int CPU_CORE_NUM = Runtime.getRuntime().availableProcessors();
    public static final String RETURN_MESSAGE_BASE_NAME = "ReturnMessage";

    public static final String ID_GENERATOR_ZK_ROOT_PATH = "/id-generator";
    public static final String ID_GENERATOR_DATA_PATH = ID_GENERATOR_ZK_ROOT_PATH + "/data";
    public static final String ID_GENERATOR_CLUSTER_PATH = ID_GENERATOR_ZK_ROOT_PATH + "/cluster";


    private Constants() {
        throw new IllegalStateException("NO INSTANCE");
    }


}
