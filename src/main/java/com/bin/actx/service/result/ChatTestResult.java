package com.bin.actx.service.result;

import lombok.Data;

@Data
public class ChatTestResult {

    private Boolean success;

    private String message;

    private Exception exception;

    public static ChatTestResult success() {
        ChatTestResult result = new ChatTestResult();
        result.setSuccess(true);
        return result;
    }

    public static ChatTestResult fail(String message) {
        ChatTestResult result = new ChatTestResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public static ChatTestResult fail(Exception exception) {
        ChatTestResult result = new ChatTestResult();
        result.setSuccess(false);
        result.setMessage(exception.getMessage());
        result.setException(exception);
        return result;
    }

    public static ChatTestResult fail(String message, Exception exception) {
        ChatTestResult result = fail(message);
        result.setException(exception);
        return result;
    }

}
