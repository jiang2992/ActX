package com.bin.actx.plugin.core.memory;

import com.bin.actx.channel.ChatMessageChannel;
import com.bin.actx.channel.MessageChannel;
import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.message.AnswerSessionMessage;
import com.bin.actx.message.AnswerSessionMessageBuilder;
import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.message.SessionMessageCollection;
import com.bin.actx.plugin.BasePlugin;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.plugin.hook.ResponseHook;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.storage.local.repository.SessionMessageRepository;
import jakarta.annotation.Resource;

/**
 * 会话记忆插件
 *
 * @author Bin
 * @since 2026/4/17 11:05
 */
public class SessionMemoryPlugin extends BasePlugin implements RequestHook, ResponseHook {

    @Resource
    private SessionMessageRepository sessionMessageRepository;

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        if (requestMessagePayload.getSessionId() == null) {
            super.warnLog("SessionId is null, skip context message fit");
            return;
        }
        SessionMessageCollection sessionMessageCollection = sessionMessageRepository.getSessionMessages(
                requestMessagePayload.getSessionId());
        requestMessagePayload.setContextMessages(sessionMessageCollection.getMessages());
    }

    /**
     * 响应过程处理，在响应结束后持久化会话消息
     *
     * @param responseContext             响应上下文
     * @param requestSessionMessage       请求消息
     * @param answerSessionMessageBuilder 响应消息构造器
     * @return 响应消息通道
     */
    @Override
    public MessageChannel responseProc(ResponseContext responseContext, RequestSessionMessage requestSessionMessage,
            AnswerSessionMessageBuilder answerSessionMessageBuilder) {
        if (responseContext.getRequestContext().getSessionId() == null) {
            return null;
        }

        // 响应开始前，保存用户请求消息
        sessionMessageRepository.addMessage(responseContext.getRequestContext().getSessionId(), requestSessionMessage);

        return new ChatMessageChannel() {
            @Override
            public void onEnd(AnswerSessionMessage answerSessionMessage) {
                if (responseContext.getRequestContext().getSessionId() == null) {
                    SessionMemoryPlugin.super.warnLog("SessionId is null, skip sessionMessageCollection save");
                    return;
                }
                sessionMessageRepository.addMessage(responseContext.getRequestContext().getSessionId(), answerSessionMessage);
                SessionMemoryPlugin.super.debugLog(
                        "Save answer message, session id: " + responseContext.getRequestContext().getSessionId());
            }
        };
    }

}
