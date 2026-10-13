package com.bin.actx.model.factory.getter;

import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * OpenAI模型消息获取器
 *
 * @author Bin
 * @since 2026/6/26 11:05
 */
public class OpenAiModelMessageGetter extends ModelMessageGetter {

    @Override
    public String getReasoningContent(AssistantMessage assistantMessage) {
        String reasoningContent = (String) assistantMessage.getMetadata().get("reasoning_content");
        if (reasoningContent == null) {
            reasoningContent = (String) assistantMessage.getMetadata().get("reasoningContent");
        }
        if (reasoningContent == null) {
            reasoningContent = (String) assistantMessage.getMetadata().get("reasoning");
        }
        return reasoningContent;
    }

}
