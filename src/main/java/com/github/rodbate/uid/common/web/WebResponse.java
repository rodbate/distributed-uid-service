package com.github.rodbate.uid.common.web;

import lombok.Getter;
import lombok.Setter;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 11:14
 */
@Getter
@Setter
public class WebResponse<T> {
    private int code;
    private String msg;
    private String exception;
    private T data;


    /**
     * build success response
     *
     * @param data data
     * @param <T>  data type
     * @return web response
     */
    public static <T> WebResponse<T> buildSuccessResponse(T data) {
        WebResponse<T> response = new WebResponse<>();
        response.setCode(ReturnCode.OK.getCode());
        response.setData(data);
        return response;
    }


    /**
     * build success response
     *
     * @param <T> data type
     * @return web response
     */
    public static <T> WebResponse<T> buildSuccessResponse() {
        WebResponse<T> response = new WebResponse<>();
        response.setCode(ReturnCode.OK.getCode());
        return response;
    }

    /**
     * build error response
     *
     * @param returnCode response code
     * @param msg        msg
     * @param exception  exception
     * @return web response
     */
    public static WebResponse buildErrorResponse(ReturnCode returnCode, String msg, String exception) {
        WebResponse response = new WebResponse<>();
        response.setCode(returnCode.getCode());
        response.setMsg(msg);
        response.setException(exception);
        return response;
    }

    /**
     * build error response
     *
     * @param returnCode response code
     * @param msg        msg
     * @param ex         exception
     * @return web response
     */
    public static WebResponse buildErrorResponse(ReturnCode returnCode, String msg, Throwable ex) {
        return buildErrorResponse(returnCode, msg, ex.getMessage());
    }

    /**
     * build error response
     *
     * @param returnCode return code
     * @param msg        msg
     * @return web response
     */
    public static WebResponse buildErrorResponse(ReturnCode returnCode, String msg) {
        return buildErrorResponse(returnCode, msg, (String) null);
    }

}
