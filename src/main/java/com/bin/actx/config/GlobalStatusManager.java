package com.bin.actx.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 全局状态管理器
 *
 * @author Bin
 * @since 2026/5/12 11:30
 */
@Component
public class GlobalStatusManager {

    private final Map<String, String> sceneAgentMapper = new ConcurrentHashMap<>();

    public void setSceneAgentId(String scene, String agentId) {
        sceneAgentMapper.put(scene, agentId);
    }

    public String getSceneAgentId(String scene) {
        return sceneAgentMapper.get(scene);
    }

    public Map<String, String> getSceneAgentMapper() {
        return sceneAgentMapper;
    }

}
