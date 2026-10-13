package com.bin.actx.message.section;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jiang.tools.json.JsonUtils;

/**
 * 子任务更新片段
 *
 * @author Bin
 * @since 2026/6/6 12:16
 */
@Data
@NoArgsConstructor
public class SubTaskUpdateAnswerSection implements AnswerSection {

    private final String type = "sub_task_update";

    private String taskId;

    private String code;

    private String status;

    private String content;

    @Override
    public void fromText(String content) {
        SubTaskUpdateAnswerSection ts = JsonUtils.toBean(content, SubTaskUpdateAnswerSection.class);
        this.taskId = ts.taskId;
        this.code = ts.code;
        this.status = ts.status;
        this.content = ts.content;
    }

    @Override
    public String toText() {
        return JsonUtils.toJson(this);
    }

}
