package com.bin.actx.service;

import com.bin.actx.agent.AgentInstance;
import com.bin.actx.agent.AgentManager;
import com.bin.actx.channel.DirectChatMessageChannel;
import com.bin.actx.channel.SSEMessageChannel;
import com.bin.actx.channel.event.StreamEvent;
import com.bin.actx.config.GlobalStatusManager;
import com.bin.actx.config.constant.SceneConstant;
import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.context.ResponseContextManager;
import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.message.handler.AnswerMessageHandler;
import com.bin.actx.message.handler.RequestMessageHandler;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.service.request.UserChatRequest;
import com.bin.actx.service.result.ChatDirectResult;
import com.bin.actx.service.result.StreamChatStopResult;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 聊天服务，提供同步和流式两种对话模式
 *
 * @author Bin
 * @since 2026/5/12 15:11
 */
@Slf4j
@Service
public class ChatService {

    @Autowired
    private RequestMessageHandler requestMessageHandler;
    @Autowired
    private AnswerMessageHandler answerMessageHandler;
    @Autowired
    private ChatModelManager chatModelManager;
    @Autowired
    private GlobalStatusManager globalStatusManager;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private ResponseContextManager responseContextManager;

    /**
     * 同步聊天请求
     *
     * @param userChatRequest 请求参数
     * @return 应答结果
     */
    public ChatDirectResult chat(UserChatRequest userChatRequest, String scene) {
        if (userChatRequest.getSessionId() == null || userChatRequest.getSessionId().isEmpty()) {
            userChatRequest.setSessionId(IdUtils.uuid());
        }

        // 获取相关配置
        String agentId = globalStatusManager.getSceneAgentId(scene);
        AgentInstance agentInstance = agentManager.getInstance(agentId);
        ChatClient chatClient = chatModelManager.getClient(agentInstance.getConfig().getModelId());

        // 创建相关上下文
        DirectChatMessageChannel messageChannel = new DirectChatMessageChannel();
        RequestContext requestContext = new RequestContext(agentInstance)
                .withScene(scene)
                .withUserId(userChatRequest.getUserId())
                .withSessionId(userChatRequest.getSessionId());
        ResponseContext responseContext = new ResponseContext(requestContext, messageChannel);

        // 构造最终请求消息
        RequestMessagePayload requestMessagePayload = requestMessageHandler.handle(requestContext, userChatRequest);
        ChatClientRequestSpec requestSpec = requestMessageHandler.createRequestSpec(responseContext, requestMessagePayload, chatClient);

        // 处理结果
        ChatClientResponse chatClientResponse = requestSpec.call().chatClientResponse();
        try {
            answerMessageHandler.handle(responseContext, chatClientResponse);
        } catch (Throwable e) {
            answerMessageHandler.handle(responseContext, e);
            throw e;
        }
        return new ChatDirectResult(requestContext.getSessionId(), messageChannel.getAnswer());
    }

    /**
     * 异步聊天请求
     *
     * @param userChatRequest 请求参数
     * @return Flux<StreamEvent>
     */
    public Flux<StreamEvent> streamChat(UserChatRequest userChatRequest, String scene) {
        if (userChatRequest.getSessionId() == null || userChatRequest.getSessionId().isEmpty()) {
            userChatRequest.setSessionId(IdUtils.uuid());
        }

        // 获取相关配置
        String agentId = globalStatusManager.getSceneAgentId(scene);
        AgentInstance agentInstance = agentManager.getInstance(agentId);
        ChatClient chatClient = chatModelManager.getClient(agentInstance.getConfig().getModelId());

        // 创建相关上下文
        SSEMessageChannel messageChannel = new SSEMessageChannel();
        RequestContext requestContext = new RequestContext(agentInstance)
                .withScene(scene)
                .withUserId(userChatRequest.getUserId())
                .withSessionId(userChatRequest.getSessionId());
        ResponseContext responseContext = new ResponseContext(requestContext, messageChannel);

        // 构造最终请求消息
        RequestMessagePayload requestMessagePayload = requestMessageHandler.handle(requestContext, userChatRequest);
        ChatClientRequestSpec requestSpec = requestMessageHandler.createRequestSpec(responseContext, requestMessagePayload, chatClient);

        // 处理结果
        Flux<ChatClientResponse> responseFlux = requestSpec.stream().chatClientResponse();
        answerMessageHandler.streamHandle(responseContext, responseFlux);
        return messageChannel.asFlux();
    }

    public String extractTitle(UserChatRequest userChatRequest) {
        String agentId = globalStatusManager.getSceneAgentId(SceneConstant.TITLE_EXTRACT);
        AgentInstance agentInstance = agentManager.getInstance(agentId);
        ChatClient chatClient = chatModelManager.getClient(agentInstance.getConfig().getModelId());

        // 创建相关上下文
        RequestContext requestContext = new RequestContext(agentInstance).withScene(SceneConstant.TITLE_EXTRACT);
        ResponseContext responseContext = new ResponseContext(requestContext, null);

        // 构造最终请求消息
        RequestMessagePayload requestMessagePayload = requestMessageHandler.handle(requestContext, userChatRequest);
        requestMessagePayload.setMessage(new RequestSessionMessage(userChatRequest.getMessage()));
        ChatClientRequestSpec requestSpec = requestMessageHandler.createRequestSpec(responseContext, requestMessagePayload, chatClient);

        return requestSpec.call().content();
    }

    public StreamChatStopResult stopStreamChat(String sessionId) {
        StreamChatStopResult result = new StreamChatStopResult();
        result.setSessionId(sessionId);
        ResponseContext responseContext = responseContextManager.getBySessionId(sessionId);
        if (responseContext == null) {
            result.setResult(false);
            result.setMessage("无法停止，当前请求已结束");
            return result;
        }
        responseContext.stop("用户主动停止请求");
        result.setResult(true);
        return result;
    }

}
