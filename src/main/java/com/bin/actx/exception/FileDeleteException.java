package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

public class FileDeleteException extends BaseCustomException {

    public FileDeleteException() {
        super();
    }

    public FileDeleteException(String message) {
        super(message);
    }

    public FileDeleteException(Exception e) {
        super(e);
    }

    public FileDeleteException(String message, Exception e) {
        super(message, e);
    }

}
