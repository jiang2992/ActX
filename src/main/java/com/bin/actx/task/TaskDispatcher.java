package com.bin.actx.task;

import com.bin.actx.context.ResponseContext;
import com.bin.actx.context.ResponseContextManager;
import com.bin.actx.service.TaskService;
import com.bin.actx.task.constant.TaskConstant;
import com.bin.actx.task.context.TaskExecuteContext;
import com.bin.actx.task.model.SponsorTaskInfo;
import com.bin.actx.task.model.SubTaskRuntime;
import com.bin.actx.task.model.TaskPlan.SubTask;
import com.bin.actx.task.result.SubTaskExecuteResult;
import com.bin.actx.task.result.TaskCompleteResult;
import com.bin.actx.task.result.TaskDispatchResult;
import com.bin.actx.task.result.TaskDispatchResult.SubTaskDispatchResult;
import com.bin.actx.task.result.TaskGroupExecuteResult;
import com.bin.actx.task.result.TaskLackResult;
import com.bin.actx.task.result.TaskResult;
import com.bin.actx.task.result.TaskTerminationResult;
import com.bin.actx.util.SpringUtils;
import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务调度器
 *
 * @author Bin
 * @since 2026/5/20 17:23
 */
@Slf4j
public class TaskDispatcher implements Closeable {

    /**
     * 任务执行上下文
     */
    private final TaskExecuteContext taskExecuteContext;

    /**
     * 任务管理器
     */
    private final TaskManager taskManager;

    /**
     * 任务服务
     */
    private final TaskService taskService;

    /**
     * 任务运行时管理器
     */
    private final TaskRuntimeManager taskRuntimeManager;

    /**
     * 子任务执行器
     */
    private final SubTaskExecutor subTaskExecutor;

    /**
     * 任务计划缓存
     */
    private final TaskPlanBuffer taskPlanBuffer;

    /**
     * 响应上下文管理器
     */
    private final ResponseContextManager responseContextManager;

    public TaskDispatcher(TaskExecuteContext taskExecuteContext) {
        this.taskExecuteContext = taskExecuteContext;
        this.taskManager = SpringUtils.getBean(TaskManager.class);
        this.taskService = SpringUtils.getBean(TaskService.class);
        this.responseContextManager = SpringUtils.getBean(ResponseContextManager.class);
        this.subTaskExecutor = new SubTaskExecutor(taskExecuteContext);
        this.taskRuntimeManager = new TaskRuntimeManager(taskExecuteContext);
        this.taskPlanBuffer = new TaskPlanBuffer(taskExecuteContext.getTaskOrder().getPlan());
    }

    /**
     * 执行任务单（整个任务流程的起始）
     *
     * @return 任务执行结果
     */
    public TaskResult run(Map<String, String> inputData) {
        taskManager.updateTaskOrderStatus(taskExecuteContext.getTaskOrder(), TaskConstant.STATUS_RUNNING);
        taskExecuteContext.getTaskMessageChannel().onTaskRunning(taskExecuteContext);

        // 写入发起者输入的信息
        if (inputData != null && !inputData.isEmpty()) {
            for (Entry<String, String> entry : inputData.entrySet()) {
                taskRuntimeManager.completeRuntime(entry.getKey(), entry.getValue(), null);
            }
        }

        Map<String, String> lastOutput = new HashMap<>();
        List<SubTaskRuntime> todoSubTaskRuntimes = taskRuntimeManager.getTodoSubTaskRuntimes();
        while (todoSubTaskRuntimes != null && !todoSubTaskRuntimes.isEmpty()) {
            lastOutput.clear();

            // 用户主动停止任务
            ResponseContext responseContext = responseContextManager.getBySessionId(taskExecuteContext.getSessionId());
            if (responseContext != null && responseContext.getStopInfo() != null) {
                TaskTerminationResult taskResult = new TaskTerminationResult(taskExecuteContext.getTaskOrder().getId(),
                        responseContext.getStopInfo().getContent());
                taskManager.updateTaskOrderStatus(taskExecuteContext.getTaskOrder(), TaskConstant.STATUS_TERMINATION,
                        responseContext.getStopInfo().getContent());
                taskRuntimeManager.clearRuntimes();
                taskExecuteContext.getTaskMessageChannel().onTaskTerminated(taskExecuteContext, taskResult);
                return taskResult;
            }

            // 执行任务
            TaskGroupExecuteResult taskGroupExecuteResult = this.executeSubTasks(todoSubTaskRuntimes);
            if (taskGroupExecuteResult.hasLackTasks()) {
                return new TaskLackResult(taskGroupExecuteResult.getLackTasks());
            }

            Map<String, SubTaskExecuteResult> subTaskExecuteResultMap = taskGroupExecuteResult.getSubTaskExecuteResult().stream()
                    .collect(Collectors.toMap(SubTaskExecuteResult::getCode, item -> item));

            // 请求调度者
            TaskDispatchResult taskDispatchResult = this.assessAndDispatcher(taskGroupExecuteResult);

            // 处理完成的调度结果
            for (SubTaskDispatchResult result : taskDispatchResult.getResults()) {
                String output = subTaskExecuteResultMap.get(result.getCode()).getOutput();
                if (result.getResult().equals(TaskDispatchResult.RESULT_CONTINUE)) {
                    boolean completedIsMainTask = taskRuntimeManager.completeRuntime(result.getRuntimeId(), output, result.getSuggest());
                    // 如果完成的是主干（非分支子任务），则保存最终子任务结果
                    if (completedIsMainTask) {
                        taskManager.saveSubTaskCompleteResult(taskExecuteContext.getTaskOrder().getId(),
                                result.getCode(),
                                output,
                                result.getSuggest()
                        );
                        taskExecuteContext.getTaskMessageChannel()
                                .onSubTaskComplete(taskExecuteContext, subTaskExecuteResultMap.get(result.getCode()));
                    }
                }
            }

            // 终止任务
            if (taskDispatchResult.isTermination()) {
                TaskTerminationResult taskResult = new TaskTerminationResult(taskExecuteContext.getTaskOrder().getId(),
                        taskDispatchResult.getTerminationSuggest());
                taskManager.updateTaskOrderStatus(taskExecuteContext.getTaskOrder(), TaskConstant.STATUS_TERMINATION,
                        taskDispatchResult.getTerminationSuggest());
                taskRuntimeManager.clearRuntimes();

                // 记录各个非成功的子任务
                for (SubTaskDispatchResult dispatchResult : taskDispatchResult.getResults()) {
                    switch (dispatchResult.getResult()) {
                        case TaskDispatchResult.RESULT_RETRY:
                            taskManager.saveSubTaskTerminateResult(taskExecuteContext.getTaskOrder().getId(),
                                    dispatchResult.getCode(),
                                    subTaskExecuteResultMap.get(dispatchResult.getCode()).getOutput(),
                                    "因同组子任务终止，跳过重试，原始建议:" + dispatchResult.getSuggest()
                            );
                            break;
                        case TaskDispatchResult.RESULT_LACK:
                            taskManager.saveSubTaskTerminateResult(taskExecuteContext.getTaskOrder().getId(),
                                    dispatchResult.getCode(),
                                    subTaskExecuteResultMap.get(dispatchResult.getCode()).getOutput(),
                                    "因同组子任务终止，跳过补充，原始建议:" + dispatchResult.getSuggest()
                            );
                            break;
                        case TaskDispatchResult.RESULT_TERMINATION:
                            taskManager.saveSubTaskTerminateResult(taskExecuteContext.getTaskOrder().getId(),
                                    dispatchResult.getCode(),
                                    subTaskExecuteResultMap.get(dispatchResult.getCode()).getOutput(),
                                    dispatchResult.getSuggest()
                            );
                            break;
                    }
                    if (!TaskDispatchResult.RESULT_CONTINUE.equals(dispatchResult.getResult())) {
                        taskExecuteContext.getTaskMessageChannel()
                                .onSubTaskTerminated(taskExecuteContext, subTaskExecuteResultMap.get(dispatchResult.getCode()));
                    }
                }
                taskExecuteContext.getTaskMessageChannel().onTaskTerminated(taskExecuteContext, taskResult);
                return taskResult;
            }

            // 处理重试和补充的调度结果
            for (SubTaskDispatchResult result : taskDispatchResult.getResults()) {
                String output = subTaskExecuteResultMap.get(result.getCode()).getOutput();
                switch (result.getResult()) {
                    case TaskDispatchResult.RESULT_RETRY:
                        taskRuntimeManager.retryRuntime(result.getRuntimeId(), output, result.getSuggest());
                        break;
                    case TaskDispatchResult.RESULT_LACK:
                        taskRuntimeManager.lackRuntime(result.getRuntimeId(), output, result.getSuggest());
                        break;
                }
            }

            // 如果是全部成功（继续）
            if (taskDispatchResult.isAllContinue()) {
                taskRuntimeManager.clearRuntimes();
            }

            // 记录最后结果
            for (Entry<String, SubTaskExecuteResult> entry : subTaskExecuteResultMap.entrySet()) {
                SubTask subTask = taskPlanBuffer.getSubTask(entry.getKey());
                lastOutput.put(subTask.getDescription(), entry.getValue().getOutput());
            }

            taskRuntimeManager.updateTodoSubTaskCodes();
            todoSubTaskRuntimes = taskRuntimeManager.getTodoSubTaskRuntimes();

        }
        TaskCompleteResult taskResult = new TaskCompleteResult();

        // 处理任务最终输出
        String finalOutput;
        if (lastOutput.isEmpty()) {
            finalOutput = "The task no output.";
        } else if (lastOutput.size() == 1) {
            finalOutput = lastOutput.values().iterator().next();
        } else {
            finalOutput = "Composite output result (%s final sub-tasks):\n".formatted(lastOutput.size());
            finalOutput += lastOutput.entrySet().stream()
                    .map(entry -> "- %s: %s".formatted(entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining("\n"));
        }
        taskResult.setOutput(finalOutput);
        taskRuntimeManager.clearRuntimes();
        taskManager.updateTaskOrderStatus(taskExecuteContext.getTaskOrder(), TaskConstant.STATUS_COMPLETED, finalOutput);
        taskExecuteContext.getTaskMessageChannel().onTaskComplete(taskExecuteContext, taskResult);
        return taskResult;
    }

    public TaskGroupExecuteResult executeSubTasks(List<SubTaskRuntime> todoRuntimes) {
        TaskGroupExecuteResult taskGroupExecuteResult = new TaskGroupExecuteResult();

        // 判断是否需要发起者介入
        List<SponsorTaskInfo> sponsorTaskInfoList = subTaskExecutor.discoverDispatcherTask(todoRuntimes);
        if (!sponsorTaskInfoList.isEmpty()) {
            for (SponsorTaskInfo sponsorTaskInfo : sponsorTaskInfoList) {
                taskGroupExecuteResult.addLackTasks(sponsorTaskInfo);
            }
            return taskGroupExecuteResult;
        }

        // 并行执行子任务
        Map<SubTask, SubTaskExecuteResult> subTaskExecuteResultMap = subTaskExecutor.parallelExecute(todoRuntimes);
        for (Entry<SubTask, SubTaskExecuteResult> entry : subTaskExecuteResultMap.entrySet()) {
            SubTaskExecuteResult subTaskExecuteResult = entry.getValue();
            taskGroupExecuteResult.addSubTaskExecuteResult(subTaskExecuteResult);
        }

        return taskGroupExecuteResult;
    }

    private TaskDispatchResult assessAndDispatcher(TaskGroupExecuteResult taskGroupExecuteResult) {
        log.debug("Assessing and dispatching sub-tasks: {}",
                taskGroupExecuteResult.getSubTaskExecuteResult().stream().map(SubTaskExecuteResult::getCode)
                        .collect(Collectors.joining(",")));
        TaskDispatchResult result = taskService.dispatch(taskRuntimeManager, taskGroupExecuteResult);
        log.debug("Dispatch result: {}", result);
        return result;
    }

    @Override
    public void close() {
        subTaskExecutor.close();
    }

}
