package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * 插件实例创建异常
 *
 * @author Bin
 * @since 2026/5/9 17:29
 */
public class PluginCreateInstanceException extends BaseCustomException {

    public PluginCreateInstanceException() {
        super();
    }

    public PluginCreateInstanceException(String message) {
        super(message);
    }

    public PluginCreateInstanceException(Exception e) {
        super(e);
    }

    public PluginCreateInstanceException(String message, Exception e) {
        super(message, e);
    }

}
