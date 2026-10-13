package com.bin.actx.plugin.hook;

import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.service.request.RequestMessagePayload;

public interface RequestHook {

    /**
     * 在发送请求前调用，通常用于装配请求消息，例如添加工具调用等
     *
     * @param requestMessagePayload 请求消息载体
     * @param requestContext        请求上下文
     */
    default void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
    }

    /**
     * 在所有requestFit调用之后，即将正式发送请求之前，这个时候已经有响应上下文了，可以在此阶段发送消息
     *
     * @param requestMessagePayload 请求消息载体
     * @param responseContext       响应上下文
     */
    default void requestBefore(RequestMessagePayload requestMessagePayload, ResponseContext responseContext) {
    }

}
