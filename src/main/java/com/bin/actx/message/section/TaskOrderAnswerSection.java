package com.bin.actx.message.section;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jiang.tools.json.JsonUtils;

/**
 * 任务单片段
 *
 * @author Bin
 * @since 2026/6/6 12:16
 */
@Data
@NoArgsConstructor
public class TaskOrderAnswerSection implements AnswerSection {

    private final String type = "task_order";

    private String id;

    private String summary;

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

        private String assignedAgent;

        private String assignedAgentName;

    }

    @Override
    public void fromText(String content) {
        TaskOrderAnswerSection ts = JsonUtils.toBean(content, TaskOrderAnswerSection.class);
        this.id = ts.id;
        this.summary = ts.summary;
        this.taskGroups = ts.taskGroups;
    }

    @Override
    public String toText() {
        return JsonUtils.toJson(this);
    }

}
