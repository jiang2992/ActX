package com.bin.actx.task.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 任务计划
 *
 * @author Bin
 * @since 2026/6/1 20:42
 */
@Data
public class TaskPlan implements Serializable {

    public static final String INPUT_DEPENDENCY_ORIGINAL_REQUEST = "original_request";

    public static final String INPUT_DEPENDENCY_TASK_GROUP_ALL = "task_group_all";

    /**
     * 任务概要
     */
    private String taskSummary;

    /**
     * 任务描述
     */
    private String taskDescription;

    /**
     * 任务组列表
     */
    private List<TaskGroup> taskGroups;

    public int subTaskCount() {
        return taskGroups.stream().mapToInt(taskGroup -> taskGroup.getSubTasks().size()).sum();
    }

    @Data
    public static class TaskGroup implements Serializable {

        /**
         * 任务组编码(格式: 'task_group_' + 自增)
         */
        private String code;

        /**
         * 简单描述该任务组的主题
         */
        private String title;

        /**
         * 子任务列表
         */
        private List<SubTask> subTasks;
    }

    @Data
    public static class SubTask implements Serializable {

        /**
         * 子任务编码(格式: 'sub_task_' + 自增)
         */
        private String code;

        /**
         * 子任务描述
         */
        private String description;

        /**
         * 指派的AgentId
         */
        private String assignedAgent;

        /**
         * 期望的产出
         */
        private String expectedOutput;

        /**
         * 输入依赖列表 (例如: ["task_group_1","sub_task_1","task_group_all","original_request"])
         */
        private List<String> input;

    }

}
