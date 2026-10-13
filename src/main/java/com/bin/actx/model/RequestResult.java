package com.bin.actx.model;

import lombok.Data;

/**
 * 统一API响应包装类
 *
 * @author Bin
 * @since 2026/8/24
 */
@Data
public class RequestResult<T> {

    /**
     * 状态码，0表示成功
     */
    private int code;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 消息
     */
    private String message;

    public static <T> RequestResult<T> success(T data) {
        RequestResult<T> result = new RequestResult<>();
        result.setCode(0);
        result.setData(data);
        return result;
    }

    public static <T> RequestResult<T> success() {
        RequestResult<T> result = new RequestResult<>();
        result.setCode(0);
        return result;
    }

    public static <T> RequestResult<T> error(int code, String message) {
        RequestResult<T> result = new RequestResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

}
