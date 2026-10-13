package com.bin.actx.task.result;

import lombok.Data;

@Data
public class TaskTerminationResult extends TaskResult {

    private String message;

    public TaskTerminationResult(String id, String message) {
        this.setId(id);
        this.message = message;
    }

}
