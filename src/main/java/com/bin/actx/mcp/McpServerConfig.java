package com.bin.actx.mcp;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class McpServerConfig {

    /**
     * 唯一标识
     */
    private String name;

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
