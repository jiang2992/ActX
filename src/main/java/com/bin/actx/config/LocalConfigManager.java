package com.bin.actx.config;

import com.bin.actx.agent.AgentConfig;
import com.bin.actx.agent.AgentInstance;
import com.bin.actx.agent.AgentManager;
import com.bin.actx.config.loader.ActxConfigLoader;
import com.bin.actx.config.loader.AgentConfigLoader;
import com.bin.actx.config.loader.DataSourceLoader;
import com.bin.actx.config.loader.McpConfigLoader;
import com.bin.actx.config.loader.ModelConfigLoader;
import com.bin.actx.config.loader.payload.ActxConfigPayload;
import com.bin.actx.config.loader.payload.ActxConfigPayload.SceneConfig;
import com.bin.actx.config.properties.AppProperties;
import com.bin.actx.mcp.McpClientManager;
import com.bin.actx.mcp.McpServerConfig;
import com.bin.actx.model.ChatModelConfig;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.model.EmbeddingModelConfig;
import com.bin.actx.model.EmbeddingModelManager;
import com.bin.actx.sql.datasource.DataSourceConfig;
import com.bin.actx.sql.datasource.DataSourceManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 本地配置管理，负责加载本地配置，并注册到相应的组件中
 *
 * @author Bin
 * @since 2026/8/12 09:50
 */
@Slf4j
@Component
public class LocalConfigManager {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ModelConfigLoader modelConfigLoader;

    @Autowired
    private AgentConfigLoader agentConfigLoader;

    @Autowired
    private ActxConfigLoader actxConfigLoader;

    @Autowired
    private McpConfigLoader mcpConfigLoader;

    @Autowired
    private ChatModelManager chatModelManager;

    @Autowired
    private EmbeddingModelManager embeddingModelManager;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private McpClientManager mcpClientManager;

    @Autowired
    private GlobalStatusManager globalStatusManager;

    @Autowired
    private DataSourceLoader dataSourceLoader;

    @Autowired
    private DataSourceManager dataSourceManager;

    public void loadAndRegister() throws IOException {
        log.debug("Load local config from: {}", appProperties.getConfigPath());

        ActxConfigPayload actxConfigPayload = actxConfigLoader.load(appProperties.getConfigPath() + "actx.json");

        // 加载模型配置
        Path modelFilePath = Paths.get(appProperties.getConfigPath() + "model.json");
        if (!Files.exists(modelFilePath)) {
            log.debug("No model.json found, skip loading model config.");
        } else {
            ModelConfigLoader.Result result = modelConfigLoader.load(modelFilePath.toString());
            if (result.getChatModelConfigs() != null) {
                for (ChatModelConfig chatModelConfig : result.getChatModelConfigs()) {
                    chatModelManager.addConfig(chatModelConfig);
                    log.debug("Add model: {} ({} - {})", chatModelConfig.getId(), chatModelConfig.getSupplierName(),
                            chatModelConfig.getModelName());
                }
            }
            if (result.getEmbeddingModelConfigs() != null) {
                for (EmbeddingModelConfig embeddingModelConfig : result.getEmbeddingModelConfigs()) {
                    embeddingModelManager.addConfig(embeddingModelConfig);
                    log.debug("Add embedding model: {} ({} - {})", embeddingModelConfig.getId(), embeddingModelConfig.getSupplierName(),
                            embeddingModelConfig.getModelName());
                }
            }
        }

        // 加载Agent配置
        List<Path> agentDirectories = getSubDirectories(appProperties.getConfigPath() + "agent/");
        List<String> agentFilePaths = agentDirectories.stream().map(agentDirectory -> agentDirectory.toAbsolutePath() + "/agent.json")
                .toList();
        List<AgentConfig> agentConfigs = agentConfigLoader.load(agentFilePaths);
        if (agentConfigs.isEmpty()) {
            log.debug("No agent.json found, skip loading agent config.");
        } else {
            for (AgentConfig agentConfig : agentConfigs) {
                agentManager.addConfig(agentConfig);
                AgentInstance instance = agentManager.createInstance(agentConfig, agentConfig.getPrivatize());
                log.debug("Create agent instance: {}, AgentConfig={} ({})", instance.getId(), agentConfig.getId(), agentConfig.getName());
            }
        }

        // 加载ActX配置
        Map<String, SceneConfig> scenes = actxConfigPayload.getScenes();
        if (scenes != null) {
            // 场景映射
            for (Entry<String, SceneConfig> entry : scenes.entrySet()) {
                String scene = entry.getKey();
                SceneConfig sceneConfig = entry.getValue();

                List<AgentInstance> agentInstances = agentManager.getInstanceByConfigId(sceneConfig.getAgent());
                if (agentInstances.isEmpty()) {
                    log.warn("No agent instance found for scene: {}", scene);
                    continue;
                }
                if (agentInstances.size() > 1) {
                    log.warn("Multiple agent instances found for scene: {}, use the first one.", scene);
                }
                AgentInstance agentInstance = agentInstances.get(0);
                globalStatusManager.setSceneAgentId(scene, agentInstance.getId());
                log.debug("Set scene: {}, AgentInstance: {}, AgentConfig: {}", scene, agentInstance.getId(),
                        agentInstance.getConfig().getId());
            }
        }

        // 加载数据源配置
        Path dataSourceFilePath = Paths.get(appProperties.getConfigPath() + "datasource.json");
        if (!Files.exists(dataSourceFilePath)) {
            log.debug("No datasource.json found, skip loading data source config.");
        } else {
            List<DataSourceConfig> dataSourceConfigs = dataSourceLoader.load(dataSourceFilePath.toString());
            dataSourceManager.registerAll(dataSourceConfigs);
        }

        // 加载MCP配置
        List<McpServerConfig> mcpServerConfigs = mcpConfigLoader.load(appProperties.getConfigPath() + "mcp.json");
        log.debug("Load MCP server config: {}", mcpServerConfigs.stream().map(McpServerConfig::getName).collect(Collectors.joining(",")));
        for (McpServerConfig mcpServerConfig : mcpServerConfigs) {
            boolean result = mcpClientManager.register(mcpServerConfig);
            log.debug("Register MCP server{}: {}", result ? "" : " (is disabled, skip init):", mcpServerConfig.getName());
        }

    }

    private List<Path> getSubDirectories(String parentPath) throws IOException {
        Path parentDir = Paths.get(parentPath);
        if (!Files.isDirectory(parentDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(parentDir)) {
            return stream.filter(Files::isDirectory)
                    .collect(Collectors.toList());
        }
    }

}
