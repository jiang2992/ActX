package com.bin.actx.model.factory.getter;

import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 模型消息获取器抽象类，用于不同厂商模型的响应消息分析、获取等。
 *
 * @author Bin
 * @since 2026/6/26 10:56
 */
public abstract class ModelMessageGetter {

    public String getContent(AssistantMessage assistantMessage) {
        return assistantMessage.getText();
    }

    abstract public String getReasoningContent(AssistantMessage assistantMessage);

}
