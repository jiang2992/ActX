package com.bin.actx.service;

import com.bin.actx.agent.AgentInstance;
import com.bin.actx.agent.AgentManager;
import com.bin.actx.config.GlobalStatusManager;
import com.bin.actx.config.constant.SceneConstant;
import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.exception.TaskExecuteContextException;
import com.bin.actx.exception.TaskPlanVerifyException;
import com.bin.actx.message.handler.RequestMessageHandler;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.service.request.UserChatRequest;
import com.bin.actx.task.TaskRuntimeManager;
import com.bin.actx.task.constant.SubTaskConstant;
import com.bin.actx.task.context.TaskExecuteContext;
import com.bin.actx.task.model.SubTaskRuntime;
import com.bin.actx.task.model.TaskEvaluateInfo;
import com.bin.actx.task.model.TaskPlan;
import com.bin.actx.task.result.TaskDispatchResult;
import com.bin.actx.task.result.TaskDispatchResult.SubTaskDispatchResult;
import com.bin.actx.task.result.TaskGroupExecuteResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.json.JsonUtils;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 任务服务
 *
 * @author Bin
 * @since 2026/5/20 17:17
 */
@Slf4j
@Component
public class TaskService {

    @Autowired
    private AgentManager agentManager;
    @Autowired
    private ChatModelManager chatModelManager;
    @Autowired
    private GlobalStatusManager globalStatusManager;
    @Autowired
    private RequestMessageHandler requestMessageHandler;

    public TaskEvaluateInfo evaluate(String taskContent) {
        String agentId = globalStatusManager.getSceneAgentId(SceneConstant.TASK_EVALUATE);
        AgentInstance agentInstance = agentManager.getInstance(agentId);
        ChatClient chatClient = chatModelManager.getClient(agentInstance.getConfig().getModelId());

        RequestContext requestContext = new RequestContext(agentInstance).withScene(SceneConstant.TASK_EVALUATE);
        ResponseContext responseContext = new ResponseContext(requestContext, null);

        UserChatRequest userChatRequest = new UserChatRequest("User task:\n" + taskContent);
        RequestMessagePayload requestMessagePayload = requestMessageHandler.handle(requestContext, userChatRequest);
        ChatClientRequestSpec requestSpec = requestMessageHandler.createRequestSpec(responseContext, requestMessagePayload, chatClient);

        String content = requestSpec.call().content();
        if (StringUtils.isEmpty(content)) {
            throw new TaskExecuteContextException("Task evaluate failed: response content is null");
        }
        try {
            return JsonUtils.toBean(content, TaskEvaluateInfo.class);
        } catch (Exception e) {
            throw new TaskExecuteContextException("Task evaluate failed, failed to convert the Json object: " + e.getMessage());
        }
    }

    public TaskPlan planning(String userId, String taskContent, TaskEvaluateInfo evaluateResult) {
        return planning(userId, taskContent, evaluateResult, null);
    }

    /**
     * 规划任务
     *
     * @param taskContent             任务内容
     * @param evaluateResult          评估结果
     * @param taskPlanVerifyException 验证任务计划异常
     * @return 任务计划
     */
    public TaskPlan planning(String userId, String taskContent, TaskEvaluateInfo evaluateResult,
            TaskPlanVerifyException taskPlanVerifyException) {
        String agentId = globalStatusManager.getSceneAgentId(SceneConstant.TASK_PLANNING);
        AgentInstance agentInstance = agentManager.getInstance(agentId);
        ChatClient chatClient = chatModelManager.getClient(agentInstance.getConfig().getModelId());

        RequestContext requestContext = new RequestContext(agentInstance)
                .withUserId(userId)
                .withScene(SceneConstant.TASK_PLANNING);
        ResponseContext responseContext = new ResponseContext(requestContext, null);

        StringBuilder input = new StringBuilder();
        input.append("[User task]:\n");
        input.append(taskContent).append("\n");
        input.append("[Evaluate Result]:\n");
        input.append(JsonUtils.toJson(evaluateResult));
        if (taskPlanVerifyException != null) {
            input.append("\n");
            input.append("[Task Plan Verify Exception]:\n");
            input.append(
                    "The last task planning verification failed. Please re-think and re-output strictly following the constraints and format.\n");
            input.append("Last exception reason: ").append(taskPlanVerifyException.getMessage()).append("\n");
            input.append("Last output plan: ").append(JsonUtils.toJson(taskPlanVerifyException.getTaskPlan()));
        }
        UserChatRequest userChatRequest = new UserChatRequest(input.toString());
        RequestMessagePayload requestMessagePayload = requestMessageHandler.handle(requestContext, userChatRequest);
        ChatClientRequestSpec requestSpec = requestMessageHandler.createRequestSpec(responseContext, requestMessagePayload, chatClient);

        if (requestMessagePayload.getSystemPrompts() != null) {
            String agentList = agentManager.getInstanceList().stream().filter(item -> !item.getSystemPrivatized()).map(item -> {
                return "  - `%s`:%s - %s".formatted(item.getId(), item.getConfig().getName(), item.getConfig().getDescription());
            }).collect(Collectors.joining("\n"));
            String systemPrompt = String.join("\n", requestMessagePayload.getSystemPrompts());
            systemPrompt = systemPrompt.replace("${agent_list}", agentList);
            requestSpec.system(systemPrompt);
        }

        String content = requestSpec.call().content();
        if (StringUtils.isEmpty(content)) {
            throw new TaskPlanVerifyException("Task planning failed: response content is null");
        }
        try {
            return JsonUtils.toBean(content, TaskPlan.class);
        } catch (Exception e) {
            throw new TaskPlanVerifyException("Task planning failed, failed to convert the Json object: " + e.getMessage());
        }
    }

    public String executeSubTask(TaskExecuteContext taskExecuteContext, TaskPlan.SubTask subTask, Map<String, String> taskInput,
            List<String> taskRemark) {
        AgentInstance agentInstance = agentManager.getInstance(subTask.getAssignedAgent());
        if (agentInstance == null) {
            throw new TaskExecuteContextException("Agent not found: " + subTask.getAssignedAgent());
        }
        ChatClient chatClient = chatModelManager.getClient(agentInstance.getConfig().getModelId());

        RequestContext requestContext = new RequestContext(agentInstance)
                .withUserId(taskExecuteContext.getUserId())
                .withScene(SceneConstant.TASK_EXECUTOR);
        ResponseContext responseContext = new ResponseContext(requestContext, null);

        StringBuilder input = new StringBuilder();
        input.append("## Task description").append("\n");
        input.append(subTask.getDescription()).append("\n");
        input.append("## Expected Output").append("\n");
        input.append(subTask.getExpectedOutput()).append("\n");

        // 任务输入
        if (subTask.getInput() != null && !subTask.getInput().isEmpty()) {
            input.append("## Input information (derived from the original request or the preceding task)").append("\n");
            input.append(taskInput.entrySet().stream().map(item -> {
                return " - `%s`: ```%s```".formatted(item.getKey(), item.getValue());
            }).collect(Collectors.joining("\n"))).append("\n");
        }

        // 任务备注
        if (taskRemark != null && !taskRemark.isEmpty()) {
            input.append("## Task Execute Remark").append("\n");
            input.append(String.join("\n", taskRemark)).append("\n");
        }

        UserChatRequest userChatRequest = new UserChatRequest(input.toString());
        RequestMessagePayload requestMessagePayload = requestMessageHandler.handle(requestContext, userChatRequest);
        ChatClientRequestSpec requestSpec = requestMessageHandler.createRequestSpec(responseContext, requestMessagePayload, chatClient);

        String content = requestSpec.call().content();
        if (StringUtils.isEmpty(content)) {
            throw new TaskExecuteContextException("Task execute failed: response content is null");
        }

        return content;
    }

    public TaskDispatchResult dispatch(TaskRuntimeManager taskRuntimeManager, TaskGroupExecuteResult taskGroupExecuteResult) {
        String agentId = globalStatusManager.getSceneAgentId(SceneConstant.TASK_DISPATCHER);
        AgentInstance agentInstance = agentManager.getInstance(agentId);
        ChatClient chatClient = chatModelManager.getClient(agentInstance.getConfig().getModelId());

        RequestContext requestContext = new RequestContext(agentInstance).withScene(SceneConstant.TASK_DISPATCHER);
        ResponseContext responseContext = new ResponseContext(requestContext, null);

        Map<String, String> codeToRuntimes = new HashMap<>();
        List<Map<String, Object>> input = taskGroupExecuteResult.getSubTaskExecuteResult().stream().map(item -> {
            codeToRuntimes.put(item.getCode(), item.getRuntimeId());

            String output;
            if (item.isSuccess()) {
                output = item.getOutput();
            } else {
                output = "Task execution failed: " + item.getException().getClass().getName() + " - " + item.getException().getMessage();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("code", item.getCode());
            map.put("output", output);

            // 写入重试备注
            SubTaskRuntime runtime = taskRuntimeManager.getRuntime(item.getRuntimeId());
            if (runtime != null && runtime.hasBranch()) {
                List<SubTaskRuntime.Branch> historyList = runtime.getBranches().stream()
                        .filter(branch -> SubTaskConstant.RUNTIME_TYPE_HISTORY.equals(branch.getType()))
                        .sorted()
                        .toList();
                if (!historyList.isEmpty()) {
                    String remark = "This is the %sth attempt of this task. The result of the last execution was as you suggested: %s".formatted(
                            historyList.size(), historyList.get(historyList.size() - 1).getAdditional());
                    map.put("remark", remark);
                }
            }

            return map;
        }).toList();

        UserChatRequest userChatRequest = new UserChatRequest(JsonUtils.toJson(input));
        RequestMessagePayload requestMessagePayload = requestMessageHandler.handle(requestContext, userChatRequest);
        ChatClientRequestSpec requestSpec = requestMessageHandler.createRequestSpec(responseContext, requestMessagePayload, chatClient);

        if (requestMessagePayload.getSystemPrompts() != null) {
            String systemPrompt = String.join("\n", requestMessagePayload.getSystemPrompts());
            systemPrompt = systemPrompt.replace("${task_plan}",
                    JsonUtils.toJson(taskRuntimeManager.getTaskExecuteContext().getTaskOrder().getPlan()));
            requestSpec.system(systemPrompt);
        }

        String content = requestSpec.call().content();
        if (StringUtils.isEmpty(content)) {
            throw new TaskExecuteContextException("Task dispatch failed: response content is null");
        }

        try {
            List<SubTaskDispatchResult> subTaskDispatchResults = JsonUtils.toList(content, SubTaskDispatchResult.class);
            for (SubTaskDispatchResult subTaskDispatchResult : subTaskDispatchResults) {
                subTaskDispatchResult.setRuntimeId(codeToRuntimes.get(subTaskDispatchResult.getCode()));
            }
            TaskDispatchResult taskDispatchResult = new TaskDispatchResult();
            taskDispatchResult.setResults(subTaskDispatchResults);
            return taskDispatchResult;
        } catch (Exception e) {
            log.error("Task dispatch failed, failed to convert the Json object: {}", content);
            throw new TaskExecuteContextException("Task dispatch failed, failed to convert the Json object: " + e.getMessage());
        }
    }

}
