package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

public class SectionCreateException extends BaseCustomException {

    public SectionCreateException() {
        super();
    }

    public SectionCreateException(String message) {
        super(message);
    }

    public SectionCreateException(Exception e) {
        super(e);
    }

    public SectionCreateException(String message, Exception e) {
        super(message, e);
    }

}
