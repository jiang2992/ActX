package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * 工具调用异常
 *
 * @author Bin
 * @since 2026/5/9 17:29
 */
public class ToolCallingException extends BaseCustomException {

    public ToolCallingException() {
        super();
    }

    public ToolCallingException(String message) {
        super(message);
    }

    public ToolCallingException(Exception e) {
        super(e);
    }

    public ToolCallingException(String message, Exception e) {
        super(message, e);
    }

}
