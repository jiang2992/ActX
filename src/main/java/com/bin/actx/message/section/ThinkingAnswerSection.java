package com.bin.actx.message.section;

import lombok.Data;

/**
 * 思考应答片段
 *
 * @author Bin
 * @since 2026/6/6 12:16
 */
@Data
public class ThinkingAnswerSection implements AnswerSection {

    private final String type = "thinking";

    private String content;

    @Override
    public void fromText(String content) {
        this.content = content;
    }

    @Override
    public String toText() {
        return this.content;
    }

    public void append(String content) {
        this.content += content;
    }

}
