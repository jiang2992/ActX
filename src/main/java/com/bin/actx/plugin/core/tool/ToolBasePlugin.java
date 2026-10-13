package com.bin.actx.plugin.core.tool;

import com.bin.actx.channel.event.StreamEvent.ToolCallPayload;
import com.bin.actx.channel.event.StreamEvent.ToolResultPayload;
import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.plugin.BasePlugin;
import com.bin.actx.plugin.hook.ToolHook;
import com.bin.actx.tool.ToolManager;
import com.bin.actx.tool.model.ToolKitInfo;
import com.bin.actx.util.TextUtils;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * 工具插件抽象类，实现了消息通道工具事件调用和简单的日志记录
 *
 * @author Bin
 * @since 2026/5/29 15:25
 */
public abstract class ToolBasePlugin extends BasePlugin implements ToolHook {

    private static final int MAX_TOOL_STRING_LENGTH = 512;

    @Resource
    protected ToolManager toolManager;

    @Override
    public void toolCallBefore(ChatClientResponse lastResponse, ResponseContext responseContext) {
        // 工具调用记录
        for (ToolCall toolCall : lastResponse.chatResponse().getResult().getOutput().getToolCalls()) {
            String arguments = TextUtils.limit(toolCall.arguments(), MAX_TOOL_STRING_LENGTH);
            ToolCallPayload toolCallPayload = new ToolCallPayload(toolCall.id(), toolCall.name(), arguments);
            responseContext.getMessageChannel().onToolCalling(toolCallPayload);
            super.debugLog(
                    "[Tool calling: %s] %s (id: %s)".formatted(toolCall.name(), arguments, toolCallPayload.getId()));
        }
    }

    /**
     * 工具调用之后，触发对应消息事件
     *
     * @param prompt          响应给LLM的请求消息
     * @param responseContext 响应处理器
     * @return Prompt
     */
    @Override
    public Prompt toolCallAfter(Prompt prompt, ResponseContext responseContext) {
        // 获取对应的调用请求
        Map<String, ToolCall> toolCalls;
        Message callMessage = prompt.getInstructions().get(prompt.getInstructions().size() - 2);
        if (callMessage instanceof AssistantMessage assistantMessage) {
            if (!assistantMessage.hasToolCalls()) {
                toolCalls = Map.of();
            } else {
                toolCalls = assistantMessage.getToolCalls().stream().collect(Collectors.toMap(ToolCall::id, tc -> tc));
            }
        } else {
            toolCalls = Map.of();
        }

        // 触发消息通道事件
        Message message = prompt.getInstructions().get(prompt.getInstructions().size() - 1);
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            for (ToolResponse toolResult : toolResponseMessage.getResponses()) {
                String arguments = toolCalls.containsKey(toolResult.id()) ? TextUtils.limit(toolCalls.get(toolResult.id()).arguments(),
                        MAX_TOOL_STRING_LENGTH) : "";
                String result = TextUtils.limit(toolResult.responseData(), MAX_TOOL_STRING_LENGTH);
                ToolResultPayload toolResultPayload = new ToolResultPayload(
                        toolResult.id(),
                        toolResult.name(),
                        arguments,
                        result);
                responseContext.getMessageChannel().onToolCallingResult(toolResultPayload);
                super.debugLog("[Tool result: %s] %s (id: %s) ".formatted(toolResultPayload.getName(), result,
                        toolResultPayload.getId()));
            }
        }

        return prompt;
    }

    /**
     * 从插件的 Metadata 中提取工具包
     *
     * @param requestContext 请求上下文
     * @return 工具包列表
     */
    protected Set<Class<?>> extractToolkitsForMetadata(RequestContext requestContext) {
        String toolkits = (String) requestContext.getPluginMetadata(this).get("toolkits");
        Set<Class<?>> toolKitList = new HashSet<>();
        if (StringUtils.notEmpty(toolkits)) {
            if ("*".equals(toolkits)) {
                toolKitList.addAll(toolManager.getToolkitList(true)
                        .stream()
                        .map(ToolKitInfo::getCls)
                        .collect(Collectors.toSet()));
            } else {
                Collection<String> toolKitNames = Arrays.stream(toolkits.split(",")).toList();
                toolKitList.addAll(
                        toolManager.getToolkitListByNames(toolKitNames)
                                .stream()
                                .map(ToolKitInfo::getCls)
                                .collect(Collectors.toSet()));
            }
        }
        return toolKitList;
    }

}
