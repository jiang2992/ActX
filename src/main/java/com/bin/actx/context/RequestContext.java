package com.bin.actx.context;

import com.bin.actx.agent.AgentInstance;
import com.bin.actx.plugin.BasePlugin;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 请求上下文，生命周期从一次完整的请求到最终响应之间。
 *
 * @author Bin
 * @since 2026/5/19 16:32
 */
@Slf4j
public class RequestContext {

    /**
     * 用户ID，不一定存在
     */
    @Getter
    private String userId;

    /**
     * 会话ID，不一定存在
     */
    @Getter
    private String sessionId;

    /**
     * 场景
     */
    @Getter
    private String scene;

    @Getter
    private final AgentInstance agentInstance;

    @Getter
    private final Map<String, Object> metadata;

    public RequestContext(AgentInstance agentInstance, Map<String, Object> metadata) {
        this.agentInstance = agentInstance;
        this.metadata = metadata;
    }

    public RequestContext(AgentInstance agentInstance) {
        this(agentInstance, new HashMap<>());
    }

    public RequestContext withScene(String scene) {
        this.scene = scene;
        return this;
    }

    public RequestContext withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public RequestContext withSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public void removeMetadata(String key) {
        metadata.remove(key);
    }

    public Map<String, Object> getPluginMetadata(BasePlugin plugin) {
        return agentInstance.getPlugins().get(plugin);
    }

    public boolean hasSession() {
        return sessionId != null;
    }

}
