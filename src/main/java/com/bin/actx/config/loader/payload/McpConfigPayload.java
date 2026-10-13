package com.bin.actx.config.loader.payload;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 配置文件根类，对应 mcp.json 结构
 *
 * @author Bin
 * @since 2026/6/11 16:29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpConfigPayload {

    /**
     * MCP 服务器映射，key 为服务器唯一标识
     */
    private Map<String, McpServer> mcpServers;

    /**
     * 单个 MCP 服务器配置（静态内部类）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpServer {

        /**
         * 传输类型：stdio / sse / streamableHttp
         */
        private String type;

        /**
         * STDIO 模式：启动命令（如 "npx", "uvx"）
         */
        private String command;

        /**
         * STDIO 模式：命令行参数列表
         */
        private List<String> args;

        /**
         * STDIO 模式：环境变量
         */
        private Map<String, String> env;

        /**
         * STDIO 模式：工作目录
         */
        private String cwd;

        /**
         * SSE / Streamable HTTP 模式：远程服务 URL
         */
        private String url;

        /**
         * SSE / Streamable HTTP 模式：自定义请求头
         */
        private Map<String, String> headers;

        /**
         * 超时时间（秒），默认 30
         */
        private Integer timeout = 30;

        /**
         * 是否禁用该服务器，默认 false
         */
        private Boolean disabled = false;

    }

}