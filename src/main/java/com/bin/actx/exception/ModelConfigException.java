package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * 模型配置异常
 *
 * @author Bin
 * @since 2026/8/24
 */
public class ModelConfigException extends BaseCustomException {

    public ModelConfigException() {
        super();
    }

    public ModelConfigException(String message) {
        super(message);
    }

    public ModelConfigException(Exception e) {
        super(e);
    }

    public ModelConfigException(String message, Exception e) {
        super(message, e);
    }

}
