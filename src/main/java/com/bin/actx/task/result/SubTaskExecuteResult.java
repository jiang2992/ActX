package com.bin.actx.task.result;

import lombok.Data;

@Data
public class SubTaskExecuteResult {

    private String runtimeId;

    private String code;

    private String output;

    private Exception exception;

    public boolean isSuccess() {
        return exception == null;
    }

    public static SubTaskExecuteResult success(String runtimeId, String code, String output) {
        SubTaskExecuteResult result = new SubTaskExecuteResult();
        result.setRuntimeId(runtimeId);
        result.setCode(code);
        result.setOutput(output);
        return result;
    }

    public static SubTaskExecuteResult failure(String runtimeId, String code, Exception exception) {
        SubTaskExecuteResult result = new SubTaskExecuteResult();
        result.setRuntimeId(runtimeId);
        result.setCode(code);
        result.setException(exception);
        result.setOutput("execute fail: " + exception.getClass().getName() + " - " + exception.getMessage());
        return result;
    }

}
