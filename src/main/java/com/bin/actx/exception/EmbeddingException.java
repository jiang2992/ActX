package com.bin.actx.exception;

import org.jiang.tools.exception.BaseCustomException;

/**
 * 嵌入异常
 *
 * @author Bin
 * @since 2026/9/4 15:37
 */
public class EmbeddingException extends BaseCustomException {

    public EmbeddingException() {
        super();
    }

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(Exception e) {
        super(e);
    }

    public EmbeddingException(String message, Exception e) {
        super(message, e);
    }

}
