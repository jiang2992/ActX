package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

public class SandboxInitializeException extends BaseCustomException {

    public SandboxInitializeException() {
        super();
    }

    public SandboxInitializeException(String message) {
        super(message);
    }

    public SandboxInitializeException(Exception e) {
        super(e);
    }

    public SandboxInitializeException(String message, Exception e) {
        super(message, e);
    }

}
