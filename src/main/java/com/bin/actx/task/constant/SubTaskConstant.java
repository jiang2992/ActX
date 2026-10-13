package com.bin.actx.task.constant;

public interface SubTaskConstant {

    /**
     * 状态：新创建的任务
     */
    String RESULT_CREATED = "created";

    /**
     * 状态：任务执行完成
     */
    String RESULT_COMPLETED = "completed";

    /**
     * 状态：任务被终止
     */
    String RESULT_TERMINATION = "termination";

    /**
     * 类型：主干
     */
    String RUNTIME_TYPE_MAIN = "main";

    /**
     * 类型：补充信息
     */
    String RUNTIME_TYPE_LACK = "lack";

    /**
     * 类型：历史执行
     */
    String RUNTIME_TYPE_HISTORY = "history";

    /**
     * 运行时：等待重试（其他子步骤完成后重试）
     */
    String RUNTIME_WAIT_FOR_RETRY = "wait_for_retry";

    /**
     * 运行时：完成
     */
    String RUNTIME_COMPLETED = "completed";

}
