package com.bin.actx.task;

import com.bin.actx.storage.local.dao.SubTaskRuntimeDao;
import com.bin.actx.storage.local.entity.SubTaskRuntimeEntity;
import com.bin.actx.task.constant.SubTaskConstant;
import com.bin.actx.task.context.TaskExecuteContext;
import com.bin.actx.task.model.SubTaskRuntime;
import com.bin.actx.task.model.TaskOrder;
import com.bin.actx.task.model.TaskPlan.SubTask;
import com.bin.actx.task.model.TaskPlan.TaskGroup;
import com.bin.actx.util.SpringUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.jiang.tools.text.id.IdUtils;

public class TaskRuntimeManager {

    @Getter
    private final TaskExecuteContext taskExecuteContext;

    private final TaskManager taskManager;

    private final SubTaskRuntimeDao subTaskRuntimeDao;

    private final TaskPlanBuffer taskPlanBuffer;

    private List<String> currentTodoSubTaskCodes;

    public TaskRuntimeManager(TaskExecuteContext taskExecuteContext) {
        this.taskExecuteContext = taskExecuteContext;
        this.taskManager = SpringUtils.getBean(TaskManager.class);
        this.subTaskRuntimeDao = SpringUtils.getBean(SubTaskRuntimeDao.class);
        this.taskPlanBuffer = new TaskPlanBuffer(taskExecuteContext.getTaskOrder().getPlan());
        this.updateTodoSubTaskCodes();
    }

    public void updateTodoSubTaskCodes() {
        List<String> todoSubTaskCodes = new ArrayList<>();
        Set<String> completedSubTasks = new HashSet<>(taskManager.getCompletedSubTaskCodes(taskExecuteContext.getTaskOrder().getId()));
        TaskOrder taskOrder = taskExecuteContext.getTaskOrder();
        for (TaskGroup taskGroup : taskOrder.getPlan().getTaskGroups()) {
            List<String> subTaskCodes = taskGroup.getSubTasks().stream().map(SubTask::getCode).toList();
            if (completedSubTasks.containsAll(subTaskCodes)) {
                continue;
            }

            for (SubTask subTask : taskGroup.getSubTasks()) {
                if (!completedSubTasks.contains(subTask.getCode())) {
                    todoSubTaskCodes.add(subTask.getCode());
                }
            }

            this.currentTodoSubTaskCodes = todoSubTaskCodes;
            return;
        }
    }

    /**
     * 完成指定任务运行时
     *
     * @param id      任务运行时id
     * @param output  完成结果
     * @param suggest 完成建议
     * @return 是否完成任务(非分支)
     */
    public boolean completeRuntime(String id, String output, String suggest) {
        SubTaskRuntimeEntity entity = subTaskRuntimeDao.findById(id);
        entity.setOutput(output);
        entity.setStatus(SubTaskConstant.RUNTIME_COMPLETED);
        entity.setAdditional(suggest);
        subTaskRuntimeDao.upsert(entity);

        if (entity.getType().equals(SubTaskConstant.RUNTIME_TYPE_MAIN)) {
            currentTodoSubTaskCodes.remove(entity.getCode());
            return true;
        }

        return false;
    }

    /**
     * 重试指定任务运行时
     *
     * @param id      任务运行时id
     * @param output  完成结果
     * @param suggest 重试建议
     */
    public void retryRuntime(String id, String output, String suggest) {
        SubTaskRuntimeEntity entity = subTaskRuntimeDao.findById(id);
        entity.setStatus(SubTaskConstant.RUNTIME_WAIT_FOR_RETRY);
        subTaskRuntimeDao.upsert(entity);

        SubTaskRuntimeEntity branchEntity = new SubTaskRuntimeEntity();
        branchEntity.setId(IdUtils.uuid());
        branchEntity.setCode(entity.getCode());
        branchEntity.setTaskOrderId(entity.getTaskOrderId());
        branchEntity.setType(SubTaskConstant.RUNTIME_TYPE_HISTORY);
        branchEntity.setStatus(SubTaskConstant.RUNTIME_COMPLETED);
        branchEntity.setDescription(entity.getDescription());
        branchEntity.setOutput(output);
        branchEntity.setAdditional(suggest);
        subTaskRuntimeDao.upsert(branchEntity);
    }

    /**
     * 将指定任务运行时标为待补充
     *
     * @param id      任务运行时id
     * @param output  完成结果
     * @param suggest 补充建议
     */
    public void lackRuntime(String id, String output, String suggest) {
        SubTaskRuntimeEntity entity = subTaskRuntimeDao.findById(id);
        entity.setStatus(SubTaskConstant.RUNTIME_WAIT_FOR_RETRY);
        subTaskRuntimeDao.upsert(entity);

        SubTaskRuntimeEntity branchEntity = new SubTaskRuntimeEntity();
        branchEntity.setId(IdUtils.uuid());
        branchEntity.setCode(entity.getCode());
        branchEntity.setTaskOrderId(entity.getTaskOrderId());
        branchEntity.setType(SubTaskConstant.RUNTIME_TYPE_LACK);
        branchEntity.setStatus(SubTaskConstant.RUNTIME_COMPLETED);
        branchEntity.setDescription(entity.getDescription());
        branchEntity.setOutput(output);
        branchEntity.setAdditional(suggest);
        subTaskRuntimeDao.upsert(branchEntity);
    }

    public SubTaskRuntime getRuntime(String id) {
        SubTaskRuntimeEntity entity = subTaskRuntimeDao.findById(id);
        if (entity == null) {
            return null;
        }
        List<SubTaskRuntimeEntity> entityList = subTaskRuntimeDao.findByCode(entity.getTaskOrderId(), entity.getCode());
        return this.formatEntityList(entity.getCode(), entityList);
    }

    public void clearRuntimes() {
        subTaskRuntimeDao.deleteByTaskOrderId(taskExecuteContext.getTaskOrder().getId());
    }

    public List<SubTaskRuntime> getTodoSubTaskRuntimes() {
        if (currentTodoSubTaskCodes.isEmpty()) {
            return new ArrayList<>();
        }

        List<SubTaskRuntimeEntity> entityList = subTaskRuntimeDao.findByCodes(taskExecuteContext.getTaskOrder().getId(),
                currentTodoSubTaskCodes);
        Map<String, List<SubTaskRuntimeEntity>> codeToEntityList = entityList.stream()
                .collect(Collectors.groupingBy(SubTaskRuntimeEntity::getCode));

        List<SubTaskRuntime> runtimes = new ArrayList<>(currentTodoSubTaskCodes.size());
        for (String subTaskCode : currentTodoSubTaskCodes) {
            List<SubTaskRuntimeEntity> entities = codeToEntityList.get(subTaskCode);
            if (entities == null) {
                SubTaskRuntimeEntity entity = this.saveSubTaskRuntime(subTaskCode);
                entities = List.of(entity);
            }
            SubTaskRuntime runtime = this.formatEntityList(subTaskCode, entities);
            runtimes.add(runtime);
        }

        return runtimes;
    }

    private SubTaskRuntime formatEntityList(String code, List<SubTaskRuntimeEntity> entityList) {
        SubTaskRuntime runtime = new SubTaskRuntime();
        runtime.setCode(code);
        List<SubTaskRuntime.Branch> branches = new ArrayList<>();
        for (SubTaskRuntimeEntity entity : entityList) {
            if (entity.getType().equals(SubTaskConstant.RUNTIME_TYPE_MAIN)) {
                runtime.setId(entity.getId());
                runtime.setStatus(entity.getStatus());
                runtime.setDescription(entity.getDescription());
                runtime.setOutput(entity.getOutput());
                runtime.setAdditional(entity.getAdditional());
                runtime.setTaskInfo(taskPlanBuffer.getSubTask(code));
                continue;
            }
            SubTaskRuntime.Branch branch = new SubTaskRuntime.Branch();
            branch.setId(entity.getId());
            branch.setType(entity.getType());
            branch.setStatus(entity.getStatus());
            branch.setOutput(entity.getOutput());
            branch.setDescription(entity.getDescription());
            branches.add(branch);
        }
        runtime.setBranches(branches);
        return runtime;
    }

    private SubTaskRuntimeEntity saveSubTaskRuntime(String code) {
        SubTask subTask = taskPlanBuffer.getSubTask(code);

        SubTaskRuntimeEntity entity = new SubTaskRuntimeEntity();
        entity.setId(IdUtils.uuid());
        entity.setCode(subTask.getCode());
        entity.setTaskOrderId(taskExecuteContext.getTaskOrder().getId());
        entity.setType(SubTaskConstant.RUNTIME_TYPE_MAIN);
        entity.setStatus(SubTaskConstant.RESULT_CREATED);
        entity.setDescription(subTask.getDescription());
        subTaskRuntimeDao.upsert(entity);

        return entity;
    }

}
