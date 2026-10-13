package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * 文件移动异常
 *
 * @author Bin
 * @since 2026/8/24
 */
public class FileMoveException extends BaseCustomException {

    public FileMoveException() {
        super();
    }

    public FileMoveException(String message) {
        super(message);
    }

    public FileMoveException(Exception e) {
        super(e);
    }

    public FileMoveException(String message, Exception e) {
        super(message, e);
    }

}
