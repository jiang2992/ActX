package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

public class FilePermissionException extends BaseCustomException {

    public FilePermissionException() {
        super();
    }

    public FilePermissionException(String message) {
        super(message);
    }

    public FilePermissionException(Exception e) {
        super(e);
    }

    public FilePermissionException(String message, Exception e) {
        super(message, e);
    }

}
