package com.bin.actx.plugin.core.basal;

import com.bin.actx.channel.ChatMessageChannel;
import com.bin.actx.channel.MessageChannel;
import com.bin.actx.channel.event.StreamEvent.ToolResultPayload;
import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.context.ResponseStopInfo;
import com.bin.actx.message.AnswerSessionMessage;
import com.bin.actx.message.AnswerSessionMessageBuilder;
import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.plugin.BasePlugin;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.plugin.hook.ResponseHook;
import com.bin.actx.service.request.RequestMessagePayload;
import java.util.Map;
import org.jiang.tools.text.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 每个Agent的基础插件
 *
 * @author Bin
 * @since 2026/4/22 14:53
 */
public class BasalPlugin extends BasePlugin implements RequestHook, ResponseHook {

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        String systemPrompt = getSystemPrompt(requestContext);
        if (StringUtils.notEmpty(systemPrompt)) {
            requestMessagePayload.addSystemPrompt(getSystemPrompt(requestContext));
        }
    }

    private String getSystemPrompt(RequestContext requestContext) {
        Map<String, Object> pluginMetadata = requestContext.getPluginMetadata(this);
        if (pluginMetadata == null || pluginMetadata.isEmpty()) {
            return null;
        }
        return (String) requestContext.getPluginMetadata(this).get("systemPrompt");
    }

    /**
     * 响应过程处理，将原始响应数据转换成会话消息
     *
     * @param responseContext             响应上下文
     * @param requestSessionMessage       请求会话消息
     * @param answerSessionMessageBuilder 响应会话消息构建器
     * @return 响应事件通道
     */
    @Override
    public MessageChannel responseProc(ResponseContext responseContext, RequestSessionMessage requestSessionMessage,
            AnswerSessionMessageBuilder answerSessionMessageBuilder) {
        return new ChatMessageChannel() {

            @Override
            public void onThinking(String content) {
                answerSessionMessageBuilder.appendThinkingSection(content);
            }

            @Override
            public void onThinkingStream(String content) {
                answerSessionMessageBuilder.appendThinkingSection(content);
            }

            @Override
            public void onAnswer(String content) {
                answerSessionMessageBuilder.appendMessageSection(content);
            }

            @Override
            public void onAnswerStream(String content) {
                answerSessionMessageBuilder.appendMessageSection(content);
            }

            @Override
            public void onToolCallingResult(ToolResultPayload toolResultPayload) {
                answerSessionMessageBuilder.appendToolSection(toolResultPayload.getId(),
                        toolResultPayload.getName(),
                        toolResultPayload.getArguments(),
                        toolResultPayload.getResult());
            }

            @Override
            public void onTell(String content) {
                answerSessionMessageBuilder.appendTellSection(content);
            }

            @Override
            public void onError(Throwable throwable) {
                String errorMessage;
                if (throwable instanceof WebClientResponseException e) {
                    String responseBody = e.getResponseBodyAsString();
                    errorMessage = "Model response error: " + responseBody;
                } else {
                    errorMessage = "The request processing has failed. Please try again later.";
                }
                answerSessionMessageBuilder.appendErrorSection(errorMessage);
            }

            @Override
            public void onStop(ResponseStopInfo stopInfo) {
                BasalPlugin.super.debugLog(("Received stop signal: " + stopInfo.getContent()));
                answerSessionMessageBuilder.appendStopSection(stopInfo.getContent());
            }

            @Override
            public void onEnd(AnswerSessionMessage answerSessionMessage) {
                BasalPlugin.super.debugLog(("Current token total: " + answerSessionMessage.getTokenTotal()));
                ChatMessageChannel.super.onEnd(answerSessionMessage);
            }
        };
    }

}
