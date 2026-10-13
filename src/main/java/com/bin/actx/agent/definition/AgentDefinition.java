package com.bin.actx.agent.definition;

import com.bin.actx.plugin.BasePlugin;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;

/**
 * 定义 Agent 的各个行为和能力
 *
 * @author Bin
 * @since 2026/5/9 11:22
 */
@Data
public class AgentDefinition {

    /**
     * 插件配置列表
     */
    private List<PluginConfig> pluginConfigs;

    @Getter
    public static class PluginConfig {

        private final String className;

        private final Map<String, Object> metadata;

        public PluginConfig(String className, Map<String, Object> metadata) {
            this.className = className;
            this.metadata = metadata;
        }

        public PluginConfig(Class<? extends BasePlugin> cls, Map<String, Object> metadata) {
            this.className = cls.getName();
            this.metadata = metadata;
        }

    }

}
