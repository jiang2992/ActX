package com.bin.actx.model.factory.getter;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.zhipuai.ZhiPuAiAssistantMessage;

/**
 * 智谱模型消息获取器
 *
 * @author Bin
 * @since 2026/6/26 11:04
 */
public class ZhipuModelMessageGetter extends ModelMessageGetter {

    @Override
    public String getReasoningContent(AssistantMessage assistantMessage) {
        if (assistantMessage instanceof ZhiPuAiAssistantMessage zhiPuAiAssistantMessage) {
            return zhiPuAiAssistantMessage.getReasoningContent();
        }
        return null;
    }

}
