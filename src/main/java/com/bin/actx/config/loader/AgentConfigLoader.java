package com.bin.actx.config.loader;

import com.bin.actx.agent.AgentConfig;
import com.bin.actx.agent.definition.CustomAgentDefinitionBuilder;
import com.bin.actx.config.loader.payload.AgentConfigPayload;
import com.bin.actx.config.loader.payload.AgentConfigPayload.PluginConfigPayload;
import com.bin.actx.plugin.core.basal.BasalPlugin;
import com.bin.actx.plugin.core.tool.DynamicToolPlugin;
import com.bin.actx.plugin.core.tool.StaticToolPlugin;
import com.bin.actx.util.MetadataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jiang.tools.text.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Agent配置加载器，用于从 JSON 文件中加载Agent配置
 *
 * @author Bin
 * @since 2026/6/2 15:48
 */
@Component
public class AgentConfigLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从指定的 JSON 文件中加载Agent配置
     *
     * @param path 配置文件路径
     * @return Agent配置
     * @throws IOException 文件读取失败时抛出
     */
    public AgentConfig load(String path) throws IOException {
        File configFile = new File(path);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        // 读取JSON文件
        AgentConfigPayload agentConfigPayload = objectMapper.readValue(configFile, AgentConfigPayload.class);

        // 转换并注册Agent配置
        return convertToAgentConfig(agentConfigPayload);
    }

    /**
     * 从指定的 JSON 文件中加载Agent配置
     *
     * @param paths 配置文件路径
     * @return Agent配置列表
     * @throws IOException 文件读取失败时抛出
     */
    public List<AgentConfig> load(Collection<String> paths) throws IOException {
        List<AgentConfig> agentConfigs = new ArrayList<>(paths.size());
        for (String path : paths) {
            agentConfigs.add(load(path));
        }
        return agentConfigs;
    }

    /**
     * 将 AgentConfigPayload 转换为 AgentConfig
     * <p>通过 CustomAgentDefinitionBuilder 装配插件，默认装配基础插件与工具插件，其余插件按照 plugins 配置装配</p>
     *
     * @param file 配置文件对象
     * @return AgentConfig 对象
     * @throws IOException 系统提示词文件读取失败时抛出
     */
    private AgentConfig convertToAgentConfig(AgentConfigPayload file) throws IOException {
        AgentConfig agentConfig = new AgentConfig();

        // 设置基本信息
        agentConfig.setId(file.getId());
        agentConfig.setName(file.getName());
        agentConfig.setDescription(file.getDescription());
        agentConfig.setModelId(file.getModel());
        agentConfig.setPrivatize(Boolean.TRUE.equals(file.getPrivatize()));

        // 读取系统提示词（systemPromptFile 优先级更高）
        String systemPrompt = file.getSystemPrompt();
        if (StringUtils.isEmpty(systemPrompt) && StringUtils.notEmpty(file.getSystemPromptFile())) {
            systemPrompt = Files.readString(Paths.get(file.getSystemPromptFile()));
        }

        // 构建 AgentDefinition：通过自定义构建器装配插件
        CustomAgentDefinitionBuilder builder = new CustomAgentDefinitionBuilder();

        // 默认装配基础插件
        Map<String, Object> basalMetadata = new HashMap<>();
        if (StringUtils.notEmpty(systemPrompt)) {
            basalMetadata.put("systemPrompt", systemPrompt);
        }
        builder.addPlugin(BasalPlugin.class, basalMetadata);

        // 默认装配工具插件
        if ("dynamic".equals(file.getToolMode())) {
            builder.addPlugin(DynamicToolPlugin.class, MetadataBuilder.of("toolkits", (Object) file.getToolkits()).build());
        } else {
            builder.addPlugin(StaticToolPlugin.class, MetadataBuilder.of("toolkits", (Object) file.getToolkits()).build());
        }

        // 按照 plugins 配置装配其余插件
        if (file.getPlugins() != null) {
            for (PluginConfigPayload plugin : file.getPlugins()) {
                builder.addPlugin(plugin.getClassName(),
                        plugin.getMetadata() == null ? new HashMap<>() : plugin.getMetadata());
            }
        }

        agentConfig.setDefinition(builder.build());
        return agentConfig;
    }

}
