package com.bin.actx.channel;

import com.bin.actx.channel.event.StreamEvent;
import com.bin.actx.context.ResponseStopInfo;
import com.bin.actx.message.AnswerSessionMessage;

/**
 * 聊天消息通道
 *
 * @author Bin
 * @since 2026/5/27 11:17
 */
public interface ChatMessageChannel extends MessageChannel {

    /**
     * 思考（普通请求）
     *
     * @param content 思考内容（全量）
     */
    default void onThinking(String content) {
    }

    /**
     * 思考（流式请求）
     *
     * @param content 思考内容（多段）
     */
    default void onThinkingStream(String content) {
    }

    /**
     * 应答正文（普通请求）
     *
     * @param content 应答内容（全量）
     */
    default void onAnswer(String content) {
    }

    /**
     * 应答正文（流式请求）
     *
     * @param content 应答内容（多段）
     */
    default void onAnswerStream(String content) {
    }

    /**
     * 开始工具调用
     *
     * @param toolCallPayload 工具调用信息
     */
    default void onToolCalling(StreamEvent.ToolCallPayload toolCallPayload) {
    }

    /**
     * 工具调用完成
     *
     * @param toolResultPayload 工具调用结果信息
     */
    default void onToolCallingResult(StreamEvent.ToolResultPayload toolResultPayload) {
    }

    /**
     * 提示信息（由部分插件发起、非LLM）
     *
     * @param content 提示信息内容
     */
    default void onTell(String content) {
    }

    /**
     * 用户主动停止
     */
    default void onStop(ResponseStopInfo stopInfo) {
    }

    /**
     * 执行过程发生错误
     *
     * @param throwable 错误信息
     */
    default void onError(Throwable throwable) {
    }

    /**
     * 消息通道结束，onError、onStop 后也会调用
     *
     * @param answerSessionMessage 过程中累计的所有应答消息
     */
    default void onEnd(AnswerSessionMessage answerSessionMessage) {
    }

}
