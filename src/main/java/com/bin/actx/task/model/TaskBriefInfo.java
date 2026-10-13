package com.bin.actx.task.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class TaskBriefInfo implements Serializable {

    private String id;

    private String status;

    private String taskSummary;

    private List<TaskGroup> taskGroups;

    @Data
    public static class TaskGroup implements Serializable {

        private String code;

        private String title;

        private List<SubTask> subTasks;

    }

    @Data
    public static class SubTask implements Serializable {

        private String code;

        private String description;

        private String status;

    }

}
