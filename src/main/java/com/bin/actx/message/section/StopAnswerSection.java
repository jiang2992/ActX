package com.bin.actx.message.section;

import lombok.Data;

/**
 * 停止应答片段
 *
 * @author Bin
 * @since 2026/8/4 17:28
 */
@Data
public class StopAnswerSection implements AnswerSection {

    private final String type = "stop";

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
