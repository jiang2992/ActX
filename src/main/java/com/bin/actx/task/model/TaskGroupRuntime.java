package com.bin.actx.task.model;

import com.bin.actx.task.model.TaskPlan.SubTask;
import com.bin.actx.task.model.TaskPlan.TaskGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class TaskGroupRuntime {

    private TaskGroup taskGroup;

    private List<SubTask> completeSubTasks = new ArrayList<>();

    private List<SubTask> todoSubTasks = new ArrayList<>();

    private Map<String, String> inputMap;

}
