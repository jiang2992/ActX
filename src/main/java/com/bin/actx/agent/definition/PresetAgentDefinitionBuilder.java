package com.bin.actx.agent.definition;

import com.bin.actx.agent.definition.AgentDefinition.PluginConfig;
import com.bin.actx.plugin.core.basal.BasalPlugin;
import com.bin.actx.plugin.core.file.FilePlugin;
import com.bin.actx.plugin.core.mcp.McpPlugin;
import com.bin.actx.plugin.core.memory.SessionMemoryPlugin;
import com.bin.actx.plugin.core.sandbox.SandboxPlugin;
import com.bin.actx.plugin.core.summary.SummaryPlugin;
import com.bin.actx.plugin.core.task.TaskPlugin;
import com.bin.actx.plugin.core.tool.DynamicToolPlugin;
import com.bin.actx.util.MetadataBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 简单的 AgentDefinition 构造器 </br> 提供预设的基础行为定义
 *
 * @author Bin
 * @since 2026/5/9 11:23
 */
@Data
public class PresetAgentDefinitionBuilder implements AgentDefinitionBuilder {

    /**
     * 记忆模式：会话记忆，会保存会话中的每一轮对话
     */
    public static final String MEMORY_MODE_SESSION = "session";

    /**
     * 记忆模式：临时，不会保存上一轮对话
     */
    public static final String MEMORY_MODE_TEMP = "temp";

    /**
     * 记忆模式
     */
    private String memoryMode;

    /**
     * 静态工具包列表，逗号分割，支持通配符'*'
     */
    private String staticToolkits;

    /**
     * 动态工具包列表，逗号分割，支持通配符'*'
     */
    private String dynamicToolkits;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 是否启用自动总结
     */
    private Boolean enableSummary;

    /**
     * 是否允许任务调度
     */
    private Boolean taskDispatch;

    /**
     * 是否允许使用沙箱
     */
    private Boolean enableSandbox;

    @Override
    public AgentDefinition build() {

        AgentDefinition agentDefinition = new AgentDefinition();
        List<PluginConfig> pluginConfigs = new ArrayList<>();

        // 基础装配
        MetadataBuilder<String, Object> basalMetadataBuilder = MetadataBuilder.of("systemPrompt", this.getSystemPrompt());
        pluginConfigs.add(new PluginConfig(BasalPlugin.class, basalMetadataBuilder.build()));

        // 会话模式装配
        if (MEMORY_MODE_SESSION.equals(this.getMemoryMode())) {
            pluginConfigs.add(new PluginConfig(SessionMemoryPlugin.class, null));
        }

        // 工具装配
        Map<String, Object> toolMetadata = new HashMap<>();
        toolMetadata.put("staticToolkits", this.getStaticToolkits());
        toolMetadata.put("dynamicToolkits", this.getDynamicToolkits());
        pluginConfigs.add(new PluginConfig(DynamicToolPlugin.class, toolMetadata));

        // 任务调度
        if (Boolean.TRUE.equals(this.getTaskDispatch())) {
            pluginConfigs.add(new PluginConfig(TaskPlugin.class, null));
        }

        // 沙箱
        if (Boolean.TRUE.equals(this.getEnableSandbox())) {
            pluginConfigs.add(new PluginConfig(SandboxPlugin.class, null));
        }

        // 总结
        if (Boolean.TRUE.equals(this.getEnableSummary())) {
            pluginConfigs.add(new PluginConfig(SummaryPlugin.class, null));
        }

        // 文件
        pluginConfigs.add(new PluginConfig(FilePlugin.class, null));

        pluginConfigs.add(new PluginConfig(McpPlugin.class, null));

        agentDefinition.setPluginConfigs(pluginConfigs);
        return agentDefinition;
    }

}
