package com.bin.actx.plugin.core.memory;

import com.bin.actx.channel.MessageChannel;
import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.message.AnswerSessionMessageBuilder;
import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.plugin.hook.ResponseHook;
import com.bin.actx.service.request.RequestMessagePayload;

/**
 * 长期记忆插件
 *
 * @author Bin
 * @since 2026/6/10 17:09
 */
public class EternalMemoryPlugin implements RequestHook, ResponseHook {

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {

    }

    @Override
    public MessageChannel responseProc(ResponseContext responseContext, RequestSessionMessage requestSessionMessage,
            AnswerSessionMessageBuilder answerSessionMessageBuilder) {
        return null;
    }

}
