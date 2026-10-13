package com.bin.actx.channel;

import com.bin.actx.task.context.TaskExecuteContext;
import com.bin.actx.task.model.TaskOrder;
import com.bin.actx.task.model.TaskPlan;
import com.bin.actx.task.result.SubTaskExecuteResult;
import com.bin.actx.task.result.TaskCompleteResult;
import com.bin.actx.task.result.TaskTerminationResult;

/**
 * 任务消息通道
 *
 * @author Bin
 * @since 2026/5/27 11:17
 */
public interface TaskMessageChannel extends MessageChannel {

    /**
     * 任务单创建
     *
     * @param taskOrder 任务单
     */
    default void onTaskCreated(TaskOrder taskOrder) {
    }

    /**
     * 任务单开始运行
     *
     * @param taskExecuteContext 任务执行上下文
     */
    default void onTaskRunning(TaskExecuteContext taskExecuteContext) {
    }

    /**
     * 任务单完成
     *
     * @param taskExecuteContext 任务执行上下文
     * @param taskResult         任务结果
     */
    default void onTaskComplete(TaskExecuteContext taskExecuteContext, TaskCompleteResult taskResult) {
    }

    /**
     * 任务单中断
     *
     * @param taskExecuteContext 任务执行上下文
     * @param taskResult         任务结果
     */
    default void onTaskTerminated(TaskExecuteContext taskExecuteContext, TaskTerminationResult taskResult) {
    }

    /**
     * 子任务创建
     *
     * @param taskExecuteContext 任务执行上下文
     * @param subTask            子任务
     */
    default void onSubTaskCreated(TaskExecuteContext taskExecuteContext, TaskPlan.SubTask subTask) {
    }

    /**
     * 子任务开始运行
     *
     * @param taskExecuteContext 任务执行上下文
     * @param subTask            子任务
     */
    default void onSubTaskRunning(TaskExecuteContext taskExecuteContext, TaskPlan.SubTask subTask) {
    }

    /**
     * 子任务完成
     *
     * @param taskExecuteContext   任务执行上下文
     * @param subTaskExecuteResult 子任务结果
     */
    default void onSubTaskComplete(TaskExecuteContext taskExecuteContext, SubTaskExecuteResult subTaskExecuteResult) {
    }

    /**
     * 子任务中断
     *
     * @param taskExecuteContext   任务执行上下文
     * @param subTaskExecuteResult 子任务结果
     */
    default void onSubTaskTerminated(TaskExecuteContext taskExecuteContext, SubTaskExecuteResult subTaskExecuteResult) {
    }

}
