package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * 文件复制异常
 *
 * @author Bin
 * @since 2026/8/24
 */
public class FileCopyException extends BaseCustomException {

    public FileCopyException() {
        super();
    }

    public FileCopyException(String message) {
        super(message);
    }

    public FileCopyException(Exception e) {
        super(e);
    }

    public FileCopyException(String message, Exception e) {
        super(message, e);
    }

}
