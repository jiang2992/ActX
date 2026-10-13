package com.bin.actx.model.factory.getter;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;

/**
 * Deepseek模型消息获取器
 *
 * @author Bin
 * @since 2026/6/26 11:03
 */
public class DeepseekModelMessageGetter extends ModelMessageGetter {

    @Override
    public String getReasoningContent(AssistantMessage assistantMessage) {
        if (assistantMessage instanceof DeepSeekAssistantMessage deepSeekAssistantMessage) {
            return deepSeekAssistantMessage.getReasoningContent();
        }
        return null;
    }

}
