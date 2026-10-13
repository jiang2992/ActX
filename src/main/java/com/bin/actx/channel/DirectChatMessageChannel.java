package com.bin.actx.channel;

/**
 * 直连消息通道
 *
 * @author Bin
 * @since 2026/5/27 16:14
 */
public class DirectChatMessageChannel implements ChatMessageChannel {

    protected String answer;

    @Override
    public void onAnswer(String content) {
        this.answer = content;
    }

    public String getAnswer() {
        return this.answer;
    }

}
