package com.bin.actx.message.section;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jiang.tools.json.JsonUtils;

/**
 * 任务更新片段
 *
 * @author Bin
 * @since 2026/6/6 12:16
 */
@Data
@NoArgsConstructor
public class TaskUpdateAnswerSection implements AnswerSection {

    private final String type = "task_update";

    private String id;

    private String status;

    private String content;

    @Override
    public void fromText(String content) {
        TaskUpdateAnswerSection ts = JsonUtils.toBean(content, TaskUpdateAnswerSection.class);
        this.id = ts.id;
        this.status = ts.status;
        this.content = ts.content;
    }

    @Override
    public String toText() {
        return JsonUtils.toJson(this);
    }

}
