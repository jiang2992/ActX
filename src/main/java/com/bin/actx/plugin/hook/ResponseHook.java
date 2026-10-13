package com.bin.actx.plugin.hook;

import com.bin.actx.channel.MessageChannel;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.message.AnswerSessionMessageBuilder;
import com.bin.actx.message.RequestSessionMessage;

public interface ResponseHook {

    /**
     * LLM响应开始之前触发，允许返回一个消息通道或返回null，如果返回消息通道，则会在LLM响应时触发消息通道中的对应函数
     *
     * @param responseContext             响应上下文
     * @param requestSessionMessage       请求会话消息
     * @param answerSessionMessageBuilder 回答会话消息生成器
     * @return 消息通道或null
     */
    default MessageChannel responseProc(ResponseContext responseContext, RequestSessionMessage requestSessionMessage,
            AnswerSessionMessageBuilder answerSessionMessageBuilder) {
        return null;
    }

}
