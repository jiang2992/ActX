package com.bin.actx.task.context;

import com.bin.actx.agent.AgentInstance;
import com.bin.actx.channel.TaskMessageChannel;
import com.bin.actx.task.model.TaskOrder;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务执行上下文，生命周期为任务执行期间
 *
 * @author Bin
 * @since 2026/5/20 17:23
 */
@Slf4j
@Getter
public class TaskExecuteContext {

    private final String userId;

    private final String sessionId;

    private final TaskOrder taskOrder;

    private final AgentInstance dispatchAgent;

    private final Thread dispatchThread;

    private final LocalDateTime startTime;

    private final TaskMessageChannel taskMessageChannel;

    public TaskExecuteContext(String userId, String sessionId, TaskOrder taskOrder, AgentInstance dispatchAgent,
            TaskMessageChannel taskMessageChannel) {
        if (taskMessageChannel == null) {
            taskMessageChannel = new TaskMessageChannel() {
                // Create empty message channel instance
            };
            log.warn("Message channel is null, using empty message channel instance instead.");
        }

        this.userId = userId;
        this.sessionId = sessionId;
        this.taskOrder = taskOrder;
        this.dispatchAgent = dispatchAgent;
        this.dispatchThread = Thread.currentThread();
        this.startTime = LocalDateTime.now();
        this.taskMessageChannel = taskMessageChannel;
    }

    public TaskExecuteContext(TaskOrder taskOrder, AgentInstance dispatchAgent) {
        this(null, null, taskOrder, dispatchAgent, null);
    }

}
