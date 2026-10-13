package com.bin.actx.message.section;

import lombok.Data;
import org.jiang.tools.json.JsonUtils;

/**
 * 工具应答片段
 *
 * @author Bin
 * @since 2026/6/6 12:16
 */
@Data
public class ToolAnswerSection implements AnswerSection {

    private final String type = "tool_call";

    private String id;

    private String name;

    private String arguments;

    private String result;

    @Override
    public void fromText(String content) {
        ToolAnswerSection ts = JsonUtils.toBean(content, ToolAnswerSection.class);
        this.id = ts.id;
        this.name = ts.name;
        this.arguments = ts.arguments;
        this.result = ts.result;
    }

    @Override
    public String toText() {
        return JsonUtils.toJson(this);
    }

}
