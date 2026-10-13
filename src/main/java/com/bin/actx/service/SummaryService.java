package com.bin.actx.service;

import com.bin.actx.agent.AgentInstance;
import com.bin.actx.agent.AgentManager;
import com.bin.actx.config.GlobalStatusManager;
import com.bin.actx.config.constant.SceneConstant;
import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.message.SessionMessage;
import com.bin.actx.message.handler.RequestMessageHandler;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.service.request.UserChatRequest;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 总结服务
 *
 * @author Bin
 * @since 2026/5/12 15:12
 */
@Component
public class SummaryService {

    @Autowired
    private AgentManager agentManager;
    @Autowired
    private ChatModelManager chatModelManager;
    @Autowired
    private GlobalStatusManager globalStatusManager;
    @Autowired
    private RequestMessageHandler requestMessageHandler;

    public String summary(List<SessionMessage> messageList) {
        String agentId = globalStatusManager.getSceneAgentId(SceneConstant.SUMMARY);
        AgentInstance agentInstance = agentManager.getInstance(agentId);
        ChatClient chatClient = chatModelManager.getClient(agentInstance.getConfig().getModelId());

        // 创建相关上下文
        RequestContext requestContext = new RequestContext(agentInstance).withScene(SceneConstant.SUMMARY);
        ResponseContext responseContext = new ResponseContext(requestContext, null);

        // 构造最终请求消息
        UserChatRequest userChatRequest = new UserChatRequest("Summarize all the contents of the above conversations");
        RequestMessagePayload requestMessagePayload = requestMessageHandler.handle(requestContext, userChatRequest);
        requestMessagePayload.setContextMessages(messageList);
        ChatClientRequestSpec requestSpec = requestMessageHandler.createRequestSpec(responseContext, requestMessagePayload, chatClient);

        return requestSpec.call().content();
    }

}
