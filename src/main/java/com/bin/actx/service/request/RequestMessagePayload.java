package com.bin.actx.service.request;

import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.message.SessionMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.springframework.ai.tool.ToolCallback;

/**
 * 将要发送给LLM的请求消息，包含了系统提示词、上下文消息、当前消息、工具列表等信息
 *
 * @author Bin
 * @since 2026/4/17 10:32
 */
@Data
public class RequestMessagePayload {

    /**
     * 会话id
     */
    private String sessionId;

    /**
     * 系统提示词
     */
    private List<String> systemPrompts = new ArrayList<>();

    /**
     * 上下文消息
     */
    private List<SessionMessage> contextMessages = new ArrayList<>();

    /**
     * 当前消息
     */
    private RequestSessionMessage message;

    /**
     * 工具包
     */
    private Set<Class<?>> toolkits = new HashSet<>();

    /**
     * 工具列表（固定静态）
     */
    private List<ToolCallback> tools = new ArrayList<>();

    public void addSystemPrompt(String systemPrompt) {
        this.systemPrompts.add(systemPrompt);
    }

    public void addSystemPrompt(Collection<String> systemPrompt) {
        this.systemPrompts.addAll(systemPrompt);
    }

    public void addToolkit(Class<?> toolkit) {
        this.toolkits.add(toolkit);
    }

    public void addToolkit(Collection<Class<?>> toolkit) {
        this.toolkits.addAll(toolkit);
    }

    public void addTool(ToolCallback tool) {
        this.tools.add(tool);
    }

    public void addTool(Collection<ToolCallback> tool) {
        this.tools.addAll(tool);
    }

}
