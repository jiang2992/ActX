package com.bin.actx.message.section;

import lombok.Data;

/**
 * 提示应答片段
 *
 * @author Bin
 * @since 2026/6/6 12:17
 */
@Data
public class TellAnswerSection implements AnswerSection {

    private final String type = "tell";

    private String content;

    @Override
    public void fromText(String content) {
        this.content = content;
    }

    @Override
    public String toText() {
        return this.content;
    }

}
