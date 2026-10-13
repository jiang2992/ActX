package com.bin.actx.tool.kit.system;

import java.util.Collection;
import java.util.Map;
import lombok.Data;

/**
 * Agent 配置信息
 *
 * @author Bin
 * @since 2026/5/12 16:14
 */
@Data
public class GlobalAgentInfo {

    /**
     * 已创建的Agent配置列表
     */
    private Collection<AgentConfigInfo> agentConfigList;

    /**
     * 已创建的Agent实例列表
     */
    private Collection<AgentInstanceInfo> agentInstanceList;

    /**
     * 场景与Agent映射关系
     */
    private Map<String, String> sceneAgentMapper;

    @Data
    public static class AgentConfigInfo {

        private String id;

        private String modelId;

        private String name;

        private String description;

    }

    @Data
    public static class AgentInstanceInfo {

        private String id;

        private String configId;

        private Boolean systemPrivatized;

    }

}
