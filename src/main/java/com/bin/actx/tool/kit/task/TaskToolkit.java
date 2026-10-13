package com.bin.actx.tool.kit.task;

import com.bin.actx.channel.TaskMessageChannel;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.context.ResponseContextManager;
import com.bin.actx.exception.TaskPlanVerifyException;
import com.bin.actx.exception.ToolCallingException;
import com.bin.actx.plugin.core.task.TaskPlugin;
import com.bin.actx.service.TaskService;
import com.bin.actx.task.TaskDispatcher;
import com.bin.actx.task.TaskManager;
import com.bin.actx.task.constant.SubTaskConstant;
import com.bin.actx.task.constant.TaskConstant;
import com.bin.actx.task.context.TaskExecuteContext;
import com.bin.actx.task.model.SubTaskBriefInfo;
import com.bin.actx.task.model.TaskEvaluateInfo;
import com.bin.actx.task.model.TaskOrder;
import com.bin.actx.task.model.TaskPlan;
import com.bin.actx.task.result.TaskResult;
import com.bin.actx.tool.ToolStrategy;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.model.ToolCallResult;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.json.JsonUtils;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 任务相关工具包
 *
 * @author Bin
 * @since 2026/5/12 15:13
 */
@Slf4j
@Toolkit(name = "任务", description = "提供复杂任务的评估、规划、执行等工具", autoDiscover = false)
public class TaskToolkit {

    /**
     * 任务计划最大重试次数
     */
    private final int MAX_TASK_CREATE_RETRY_COUNT = 2;

    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private ResponseContextManager responseContextManager;

    /**
     * 任务评估工具
     *
     * @param task 任务描述
     * @return 评估结果
     */
    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = TaskPlugin.TOOL_EVALUATE_USER_TASK_NAME, description = "评估任务的复杂性")
    private ToolCallResult<TaskEvaluateInfo> evaluateUserTask(
            @ToolParam(description = "提供用户任务的完整描述（在必要时结合上下文说明）") String task
    ) {
        if (StringUtils.isEmpty(task)) {
            throw new ToolCallingException("Task cannot be empty");
        }
        try {
            TaskEvaluateInfo taskEvaluateInfo = taskService.evaluate(task);
            String taskResultId = taskManager.saveEvaluateResult(taskEvaluateInfo);
            return ToolCallResult.of(taskEvaluateInfo, "评估结果ID:" + taskResultId);
        } catch (Exception e) {
            log.error("Task evaluate failed: {}", e.getMessage(), e);
            throw new ToolCallingException(e.getMessage());
        }
    }

    /**
     * 任务计划工具
     *
     * @param task             任务描述
     * @param evaluateResultId 评估结果ID
     * @return 评估结果
     */
    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = TaskPlugin.TOOL_PLANNING_AND_EXECUTE_TASK_NAME, description = "规划并执行任务，任务将交由调度器Agent处理，直到产出任务结果、异常退出或需补充信息时返回")
    private ToolCallResult<TaskResult> planningAndExecuteTask(
            @ToolParam(description = "提供用户任务的完整描述（在必要时结合上下文说明）") String task,
            @ToolParam(description = "任务评估结果ID") String evaluateResultId,
            ToolContext toolContext) {
        if (StringUtils.isEmpty(task)) {
            throw new ToolCallingException("Task cannot be empty");
        }
        TaskOrder taskOrder = null;
        try {
            ResponseContext responseContext = responseContextManager.getByToolContext(toolContext.getContext());
            TaskEvaluateInfo evaluateResult = taskManager.getEvaluateResult(evaluateResultId);

            // 任务创建失败则重试
            TaskPlanVerifyException lastFailExeception = null;
            for (int i = 0; i <= MAX_TASK_CREATE_RETRY_COUNT; i++) {
                TaskPlan planningResult = taskService.planning(responseContext.getRequestContext().getUserId(), task, evaluateResult,
                        lastFailExeception);
                try {
                    taskOrder = taskManager.createTask(evaluateResult, planningResult);
                    break;
                } catch (TaskPlanVerifyException e) {
                    log.warn("Task plan verification failed: {} : {}", e.getMessage(), JsonUtils.toJson(planningResult));
                    lastFailExeception = e;
                }
            }

            // 超过次数仍未成功，抛出异常
            if (taskOrder == null) {
                throw new TaskPlanVerifyException("The task failed to be created multiple times.");
            }

            TaskExecuteContext taskExecuteContext = createTaskExecuteContext(taskOrder, responseContext);

            // 推送事件
            taskExecuteContext.getTaskMessageChannel().onTaskCreated(taskOrder);

            // 执行
            TaskDispatcher taskDispatcher = new TaskDispatcher(taskExecuteContext);
            TaskResult executeResult;
            try {
                executeResult = taskDispatcher.run(null);
            } finally {
                taskManager.destroyTaskExecuteContext(taskOrder.getId());
                taskDispatcher.close();
            }

            return ToolCallResult.of(executeResult);
        } catch (Exception e) {
            log.error("Task execution failed: {}", e.getMessage(), e);
            throw new ToolCallingException("TaskId=" + (taskOrder != null ? taskOrder.getId() : null) + ", " + e.getMessage());
        }
    }

    /**
     * 继续任务
     *
     * @param taskId    任务id
     * @param inputData 输入数据
     * @return 评估结果
     */
    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = TaskPlugin.TOOL_CONTINUE_TASK_NAME, description = "继续执行当前任务，通常在补充任务信息后或异常中断后调用")
    private ToolCallResult<TaskResult> continueTask(
            @ToolParam(description = "任务ID") String taskId,
            @ToolParam(description = "输入的数据，补充任务信息的情况下必填，Key为任务Code，Value为总结后的信息补充或决策）", required = false) Map<String, String> inputData,
            ToolContext toolContext) {
        if (StringUtils.isEmpty(taskId)) {
            throw new ToolCallingException("Task id cannot be empty");
        }
        try {
            ResponseContext responseContext = responseContextManager.getByToolContext(toolContext.getContext());
            TaskOrder taskOrder = taskManager.getTask(taskId);
            if (taskOrder == null) {
                return ToolCallResult.of(null, "Task not found");
            }
            if (TaskConstant.STATUS_TERMINATION.equals(taskOrder.getStatus())) {
                return ToolCallResult.of(null, "Task is terminated");
            }

            TaskExecuteContext taskExecuteContext = createTaskExecuteContext(taskOrder, responseContext);

            // 推送事件
            taskExecuteContext.getTaskMessageChannel().onTaskCreated(taskOrder);
            // TODO 推送已完成子任务

            // 执行
            TaskDispatcher taskDispatcher = new TaskDispatcher(taskExecuteContext);
            TaskResult executeResult;
            try {
                executeResult = taskDispatcher.run(inputData);
            } finally {
                taskManager.destroyTaskExecuteContext(taskOrder.getId());
                taskDispatcher.close();
            }

            return ToolCallResult.of(executeResult);
        } catch (Exception e) {
            log.error("Task execution failed: {}", e.getMessage(), e);
            throw new ToolCallingException(e.getMessage());
        }
    }

    @Tool(name = "get_task_info", description = "获取任务信息")
    private ToolCallResult<TaskOrder> getTaskInfo(
            @ToolParam(description = "任务ID") String taskId
    ) {
        TaskOrder task = taskManager.getTask(taskId);
        if (task == null) {
            return ToolCallResult.of(null, "Task not found");
        }

        StringBuilder message = new StringBuilder();
        switch (task.getStatus()) {
            case TaskConstant.STATUS_CREATED:
                message.append("该任务单已创建，但未开始执行;");
                break;
            case TaskConstant.STATUS_RUNNING:
                message.append("该任务单已开始执行;");
                break;
            case TaskConstant.STATUS_COMPLETED:
                message.append("该任务单已完成;");
                break;
            case TaskConstant.STATUS_TERMINATION:
                message.append("该任务单已终止;");
                break;
        }

        // 查询子任务完成情况
        StringBuilder subTaskResultText = new StringBuilder("子任务完成情况：").append("\n");
        List<SubTaskBriefInfo> subTaskResultList = taskManager.getSubTaskBriefInfo(taskId);
        if (subTaskResultList == null && subTaskResultList.isEmpty()) {
            subTaskResultText.append("无任何子任务执行记录").append("\n");
        } else {
            for (SubTaskBriefInfo subTaskBriefInfo : subTaskResultList) {
                switch (subTaskBriefInfo.getStatus()) {
                    case SubTaskConstant.RESULT_CREATED:
                        subTaskResultText.append(subTaskBriefInfo.getCode()).append(":已创建,但无结果信息;\n");
                        break;
                    case SubTaskConstant.RESULT_COMPLETED:
                        subTaskResultText.append(subTaskBriefInfo.getCode()).append(":已完成;\n");
                        break;
                    case TaskConstant.STATUS_TERMINATION:
                        subTaskResultText.append(subTaskBriefInfo.getCode()).append(":已终止，备注:")
                                .append(subTaskBriefInfo.getAdditional()).append(";\n");
                        break;
                }
            }
        }

        message.append("\n").append(subTaskResultText);
        return ToolCallResult.of(task, message.toString());
    }

    private TaskExecuteContext createTaskExecuteContext(TaskOrder taskOrder, ResponseContext responseContext) {
        TaskMessageChannel taskMessageChannel = null;
        if (responseContext.getMessageChannel() instanceof TaskMessageChannel) {
            taskMessageChannel = (TaskMessageChannel) responseContext.getMessageChannel();
        }
        return taskManager.createTaskExecuteContext(taskOrder, responseContext.getRequestContext(), taskMessageChannel);
    }

}
