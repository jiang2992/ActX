package com.bin.actx.mcp;

import com.bin.actx.exception.McpRegisterException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class McpClientManager {

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, McpClientInstance> clients = new ConcurrentHashMap<>();

    /**
     * 注册一个新的 MCP 客户端
     */
    public synchronized boolean register(McpServerConfig config) {
        if (clients.containsKey(config.getName())) {
            throw new IllegalArgumentException("Client already exists: " + config.getName());
        }
        try {
            McpClientInstance instance = new McpClientInstance();
            instance.setConfig(config);
            instance.setDisabled(config.getDisabled() != null ? config.getDisabled() : false);
            if (!instance.getDisabled()) {
                McpSyncClient client = createClient(config);
                client.initialize();
                instance.setClient(client);
            }
            clients.put(config.getName(), instance);
            return !instance.getDisabled();
        } catch (Exception e) {
            throw new McpRegisterException("Failed to register client: " + config.getName(), e);
        }
    }

    /**
     * 注销并关闭客户端
     */
    public synchronized void unregister(String name) {
        McpClientInstance instance = clients.get(name);
        if (instance == null) {
            return;
        }
        if (instance.getClient() != null) {
            instance.getClient().close();
            instance.setClient(null);
        }
        instance.setDisabled(false);
    }

    public List<ToolCallback> getAllToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        for (McpClientInstance instance : clients.values()) {
            if (instance.getDisabled() || instance.getClient() == null) {
                continue;
            }
            McpSyncClient client = instance.getClient();
            for (Tool tool : client.listTools().tools()) {
                toolCallbacks.add(SyncMcpToolCallback.builder().mcpClient(client).tool(tool).build());
            }
        }
        return toolCallbacks;
    }

    public List<ToolCallback> getToolCallbacksByNames(Collection<String> names) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        for (McpClientInstance instance : clients.values()) {
            if (instance.getDisabled() || instance.getClient() == null) {
                continue;
            }
            if (!names.contains(instance.getConfig().getName())) {
                continue;
            }
            McpSyncClient client = instance.getClient();
            for (Tool tool : client.listTools().tools()) {
                toolCallbacks.add(SyncMcpToolCallback.builder().mcpClient(client).tool(tool).build());
            }
        }
        return toolCallbacks;
    }

    public List<McpClientInstance> getAllInstances() {
        return new ArrayList<>(clients.values());
    }

    private McpSyncClient createClient(McpServerConfig config) throws URISyntaxException {
        return McpClient.sync(getTransport(config))
                .clientInfo(new Implementation("dynamic-client", "1.0.0"))
                .initializationTimeout(Duration.ofSeconds(config.getTimeout()))
                .requestTimeout(Duration.ofSeconds(config.getTimeout()))
                .build();
    }

    private McpClientTransport getTransport(McpServerConfig config) throws URISyntaxException {
        switch (config.getType().toLowerCase()) {
            case "stdio":
                ServerParameters.Builder serverParametersBuilder = new ServerParameters.Builder(config.getCommand());
                serverParametersBuilder.args(config.getArgs());
                serverParametersBuilder.env(config.getEnv());
                return new StdioClientTransport(serverParametersBuilder.build(), new JacksonMcpJsonMapper(objectMapper));
            case "sse":
                HttpClientSseClientTransport.Builder sseClientTransport = HttpClientSseClientTransport.builder(config.getUrl());
                sseClientTransport.jsonMapper(new JacksonMcpJsonMapper(objectMapper));
                sseClientTransport.httpRequestCustomizer((builder, method, endpoint, body,
                        context) -> {
                    if (config.getHeaders() != null) {
                        for (Entry<String, String> entry : config.getHeaders().entrySet()) {
                            builder.setHeader(entry.getKey(), entry.getValue());
                        }
                    }
                });
                if (config.getTimeout() != null) {
                    sseClientTransport.connectTimeout(Duration.ofSeconds(config.getTimeout()));
                }
                return sseClientTransport.build();
            case "streamablehttp":
                URI uri = new URI(config.getUrl());
                StringBuilder baseUrl = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
                if (uri.getPort() != -1) {
                    baseUrl.append(":").append(uri.getPort());
                }
                HttpClientStreamableHttpTransport.Builder streamableHttpTransport = HttpClientStreamableHttpTransport.builder(
                        baseUrl.toString());
                streamableHttpTransport.jsonMapper(new JacksonMcpJsonMapper(objectMapper));
                streamableHttpTransport.endpoint(uri.getPath());
                streamableHttpTransport.httpRequestCustomizer((builder, method, endpoint, body,
                        context) -> {
                    if (config.getHeaders() != null) {
                        for (Entry<String, String> entry : config.getHeaders().entrySet()) {
                            builder.setHeader(entry.getKey(), entry.getValue());
                        }
                    }
                });
                if (config.getTimeout() != null) {
                    streamableHttpTransport.connectTimeout(Duration.ofSeconds(config.getTimeout()));
                }
                streamableHttpTransport.resumableStreams(true);
                return streamableHttpTransport.build();
            default:
                throw new IllegalArgumentException("Unsupported transport type: " + config.getType());
        }
    }

    @PreDestroy
    public void cleanup() {
        for (McpClientInstance instance : clients.values()) {
            if (instance.getClient() == null) {
                continue;
            }
            instance.getClient().close();
        }
        clients.clear();
    }

}
