package com.bin.actx.message.handler;

import com.bin.actx.advisor.ToolAdvisor;
import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.context.ResponseContextManager;
import com.bin.actx.file.AttachmentInfo;
import com.bin.actx.file.AttachmentManager;
import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.message.attachment.AttachmentPreview;
import com.bin.actx.plugin.PluginManager;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.service.request.UserChatRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户消息处理器默认实现
 *
 * @author Bin
 * @since 2026/5/12 11:41
 */
@Slf4j
@Component
public class RequestMessageHandler {

    @Autowired
    private AttachmentManager attachmentManager;
    @Autowired
    private PluginManager pluginManager;
    @Autowired
    private ResponseContextManager responseContextManager;

    public RequestMessagePayload handle(RequestContext requestContext, UserChatRequest userChatRequest) {
        RequestMessagePayload requestMessagePayload = new RequestMessagePayload();

        if (userChatRequest != null) {
            requestMessagePayload.setSessionId(userChatRequest.getSessionId());
            if (userChatRequest.getMessage() != null) {
                RequestSessionMessage.Builder builder = new RequestSessionMessage.Builder();
                builder.content(userChatRequest.getMessage());
                if (userChatRequest.getAttachments() != null && !userChatRequest.getAttachments().isEmpty()) {
                    List<AttachmentInfo> attachmentInfoList = userChatRequest.getAttachments().stream()
                            .map(attachmentManager::getAttachment).filter(Objects::nonNull).toList();
                    builder.attachments(attachmentInfoList);
                }
                requestMessagePayload.setMessage(builder.build());
            }
        }

        // 调用插件钩子
        List<RequestHook> requestHooks = pluginManager.getRequestHooks(requestContext.getAgentInstance());
        for (RequestHook hook : requestHooks) {
            hook.requestFit(requestMessagePayload, requestContext);
        }

        return requestMessagePayload;
    }

    public ChatClientRequestSpec createRequestSpec(ResponseContext responseContext, RequestMessagePayload requestMessagePayload,
            ChatClient chatClient) {
        // 托管响应上下文
        responseContextManager.add(responseContext);

        // 调用插件钩子
        List<RequestHook> requestHooks = pluginManager.getRequestHooks(responseContext.getRequestContext().getAgentInstance());
        for (RequestHook hook : requestHooks) {
            hook.requestBefore(requestMessagePayload, responseContext);
        }

        // 将请求消息添加到上下文消息中
        RequestSessionMessage requestSessionMessage = requestMessagePayload.getMessage();
        List<Message> findMessageList = new ArrayList<>();
        requestMessagePayload.getContextMessages().forEach(message -> message.appendToMessages(findMessageList));
        if (requestSessionMessage != null) {
            if (requestSessionMessage.getAttachments() != null && !requestSessionMessage.getAttachments().isEmpty()) {
                List<AttachmentInfo> attachmentInfoList = requestSessionMessage.getAttachments();
                List<AttachmentPreview> attachmentPreviewList = attachmentInfoList.stream().map(attachmentManager::toAttachmentPreview)
                        .filter(Objects::nonNull).toList();
                requestSessionMessage.appendToMessages(findMessageList, attachmentPreviewList);
            } else {
                requestSessionMessage.appendToMessages(findMessageList);
            }
        }

        // 构造请求
        ChatClientRequestSpec requestSpec = chatClient.prompt()
                .toolContext(responseContext.buildToolContext())
                .messages(findMessageList);

        // 添加工具相关
        if (requestMessagePayload.getTools() != null && !requestMessagePayload.getTools().isEmpty()) {
            requestSpec.advisors(new ToolAdvisor(responseContext))
                    .toolCallbacks(requestMessagePayload.getTools());
        }

        // 添加系统提示词
        if (requestMessagePayload.getSystemPrompts() != null) {
            requestSpec.system(String.join("\n", requestMessagePayload.getSystemPrompts()));
        }

        // 初始化响应上下文
        responseContext.init(requestSessionMessage);

        return requestSpec;
    }

}
