package com.bin.actx.task;

import com.bin.actx.channel.TaskMessageChannel;
import com.bin.actx.context.RequestContext;
import com.bin.actx.exception.TaskExecuteContextException;
import com.bin.actx.exception.TaskPlanVerifyException;
import com.bin.actx.storage.local.dao.SubTaskExecuteResultDao;
import com.bin.actx.storage.local.dao.TaskEvaluateResultDao;
import com.bin.actx.storage.local.dao.TaskOrderDao;
import com.bin.actx.storage.local.dao.TaskPlanDao;
import com.bin.actx.storage.local.entity.SubTaskExecuteResultEntity;
import com.bin.actx.storage.local.entity.TaskEvaluateResultEntity;
import com.bin.actx.storage.local.entity.TaskOrderEntity;
import com.bin.actx.storage.local.entity.TaskPlanEntity;
import com.bin.actx.task.constant.SubTaskConstant;
import com.bin.actx.task.constant.TaskConstant;
import com.bin.actx.task.context.TaskExecuteContext;
import com.bin.actx.task.model.SubTaskBriefInfo;
import com.bin.actx.task.model.SubTaskOutputInfo;
import com.bin.actx.task.model.TaskBriefInfo;
import com.bin.actx.task.model.TaskEvaluateInfo;
import com.bin.actx.task.model.TaskOrder;
import com.bin.actx.task.model.TaskPlan;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jiang.tools.json.JsonUtils;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 任务管理器
 *
 * @author Bin
 * @since 2026/5/20 17:23
 */
@Component
public class TaskManager {

    @Autowired
    private TaskPlanDao taskPlanDao;

    @Autowired
    private TaskOrderDao taskOrderDao;

    @Autowired
    private SubTaskExecuteResultDao subTaskExecuteResultDao;

    @Autowired
    private TaskEvaluateResultDao taskEvaluateResultDao;

    private Map<String, TaskExecuteContext> taskExecuteContext = new ConcurrentHashMap<>();

    @Transaction
    public TaskOrder createTask(TaskEvaluateInfo evaluateResult, TaskPlan planningResult) {
        this.verifyTaskPlan(planningResult);

        TaskOrder taskOrder = new TaskOrder();
        taskOrder.setId(IdUtils.uuid());
        taskOrder.setPlan(planningResult);
        taskOrder.setStatus(TaskConstant.STATUS_CREATED);

        // 保存计划
        TaskPlanEntity taskPlanEntity = new TaskPlanEntity();
        taskPlanEntity.setId(IdUtils.uuid());
        taskPlanEntity.setTaskSummary(planningResult.getTaskSummary());
        taskPlanEntity.setTaskDescription(planningResult.getTaskDescription());
        taskPlanEntity.setTaskGroupsJson(JsonUtils.toJson(planningResult.getTaskGroups()));
        taskPlanDao.upsert(taskPlanEntity);

        // 保存任务单
        TaskOrderEntity taskOrderEntity = new TaskOrderEntity();
        taskOrderEntity.setId(taskOrder.getId());
        taskOrderEntity.setPlanId(taskPlanEntity.getId());
        taskOrderEntity.setStatus(taskOrder.getStatus());
        taskOrderDao.upsert(taskOrderEntity);

        return taskOrder;
    }

    /**
     * 创建任务执行上下文
     *
     * @param taskOrder      任务单
     * @param requestContext 请求上下文
     * @return 任务执行上下文
     */
    public synchronized TaskExecuteContext createTaskExecuteContext(TaskOrder taskOrder, RequestContext requestContext,
            TaskMessageChannel taskMessageChannel) {
        TaskExecuteContext taskExecuteContext = this.taskExecuteContext.get(taskOrder.getId());
        if (taskExecuteContext != null) {
            throw new TaskExecuteContextException(
                    "Task conflict. This task has been executed by another dispatcher, another dispatcher info:[session=%s, agent=%s, thread=%s]".formatted(
                            taskExecuteContext.getSessionId(), taskExecuteContext.getDispatchAgent().getId(),
                            taskExecuteContext.getDispatchThread().getName()));
        }
        taskExecuteContext = new TaskExecuteContext(requestContext.getUserId(), requestContext.getSessionId(), taskOrder,
                requestContext.getAgentInstance(),
                taskMessageChannel);
        this.taskExecuteContext.put(taskOrder.getId(), taskExecuteContext);
        return taskExecuteContext;
    }

    /**
     * 销毁任务执行上下文
     *
     * @param taskOrderId 任务单ID
     */
    public void destroyTaskExecuteContext(String taskOrderId) {
        this.taskExecuteContext.remove(taskOrderId);
    }

    /**
     * 更新任务单状态
     *
     * @param taskOrder 任务单
     * @param status    状态
     */
    public void updateTaskOrderStatus(TaskOrder taskOrder, String status) {
        this.updateTaskOrderStatus(taskOrder, status, null);
    }

    /**
     * 更新任务单状态
     *
     * @param taskOrder 任务单
     * @param status    状态
     * @param output    任务输出（最终输出）
     */
    public void updateTaskOrderStatus(TaskOrder taskOrder, String status, String output) {
        TaskOrderEntity taskOrderEntity = taskOrderDao.findById(taskOrder.getId());
        taskOrderEntity.setStatus(status);
        taskOrderEntity.setOutput(output);
        taskOrderDao.upsert(taskOrderEntity);
        taskOrder.setStatus(status);
        taskOrder.setOutput(output);
    }

    public void saveSubTaskCompleteResult(String taskOrderId, String code, String output, String additional) {
        SubTaskExecuteResultEntity subTaskExecuteResultEntity = new SubTaskExecuteResultEntity();
        subTaskExecuteResultEntity.setId(IdUtils.uuid());
        subTaskExecuteResultEntity.setCode(code);
        subTaskExecuteResultEntity.setTaskOrderId(taskOrderId);
        subTaskExecuteResultEntity.setStatus(SubTaskConstant.RESULT_COMPLETED);
        subTaskExecuteResultEntity.setOutput(output);
        subTaskExecuteResultEntity.setAdditional(additional);
        subTaskExecuteResultDao.upsert(subTaskExecuteResultEntity);
    }

    public void saveSubTaskTerminateResult(String taskOrderId, String code, String output, String additional) {
        SubTaskExecuteResultEntity subTaskExecuteResultEntity = new SubTaskExecuteResultEntity();
        subTaskExecuteResultEntity.setId(IdUtils.uuid());
        subTaskExecuteResultEntity.setCode(code);
        subTaskExecuteResultEntity.setTaskOrderId(taskOrderId);
        subTaskExecuteResultEntity.setStatus(SubTaskConstant.RESULT_TERMINATION);
        subTaskExecuteResultEntity.setOutput(output);
        subTaskExecuteResultEntity.setAdditional(additional);
        subTaskExecuteResultDao.upsert(subTaskExecuteResultEntity);
    }

    public TaskOrder getTask(String id) {
        TaskOrderEntity taskOrderEntity = taskOrderDao.findById(id);
        if (taskOrderEntity == null) {
            return null;
        }

        TaskPlanEntity taskPlanEntity = taskPlanDao.findById(taskOrderEntity.getPlanId());
        TaskPlan planningResult = new TaskPlan();
        planningResult.setTaskSummary(taskPlanEntity.getTaskSummary());
        planningResult.setTaskDescription(taskPlanEntity.getTaskDescription());
        planningResult.setTaskGroups(JsonUtils.toList(taskPlanEntity.getTaskGroupsJson(), TaskPlan.TaskGroup.class));

        TaskOrder taskOrder = new TaskOrder();
        taskOrder.setId(taskOrderEntity.getId());
        taskOrder.setStatus(taskOrderEntity.getStatus());
        taskOrder.setPlan(planningResult);
        taskOrder.setOutput(taskOrderEntity.getOutput());

        return taskOrder;
    }

    public Map<String, SubTaskOutputInfo> getSubTaskOutputs(String taskOrderId, Collection<String> subTaskCodes) {
        List<SubTaskExecuteResultEntity> list = subTaskExecuteResultDao.findByTaskOrderIdAndCodes(taskOrderId, subTaskCodes);
        return list.stream().collect(Collectors.toMap(SubTaskExecuteResultEntity::getCode, item -> {
            SubTaskOutputInfo outputInfo = new SubTaskOutputInfo();
            outputInfo.setOutput(item.getOutput());
            outputInfo.setAdditional(item.getAdditional());
            return outputInfo;
        }));
    }

    public List<String> getCompletedSubTaskCodes(String taskOrderId) {
        return subTaskExecuteResultDao.findCompletedCodeByTaskOrderId(taskOrderId).stream().map(SubTaskExecuteResultEntity::getCode)
                .toList();
    }

    public List<SubTaskBriefInfo> getSubTaskBriefInfo(String taskOrderId) {
        return subTaskExecuteResultDao.findByTaskOrderId(taskOrderId).stream().map(item -> {
            SubTaskBriefInfo briefInfo = new SubTaskBriefInfo();
            briefInfo.setCode(item.getCode());
            briefInfo.setStatus(item.getStatus());
            briefInfo.setOutput(item.getOutput());
            briefInfo.setAdditional(item.getAdditional());
            return briefInfo;
        }).toList();
    }

    public TaskBriefInfo getTaskBriefInfo(String taskOrderId) {
        TaskOrder taskOrder = this.getTask(taskOrderId);
        List<String> completedSubTask = this.getCompletedSubTaskCodes(taskOrderId);

        TaskBriefInfo briefInfo = new TaskBriefInfo();
        briefInfo.setId(taskOrder.getId());
        briefInfo.setStatus(taskOrder.getStatus());
        briefInfo.setTaskSummary(taskOrder.getPlan().getTaskSummary());
        briefInfo.setTaskGroups(taskOrder.getPlan().getTaskGroups().stream().map(groupItem -> {
            TaskBriefInfo.TaskGroup taskGroup = new TaskBriefInfo.TaskGroup();
            taskGroup.setCode(groupItem.getCode());
            taskGroup.setTitle(groupItem.getTitle());
            taskGroup.setSubTasks(groupItem.getSubTasks().stream().map(subTaskItem -> {
                TaskBriefInfo.SubTask subTask = new TaskBriefInfo.SubTask();
                subTask.setCode(subTaskItem.getCode());
                subTask.setDescription(subTaskItem.getDescription());
                subTask.setStatus(
                        completedSubTask.contains(subTaskItem.getCode()) ? TaskConstant.STATUS_COMPLETED : TaskConstant.STATUS_CREATED);
                return subTask;
            }).toList());
            return taskGroup;
        }).toList());
        return briefInfo;
    }

    /**
     * 保存任务评估结果
     *
     * @param evaluateResult 评估结果
     * @return 评估结果ID
     */
    public String saveEvaluateResult(TaskEvaluateInfo evaluateResult) {
        TaskEvaluateResultEntity entity = new TaskEvaluateResultEntity();
        entity.setId(IdUtils.uuid());
        entity.setTaskSummary(evaluateResult.getTaskSummary());

        // 设置维度评分
        if (evaluateResult.getDimensionScores() != null) {
            TaskEvaluateInfo.DimensionScores scores = evaluateResult.getDimensionScores();
            entity.setStepsAndBranches(scores.getStepsAndBranches());
            entity.setDependencies(scores.getDependencies());
            entity.setKnowledgeBreadth(scores.getKnowledgeBreadth());
            entity.setUncertainty(scores.getUncertainty());
            entity.setExecutionRisk(scores.getExecutionRisk());
        }

        entity.setTotalScore(evaluateResult.getTotalScore());
        entity.setOverallComplexityLevel(evaluateResult.getOverallComplexityLevel());
        entity.setRequiresPlanning(evaluateResult.getRequiresPlanning());

        // 将风险因子列表转换为JSON字符串存储
        if (evaluateResult.getKeyRiskFactors() != null && !evaluateResult.getKeyRiskFactors().isEmpty()) {
            entity.setKeyRiskFactors(JsonUtils.toJson(evaluateResult.getKeyRiskFactors()));
        }

        entity.setSuggestion(evaluateResult.getSuggestion());

        taskEvaluateResultDao.upsert(entity);
        return entity.getId();
    }

    /**
     * 获取任务评估结果
     *
     * @param id 评估结果ID
     * @return 评估结果，不存在返回null
     */
    public TaskEvaluateInfo getEvaluateResult(String id) {
        TaskEvaluateResultEntity entity = taskEvaluateResultDao.findById(id);
        if (entity == null) {
            return null;
        }

        TaskEvaluateInfo result = new TaskEvaluateInfo();
        result.setTaskSummary(entity.getTaskSummary());

        // 构建维度评分
        TaskEvaluateInfo.DimensionScores scores = new TaskEvaluateInfo.DimensionScores();
        scores.setStepsAndBranches(entity.getStepsAndBranches());
        scores.setDependencies(entity.getDependencies());
        scores.setKnowledgeBreadth(entity.getKnowledgeBreadth());
        scores.setUncertainty(entity.getUncertainty());
        scores.setExecutionRisk(entity.getExecutionRisk());
        result.setDimensionScores(scores);

        result.setTotalScore(entity.getTotalScore());
        result.setOverallComplexityLevel(entity.getOverallComplexityLevel());
        result.setRequiresPlanning(entity.getRequiresPlanning());

        // 将JSON字符串转换回风险因子列表
        if (entity.getKeyRiskFactors() != null && !entity.getKeyRiskFactors().isEmpty()) {
            result.setKeyRiskFactors(JsonUtils.toList(entity.getKeyRiskFactors(), String.class));
        }

        result.setSuggestion(entity.getSuggestion());

        return result;
    }

    /**
     * 验证任务计划，不合法时抛出异常
     *
     * @param taskPlan 任务计划
     */
    private void verifyTaskPlan(TaskPlan taskPlan) {
        Set<String> availableTaskGroupInput = new HashSet<>();
        Set<String> availableSubTaskInput = new HashSet<>();
        for (TaskPlan.TaskGroup taskGroup : taskPlan.getTaskGroups()) {
            if (taskGroup.getSubTasks() == null) {
                throw new TaskPlanVerifyException("Task Group: " + taskGroup.getCode() + ". Sub-task cannot be empty.", taskPlan);
            }
            for (TaskPlan.SubTask subTask : taskGroup.getSubTasks()) {
                if (subTask.getInput() == null) {
                    continue;
                }
                for (String input : subTask.getInput()) {
                    if (input.equals(TaskPlan.INPUT_DEPENDENCY_ORIGINAL_REQUEST) || input.equals(
                            TaskPlan.INPUT_DEPENDENCY_TASK_GROUP_ALL)) {
                        continue;
                    }
                    if (!availableTaskGroupInput.contains(input) && !availableSubTaskInput.contains(input)) {
                        throw new TaskPlanVerifyException(
                                "Task Group: " + taskGroup.getCode() + ". Sub-task: " + subTask.getCode() + ". Input dependency: " + input
                                        + " is not available. (Can only rely on the previous task groups or sub-tasks)", taskPlan);
                    }
                }
            }
            availableTaskGroupInput.add(taskGroup.getCode());
            availableSubTaskInput.addAll(taskGroup.getSubTasks().stream().map(TaskPlan.SubTask::getCode).toList());
        }
    }

}
