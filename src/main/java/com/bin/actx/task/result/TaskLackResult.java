package com.bin.actx.task.result;

import com.bin.actx.task.model.SponsorTaskInfo;
import java.util.List;

public class TaskLackResult extends TaskResult {

    private List<SponsorTaskInfo> lackTasks;

    public TaskLackResult(List<SponsorTaskInfo> lackTasks) {
        this.lackTasks = lackTasks;
    }

}
