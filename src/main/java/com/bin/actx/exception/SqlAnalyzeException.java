package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * SQL解析异常
 *
 * @author Bin
 * @since 2026/8/25
 */
public class SqlAnalyzeException extends BaseCustomException {

    public SqlAnalyzeException() {
        super();
    }

    public SqlAnalyzeException(String message) {
        super(message);
    }

    public SqlAnalyzeException(Exception e) {
        super(e);
    }

    public SqlAnalyzeException(String message, Exception e) {
        super(message, e);
    }

}
