package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * 模型创建异常
 *
 * @author Bin
 * @since 2026/8/24
 */
public class CreateModelException extends BaseCustomException {

    public CreateModelException() {
        super();
    }

    public CreateModelException(String message) {
        super(message);
    }

    public CreateModelException(Exception e) {
        super(e);
    }

    public CreateModelException(String message, Exception e) {
        super(message, e);
    }

}
