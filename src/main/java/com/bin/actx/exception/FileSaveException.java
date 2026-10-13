package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

public class FileSaveException extends BaseCustomException {

    public FileSaveException() {
        super();
    }

    public FileSaveException(String message) {
        super(message);
    }

    public FileSaveException(Exception e) {
        super(e);
    }

    public FileSaveException(String message, Exception e) {
        super(message, e);
    }

}
