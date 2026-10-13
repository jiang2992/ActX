package com.bin.actx.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.Data;

/**
 * class description
 *
 * @author Bin
 * @since 2026/6/12 14:13
 */
@Data
public class McpClientInstance {

    private McpServerConfig config;

    private Boolean disabled;

    private McpSyncClient client;

}
