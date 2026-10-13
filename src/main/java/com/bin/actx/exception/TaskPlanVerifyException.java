package com.bin.actx.exception;

import com.bin.actx.task.model.TaskPlan;
import lombok.Getter;
import org.jiang.tools.exception.BaseCustomException;

public class TaskPlanVerifyException extends BaseCustomException {

    @Getter
    private TaskPlan taskPlan;

    public TaskPlanVerifyException() {
        super();
    }

    public TaskPlanVerifyException(String message) {
        super(message);
    }

    public TaskPlanVerifyException(String message, TaskPlan taskPlan) {
        super(message);
        this.taskPlan = taskPlan;
    }

}
