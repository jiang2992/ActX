package com.bin.actx.message.section;

import lombok.Data;

/**
 * 错误应答片段
 *
 * @author Bin
 * @since 2026/6/6 12:17
 */
@Data
public class ErrorAnswerSection implements AnswerSection {

    private final String type = "error";

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
