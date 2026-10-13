package com.bin.actx.config.loader;

import com.bin.actx.config.loader.payload.McpConfigPayload;
import com.bin.actx.config.loader.payload.McpConfigPayload.McpServer;
import com.bin.actx.mcp.McpServerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * MCP配置加载器，用于从 mcp.json 文件中加载MCP服务器配置
 *
 * @author Bin
 * @since 2026/6/11 16:29
 */
@Component
public class McpConfigLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从指定的 mcp.json 文件中加载MCP服务器配置
     *
     * @param path 配置文件路径
     * @return MCP服务器配置列表
     * @throws IOException 文件读取失败时抛出
     */
    public List<McpServerConfig> load(String path) throws IOException {
        File configFile = new File(path);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        McpConfigPayload payload = objectMapper.readValue(configFile, McpConfigPayload.class);

        Map<String, McpServer> mcpServers = payload.getMcpServers();
        if (mcpServers == null || mcpServers.isEmpty()) {
            return new ArrayList<>();
        }

        List<McpServerConfig> configs = new ArrayList<>();
        for (Map.Entry<String, McpServer> entry : mcpServers.entrySet()) {
            String serverName = entry.getKey();
            McpServer server = entry.getValue();

            McpServerConfig config = new McpServerConfig();
            config.setName(serverName);
            config.setType(server.getType());
            config.setCommand(server.getCommand());
            config.setArgs(server.getArgs());
            config.setEnv(server.getEnv());
            config.setCwd(server.getCwd());
            config.setUrl(server.getUrl());
            config.setHeaders(server.getHeaders());
            config.setTimeout(server.getTimeout());
            config.setDisabled(server.getDisabled());

            configs.add(config);
        }

        return configs;
    }

}
