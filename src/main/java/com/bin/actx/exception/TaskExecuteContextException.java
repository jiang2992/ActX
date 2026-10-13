package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * 任务执行异常
 *
 * @author Bin
 * @since 2026/5/9 17:29
 */
public class TaskExecuteContextException extends BaseCustomException {

    public TaskExecuteContextException() {
        super();
    }

    public TaskExecuteContextException(String message) {
        super(message);
    }

    public TaskExecuteContextException(Exception e) {
        super(e);
    }

    public TaskExecuteContextException(String message, Exception e) {
        super(message, e);
    }

}
