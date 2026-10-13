package com.bin.actx.message.section;

import lombok.Data;
import lombok.Getter;

/**
 * 消息应答片段
 *
 * @author Bin
 * @since 2026/6/6 12:16
 */
@Data
public class MessageAnswerSection implements AnswerSection {

    @Getter
    private final String type = "answer";

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
