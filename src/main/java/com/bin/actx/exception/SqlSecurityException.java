package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * SQL安全异常，当SQL违反安全规则或权限不足时抛出
 *
 * @author Bin
 * @since 2026/8/25
 */
public class SqlSecurityException extends BaseCustomException {

    public SqlSecurityException() {
        super();
    }

    public SqlSecurityException(String message) {
        super(message);
    }

    public SqlSecurityException(Exception e) {
        super(e);
    }

    public SqlSecurityException(String message, Exception e) {
        super(message, e);
    }

}
