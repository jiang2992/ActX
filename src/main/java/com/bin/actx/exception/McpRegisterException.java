package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * MCP客户端注册异常
 *
 * @author Bin
 * @since 2026/8/24
 */
public class McpRegisterException extends BaseCustomException {

    public McpRegisterException() {
        super();
    }

    public McpRegisterException(String message) {
        super(message);
    }

    public McpRegisterException(Exception e) {
        super(e);
    }

    public McpRegisterException(String message, Exception e) {
        super(message, e);
    }

}
