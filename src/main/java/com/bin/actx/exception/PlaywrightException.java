package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * Playwright执行异常
 *
 * @author Bin
 * @since 2026/8/24
 */
public class PlaywrightException extends BaseCustomException {

    public PlaywrightException() {
        super();
    }

    public PlaywrightException(String message) {
        super(message);
    }

    public PlaywrightException(Exception e) {
        super(e);
    }

    public PlaywrightException(String message, Exception e) {
        super(message, e);
    }

}
