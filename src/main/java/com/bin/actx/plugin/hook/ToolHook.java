package com.bin.actx.plugin.hook;

import com.bin.actx.context.ResponseContext;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.prompt.Prompt;

public interface ToolHook {

    /**
     * 在工具调用前调用
     *
     * @param lastResponse    LLM请求调用工具的响应
     * @param responseContext 响应处理器
     */
    default void toolCallBefore(ChatClientResponse lastResponse, ResponseContext responseContext) {
    }

    /**
     * 在工具调用后调用，允许修改工具调用后的响应给LLM的请求消息
     *
     * @param prompt          响应给LLM的请求消息
     * @param responseContext 响应处理器
     * @return 修改后的请求消息，如果返回null，则不会做任何修改
     */
    default Prompt toolCallAfter(Prompt prompt, ResponseContext responseContext) {
        return null;
    }

}
