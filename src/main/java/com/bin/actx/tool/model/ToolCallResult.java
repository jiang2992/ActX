package com.bin.actx.tool.model;

import lombok.Data;

/**
 * 工具调用结果通用包装类
 *
 * @author Bin
 * @since 2026/5/12 15:14
 */
@Data
public class ToolCallResult<T> {

    /**
     * 结果数据
     */
    private T data;

    /**
     * 附加消息，用于向LLM传达操作结果的上下文信息
     */
    private String message;

    public static <T> ToolCallResult<T> of(T data) {
        ToolCallResult<T> result = new ToolCallResult<>();
        result.setData(data);
        return result;
    }

    public static <T> ToolCallResult<T> of(T data, String message) {
        ToolCallResult<T> result = new ToolCallResult<>();
        result.setData(data);
        result.setMessage(message);
        return result;
    }

}
