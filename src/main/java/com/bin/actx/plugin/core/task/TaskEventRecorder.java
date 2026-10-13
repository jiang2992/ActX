package com.bin.actx.plugin.core.task;

import com.bin.actx.agent.AgentManager;
import com.bin.actx.channel.TaskMessageChannel;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.message.AnswerSessionMessageBuilder;
import com.bin.actx.message.section.SubTaskUpdateAnswerSection;
import com.bin.actx.message.section.TaskOrderAnswerSection;
import com.bin.actx.message.section.TaskUpdateAnswerSection;
import com.bin.actx.task.constant.TaskConstant;
import com.bin.actx.task.context.TaskExecuteContext;
import com.bin.actx.task.model.TaskOrder;
import com.bin.actx.task.model.TaskPlan.SubTask;
import com.bin.actx.task.result.SubTaskExecuteResult;
import com.bin.actx.task.result.TaskCompleteResult;
import com.bin.actx.task.result.TaskTerminationResult;
import com.bin.actx.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务事件记录器
 *
 * @author Bin
 * @since 2026/6/2 14:06
 */
@Slf4j
public class TaskEventRecorder implements TaskMessageChannel {

    private ResponseContext responseContext;
    private TaskExecuteContext taskExecuteContext;
    private AnswerSessionMessageBuilder answerSessionMessageBuilder;
    private AgentManager agentManager;

    public TaskEventRecorder(ResponseContext responseContext, AnswerSessionMessageBuilder answerSessionMessageBuilder) {
        this.responseContext = responseContext;
        this.answerSessionMessageBuilder = answerSessionMessageBuilder;
        this.agentManager = SpringUtils.getBean(AgentManager.class);
    }

    @Override
    public void onTaskCreated(TaskOrder taskOrder) {
        log.info("[{}] Task Order Created：{} ({} sub-tasks)", taskOrder.getId(), taskOrder.getPlan().subTaskCount(),
                taskOrder.getPlan().getTaskSummary());

        TaskOrderAnswerSection section = new TaskOrderAnswerSection();
        section.setId(taskOrder.getId());
        section.setSummary(taskOrder.getPlan().getTaskSummary());
        section.setTaskGroups(taskOrder.getPlan().getTaskGroups().stream().map(taskGroupItem -> {
            TaskOrderAnswerSection.TaskGroup taskGroup = new TaskOrderAnswerSection.TaskGroup();
            taskGroup.setCode(taskGroupItem.getCode());
            taskGroup.setTitle(taskGroupItem.getTitle());
            taskGroup.setSubTasks(taskGroupItem.getSubTasks().stream().map(subTaskItem -> {
                TaskOrderAnswerSection.SubTask subTask = new TaskOrderAnswerSection.SubTask();
                subTask.setCode(subTaskItem.getCode());
                subTask.setDescription(subTaskItem.getDescription());
                subTask.setAssignedAgent(subTaskItem.getAssignedAgent());
                subTask.setAssignedAgentName(agentManager.getInstance(subTaskItem.getAssignedAgent()).getConfig().getName());
                return subTask;
            }).toList());
            return taskGroup;
        }).toList());

        answerSessionMessageBuilder.addSection(section);
    }

    @Override
    public void onTaskRunning(TaskExecuteContext taskExecuteContext) {
        this.taskExecuteContext = taskExecuteContext;
        log.info("[{}] Task Order Running：Session={}, Agent={}, Thread={}", taskExecuteContext.getTaskOrder().getId(),
                taskExecuteContext.getSessionId(),
                taskExecuteContext.getDispatchAgent().getId(),
                taskExecuteContext.getDispatchThread().getName());
    }

    @Override
    public void onTaskComplete(TaskExecuteContext taskExecuteContext, TaskCompleteResult taskResult) {
        String output = taskResult.getOutput().length() > 500 ? (taskResult.getOutput().substring(0, 500) + "...")
                : taskResult.getOutput();
        log.info("[{}] Task Order Complete：{}", taskResult.getId(), output);

        TaskUpdateAnswerSection section = new TaskUpdateAnswerSection();
        section.setId(taskResult.getId());
        section.setStatus(TaskConstant.STATUS_COMPLETED);
        answerSessionMessageBuilder.addSection(section);
    }

    @Override
    public void onTaskTerminated(TaskExecuteContext taskExecuteContext, TaskTerminationResult taskResult) {
        log.info("[{}] Task Order Terminated：{}", taskResult.getId(), taskResult.getMessage());

        TaskUpdateAnswerSection section = new TaskUpdateAnswerSection();
        section.setId(taskResult.getId());
        section.setStatus(TaskConstant.STATUS_COMPLETED);
        answerSessionMessageBuilder.addSection(section);
    }

    @Override
    public void onSubTaskCreated(TaskExecuteContext taskExecuteContext, SubTask subTask) {
        log.debug("[{} - {}] Sub Task Created：{}", taskExecuteContext.getTaskOrder().getId(), subTask.getCode(), subTask.getDescription());
    }

    @Override
    public void onSubTaskRunning(TaskExecuteContext taskExecuteContext, SubTask subTask) {
        log.debug("[{} - {}] Sub Task Running：Agent={}", taskExecuteContext.getTaskOrder().getId(), subTask.getCode(),
                subTask.getAssignedAgent());

        SubTaskUpdateAnswerSection section = new SubTaskUpdateAnswerSection();
        section.setTaskId(taskExecuteContext.getTaskOrder().getId());
        section.setCode(subTask.getCode());
        section.setStatus(TaskConstant.STATUS_RUNNING);
        answerSessionMessageBuilder.addSection(section);
    }

    @Override
    public void onSubTaskComplete(TaskExecuteContext taskExecuteContext, SubTaskExecuteResult subTaskExecuteResult) {
        String output = subTaskExecuteResult.getOutput().length() > 500 ? (subTaskExecuteResult.getOutput().substring(0, 500) + "...")
                : subTaskExecuteResult.getOutput();
        log.debug("[{} - {}] Sub Task Complete：{}", taskExecuteContext.getTaskOrder().getId(), subTaskExecuteResult.getCode(), output);

        SubTaskUpdateAnswerSection section = new SubTaskUpdateAnswerSection();
        section.setTaskId(taskExecuteContext.getTaskOrder().getId());
        section.setCode(subTaskExecuteResult.getCode());
        section.setStatus(TaskConstant.STATUS_COMPLETED);
        answerSessionMessageBuilder.addSection(section);
    }

    @Override
    public void onSubTaskTerminated(TaskExecuteContext taskExecuteContext, SubTaskExecuteResult subTaskExecuteResult) {
        String exception = subTaskExecuteResult.isSuccess() ? "Unknown Exception (exception is null)"
                : (subTaskExecuteResult.getException().getClass().getName() + " - " + subTaskExecuteResult.getException().getMessage());
        log.debug("[{} - {}] Sub Task Terminated：{}", taskExecuteContext.getTaskOrder().getId(), subTaskExecuteResult.getCode(), exception);

        SubTaskUpdateAnswerSection section = new SubTaskUpdateAnswerSection();
        section.setTaskId(taskExecuteContext.getTaskOrder().getId());
        section.setCode(subTaskExecuteResult.getCode());
        section.setStatus(TaskConstant.STATUS_TERMINATION);
        section.setContent(exception);
        answerSessionMessageBuilder.addSection(section);
    }

}
