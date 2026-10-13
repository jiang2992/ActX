package com.bin.actx.service;

import com.bin.actx.agent.AgentInstance;
import com.bin.actx.agent.AgentManager;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.service.result.ChatTestResult;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 测试服务
 *
 * @author Bin
 * @since 2026/5/12 16:03
 */
@Slf4j
@Service
public class TestService {

    @Autowired
    private AgentManager agentManager;
    @Autowired
    private ChatModelManager chatModelManager;

    /**
     * 测试 Agent 连通性
     *
     * @param agentId AgentId
     * @return 是否连通
     */
    public ChatTestResult test(String agentId) {
        AgentInstance agentInstance = agentManager.getInstance(agentId);
        if (agentInstance == null) {
            return ChatTestResult.fail("Agent not found");
        }

        try {
            ChatClient chatClient = chatModelManager.getClient(agentInstance.getConfig().getModelId());
            String content = chatClient.prompt("Hello！Please reply Hi without thinking.").call().content();
            if (StringUtils.isEmpty(content)) {
                return ChatTestResult.fail("Response content is empty");
            }
            return ChatTestResult.success();
        } catch (Exception e) {
            return ChatTestResult.fail(e);
        }
    }

}
