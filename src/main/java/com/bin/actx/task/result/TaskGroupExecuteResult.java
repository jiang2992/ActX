package com.bin.actx.task.result;

import com.bin.actx.task.model.SponsorTaskInfo;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TaskGroupExecuteResult {

    /**
     * 需要调度者干预的任务
     */
    private List<SponsorTaskInfo> lackTasks = new ArrayList<>();

    /**
     * 任务输出
     */
    private List<SubTaskExecuteResult> subTaskExecuteResult = new ArrayList<>();

    public boolean hasLackTasks() {
        return lackTasks != null && !lackTasks.isEmpty();
    }

    public void addLackTasks(SponsorTaskInfo sponsorTaskInfo) {
        lackTasks.add(sponsorTaskInfo);
    }

    public void addSubTaskExecuteResult(SubTaskExecuteResult subTaskExecuteResult) {
        this.subTaskExecuteResult.add(subTaskExecuteResult);
    }

}
