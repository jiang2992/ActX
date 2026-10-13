package com.bin.actx.channel;

import com.bin.actx.agent.AgentManager;
import com.bin.actx.channel.event.StreamEvent;
import com.bin.actx.channel.event.StreamEvent.ToolCallPayload;
import com.bin.actx.channel.event.StreamEvent.ToolResultPayload;
import com.bin.actx.channel.event.StreamEventType;
import com.bin.actx.context.ResponseStopInfo;
import com.bin.actx.message.AnswerSessionMessage;
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
import org.jiang.tools.json.JsonUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * SSE消息通道
 *
 * @author Bin
 * @since 2026/5/27 16:14
 */
@Slf4j
public class SSEMessageChannel implements ChatMessageChannel, TaskMessageChannel {

    private final Sinks.Many<StreamEvent> sinks = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public void onThinkingStream(String content) {
        this.emit(new StreamEvent(StreamEventType.THINKING, content));
    }

    @Override
    public void onAnswerStream(String content) {
        this.emit(new StreamEvent(StreamEventType.MESSAGE, content));
    }

    @Override
    public void onToolCalling(ToolCallPayload toolCallPayload) {
        this.emit(new StreamEvent(StreamEventType.TOOL_CALL, JsonUtils.toJson(toolCallPayload)));
    }

    @Override
    public void onToolCallingResult(ToolResultPayload toolResultPayload) {
        this.emit(new StreamEvent(StreamEventType.TOOL_RESULT, JsonUtils.toJson(toolResultPayload)));
    }

    @Override
    public void onTell(String content) {
        this.emit(new StreamEvent(StreamEventType.TELL, content));
    }

    @Override
    public void onStop(ResponseStopInfo stopInfo) {
        this.emit(new StreamEvent(StreamEventType.STOP, stopInfo.getContent()));
    }

    @Override
    public void onError(Throwable throwable) {
        StreamEvent errorEvent;
        if (throwable instanceof WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            errorEvent = new StreamEvent(StreamEventType.ERROR, "Model response error: " + responseBody);
        } else {
            errorEvent = new StreamEvent(StreamEventType.ERROR, "The request processing has failed. Please try again later.");
        }
        this.emit(errorEvent);
    }

    @Override
    public void onEnd(AnswerSessionMessage answerSessionMessage) {
        this.complete();
    }

    @Override
    public void onTaskCreated(TaskOrder taskOrder) {
        AgentManager agentManager = SpringUtils.getBean(AgentManager.class);
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
        this.emit(new StreamEvent(StreamEventType.TASK_ORDER, JsonUtils.toJson(section)));
    }

    @Override
    public void onTaskRunning(TaskExecuteContext taskExecuteContext) {
        TaskMessageChannel.super.onTaskRunning(taskExecuteContext);
    }

    @Override
    public void onTaskComplete(TaskExecuteContext taskExecuteContext, TaskCompleteResult taskResult) {
        TaskUpdateAnswerSection section = new TaskUpdateAnswerSection();
        section.setId(taskResult.getId());
        section.setStatus(TaskConstant.STATUS_COMPLETED);
        this.emit(new StreamEvent(StreamEventType.TASK_UPDATE, JsonUtils.toJson(section)));
    }

    @Override
    public void onTaskTerminated(TaskExecuteContext taskExecuteContext, TaskTerminationResult taskResult) {
        TaskUpdateAnswerSection section = new TaskUpdateAnswerSection();
        section.setId(taskResult.getId());
        section.setStatus(TaskConstant.STATUS_TERMINATION);
        this.emit(new StreamEvent(StreamEventType.TASK_UPDATE, JsonUtils.toJson(section)));
        this.emit(new StreamEvent(StreamEventType.TELL, "任务终止：任务单执行失败"));
    }

    @Override
    public void onSubTaskCreated(TaskExecuteContext taskExecuteContext, SubTask subTask) {
        TaskMessageChannel.super.onSubTaskCreated(taskExecuteContext, subTask);
    }

    @Override
    public void onSubTaskRunning(TaskExecuteContext taskExecuteContext, SubTask subTask) {
        SubTaskUpdateAnswerSection section = new SubTaskUpdateAnswerSection();
        section.setTaskId(taskExecuteContext.getTaskOrder().getId());
        section.setCode(subTask.getCode());
        section.setStatus(TaskConstant.STATUS_RUNNING);
        this.emit(new StreamEvent(StreamEventType.SUB_TASK_UPDATE, JsonUtils.toJson(section)));
    }

    @Override
    public void onSubTaskComplete(TaskExecuteContext taskExecuteContext, SubTaskExecuteResult subTaskExecuteResult) {
        SubTaskUpdateAnswerSection section = new SubTaskUpdateAnswerSection();
        section.setTaskId(taskExecuteContext.getTaskOrder().getId());
        section.setCode(subTaskExecuteResult.getCode());
        section.setStatus(TaskConstant.STATUS_COMPLETED);
        this.emit(new StreamEvent(StreamEventType.SUB_TASK_UPDATE, JsonUtils.toJson(section)));
    }

    @Override
    public void onSubTaskTerminated(TaskExecuteContext taskExecuteContext, SubTaskExecuteResult subTaskExecuteResult) {
        String exception = subTaskExecuteResult.isSuccess() ? "Unknown Exception (exception is null)"
                : (subTaskExecuteResult.getException().getClass().getName() + " - " + subTaskExecuteResult.getException().getMessage());

        SubTaskUpdateAnswerSection section = new SubTaskUpdateAnswerSection();
        section.setTaskId(taskExecuteContext.getTaskOrder().getId());
        section.setCode(subTaskExecuteResult.getCode());
        section.setStatus(TaskConstant.STATUS_TERMINATION);
        section.setContent(exception);
        this.emit(new StreamEvent(StreamEventType.SUB_TASK_UPDATE, JsonUtils.toJson(section)));
    }

    private synchronized void emit(StreamEvent event) {
        sinks.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private void complete() {
        this.emit(new StreamEvent(StreamEventType.DONE, "DONE"));
        sinks.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
    }

    public Flux<StreamEvent> asFlux() {
        return sinks.asFlux();
    }

}
