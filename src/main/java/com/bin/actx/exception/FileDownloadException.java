package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

public class FileDownloadException extends BaseCustomException {

    public FileDownloadException() {
        super();
    }

    public FileDownloadException(String message) {
        super(message);
    }

    public FileDownloadException(Exception e) {
        super(e);
    }

    public FileDownloadException(String message, Exception e) {
        super(message, e);
    }

}
