package com.bin.actx.task;

import com.bin.actx.task.model.TaskPlan;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务计划缓存器
 *
 * @author Bin
 * @since 2026/6/1 20:54
 */
public class TaskPlanBuffer {

    /**
     * 从任务组Code关联任务组 Mapper (groupCode=>TaskGroup)
     */
    private final Map<String, TaskPlan.TaskGroup> taskGroupMap = new ConcurrentHashMap<>();

    /**
     * 从子任务Code关联任务组 Mapper (subTaskCode=>TaskGroup)
     */
    private final Map<String, TaskPlan.TaskGroup> subTaskGroupMap = new ConcurrentHashMap<>();

    /**
     * 从子任务Code关联子任务 Mapper (subTaskCode=>subTask)
     */
    private final Map<String, TaskPlan.SubTask> subTaskMap = new ConcurrentHashMap<>();


    public TaskPlanBuffer(TaskPlan taskPlan) {
        this.init(taskPlan);
    }

    private void init(TaskPlan taskPlan) {
        for (TaskPlan.TaskGroup taskGroup : taskPlan.getTaskGroups()) {
            taskGroupMap.put(taskGroup.getCode(), taskGroup);
            for (TaskPlan.SubTask subTask : taskGroup.getSubTasks()) {
                subTaskMap.put(subTask.getCode(), subTask);
                subTaskGroupMap.put(subTask.getCode(), taskGroup);
            }
        }
    }

    public TaskPlan.TaskGroup getTaskGroup(String taskGroupCode) {
        return taskGroupMap.get(taskGroupCode);
    }

    public TaskPlan.SubTask getSubTask(String subTaskCode) {
        return subTaskMap.get(subTaskCode);
    }

    public TaskPlan.TaskGroup getTaskGroupBySubTaskCode(String subTaskCode) {
        return subTaskGroupMap.get(subTaskCode);
    }

}
