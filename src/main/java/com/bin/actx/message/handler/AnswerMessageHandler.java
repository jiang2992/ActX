package com.bin.actx.message.handler;

import com.bin.actx.channel.ChatMessageChannel;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.context.ResponseContextManager;
import com.bin.actx.message.AnswerSessionMessage;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.model.factory.getter.ModelMessageGetter;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 应答消息处理器默认实现
 *
 * @author Bin
 * @since 2026/5/12 11:40
 */
@Slf4j
@Component
public class AnswerMessageHandler {

    @Autowired
    private ChatModelManager chatModelManager;
    @Autowired
    private ResponseContextManager responseContextManager;

    public AnswerSessionMessage handle(ResponseContext responseContext, ChatClientResponse chatClientResponse) {
        ChatMessageChannel messageChannel = responseContext.getMessageChannel();

        ChatResponse response = chatClientResponse.chatResponse();
        String content = null;
        if (response != null) {
            ModelMessageGetter modelMessageGetter = chatModelManager.getModelMessageGetter(
                    responseContext.getRequestContext().getAgentInstance().getConfig().getModelId());
            content = modelMessageGetter.getContent(response.getResult().getOutput());
            Usage usage = response.getMetadata().getUsage();
            if (usage != null) {
                responseContext.setUsage(usage);
            }
        }

        messageChannel.onAnswer(content);

        AnswerSessionMessage answerSessionMessage = responseContext.complete();
        messageChannel.onEnd(answerSessionMessage);
        responseContextManager.remove(responseContext.getId());

        return answerSessionMessage;
    }

    public AnswerSessionMessage handle(ResponseContext responseContext, Throwable throwable) {
        ChatMessageChannel messageChannel = responseContext.getMessageChannel();

        responseContext.setThrowable(throwable);

        messageChannel.onError(throwable);

        AnswerSessionMessage answerSessionMessage = responseContext.complete();
        messageChannel.onEnd(answerSessionMessage);
        responseContextManager.remove(responseContext.getId());

        return answerSessionMessage;
    }

    public void streamHandle(ResponseContext responseContext, Flux<ChatClientResponse> responseFlux) {
        ChatMessageChannel messageChannel = responseContext.getMessageChannel();

        responseFlux
                .takeWhile(response -> responseContext.getStopInfo() == null)
                .doOnNext(response -> this.streamEventHandle(responseContext, response))
                .doOnError(throwable -> {
                    responseContext.setThrowable(throwable);
                    messageChannel.onError(throwable);
                })
                .doFinally(signalType -> {
                    if (responseContext.getStopInfo() != null) {
                        messageChannel.onStop(responseContext.getStopInfo());
                    }
                    AnswerSessionMessage answerSessionMessage = responseContext.complete();
                    messageChannel.onEnd(answerSessionMessage);
                    responseContextManager.remove(responseContext.getId());
                })
                .subscribe();
    }


    private void streamEventHandle(ResponseContext responseContext, ChatClientResponse chatClientResponse) {
        ChatMessageChannel chatMessageChannel = responseContext.getMessageChannel();

        ChatResponse response = chatClientResponse.chatResponse();
        if (response == null) {
            return;
        }

        Usage usage = response.getMetadata().getUsage();
        if (usage != null && !(usage instanceof EmptyUsage)) {
            responseContext.setUsage(usage);
        }

        ModelMessageGetter modelMessageGetter = chatModelManager.getModelMessageGetter(
                responseContext.getRequestContext().getAgentInstance().getConfig().getModelId());

        Generation gen = response.getResult();
        if (gen == null) {
            return;
        }
        AssistantMessage assistantMessage = gen.getOutput();

        String reasoningContent = modelMessageGetter.getReasoningContent(assistantMessage);
        if (StringUtils.notEmpty(reasoningContent)) {
            chatMessageChannel.onThinkingStream(reasoningContent);
            return;
        }

        String text = modelMessageGetter.getContent(assistantMessage);
        if (StringUtils.notEmpty(text)) {
            chatMessageChannel.onAnswerStream(text);
            return;
        }

    }

}
