package com.bin.actx.agent.definition;

import com.bin.actx.agent.definition.AgentDefinition.PluginConfig;
import com.bin.actx.plugin.BasePlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 自定义的 AgentDefinition 构造器 </br> 允许自由配置插件，基础插件与工具插件由调用方装配
 *
 * @author Bin
 * @since 2026/5/9 11:23
 */
@Data
public class CustomAgentDefinitionBuilder implements AgentDefinitionBuilder {

    private List<PluginConfig> pluginConfigs = new ArrayList<>();

    public CustomAgentDefinitionBuilder addPlugin(Class<? extends BasePlugin> cls, Map<String, Object> metadata) {
        pluginConfigs.add(new PluginConfig(cls, metadata));
        return this;
    }

    /**
     * 以类名形式添加插件（适用于从 JSON 配置加载）
     *
     * @param className 插件类名
     * @param metadata  插件元数据
     * @return 当前构建器
     */
    public CustomAgentDefinitionBuilder addPlugin(String className, Map<String, Object> metadata) {
        pluginConfigs.add(new PluginConfig(className, metadata));
        return this;
    }

    @Override
    public AgentDefinition build() {
        AgentDefinition agentDefinition = new AgentDefinition();
        agentDefinition.setPluginConfigs(pluginConfigs);
        return agentDefinition;
    }

}
