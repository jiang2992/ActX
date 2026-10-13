package com.bin.actx.task.constant;

public interface TaskConstant {

    /**
     * 状态：新创建的任务
     */
    String STATUS_CREATED = "created";

    /**
     * 状态：任务正在执行
     */
    String STATUS_RUNNING = "running";

    /**
     * 状态：任务等待
     */
    String STATUS_WAIT = "wait";

    /**
     * 状态：任务执行完成
     */
    String STATUS_COMPLETED = "completed";

    /**
     * 状态：任务被终止，通常是发生无法继续的错误
     */
    String STATUS_TERMINATION = "termination";

}
