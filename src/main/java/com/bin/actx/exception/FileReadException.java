package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * 文件读取异常
 *
 * @author Bin
 * @since 2026/8/24
 */
public class FileReadException extends BaseCustomException {

    public FileReadException() {
        super();
    }

    public FileReadException(String message) {
        super(message);
    }

    public FileReadException(Exception e) {
        super(e);
    }

    public FileReadException(String message, Exception e) {
        super(message, e);
    }

}
