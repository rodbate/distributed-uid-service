package com.github.rodbate.uid.cluster.transport.http;

/**
 * User: jiangsongsong
 * Date: 2018/12/25
 * Time: 10:03
 */
public final class HttpTransportConstant {

    private HttpTransportConstant() {
    }

    private static final String API_PREFIX = "/api";
    public static final String GET_ALLOCATED_ID_API = API_PREFIX + "/allocatedId";

    public static final String PARAM_BIZ_TYPE_NAME = "bizType";
    public static final String PARAM_STEP_NAME = "step";

    public static final String GET_ALLOCATED_ID_API_TEMPLATE = String.format("%s/{%s}/{%s}", GET_ALLOCATED_ID_API, PARAM_BIZ_TYPE_NAME, PARAM_STEP_NAME);
}
