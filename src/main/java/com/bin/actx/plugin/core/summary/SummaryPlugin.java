package com.bin.actx.plugin.core.summary;

import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.message.SessionInfo;
import com.bin.actx.message.SessionMessage;
import com.bin.actx.message.SessionSummary;
import com.bin.actx.message.SessionSummary.Record;
import com.bin.actx.model.ChatModelConfig;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.plugin.BasePlugin;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.service.SummaryService;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.storage.local.repository.SessionRepository;
import com.bin.actx.storage.local.repository.SessionSummaryRepository;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 会话总结插件（在有会话的情况下才生效）
 *
 * @author Bin
 * @since 2026/4/22 14:53
 */
public class SummaryPlugin extends BasePlugin implements RequestHook {

    /**
     * 默认摘要阈值
     */
    private static final Double DEFAULT_TOKEN_THRESHOLD_RATIO = 0.6;

    /**
     * 保留的消息数，包括用户消息与应答消息
     */
    private static final Integer RETAIN_MESSAGE_COUNT = 10;

    @Resource
    private SessionSummaryRepository sessionSummaryRepository;

    @Resource
    private ChatModelManager chatModelManager;

    @Resource
    private SummaryService summaryService;

    @Resource
    private SessionRepository sessionRepository;

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        if (requestMessagePayload.getSessionId() == null) {
            super.warnLog("SessionId is null, skip message summary");
            return;
        }

        SessionInfo sessionInfo = sessionRepository.getSession(requestMessagePayload.getSessionId());

        // 获取完整上下文消息，后续的 requestMessagePayload.getContextMessages() 可能会包含上下文信息
        List<SessionMessage> fullContextMessages = requestMessagePayload.getContextMessages();
        if (fullContextMessages.isEmpty() || fullContextMessages.size() < RETAIN_MESSAGE_COUNT) {
            return;
        }

        // 摘要后的上下文消息
        List<SessionMessage> summaryMessageList = null;

        SessionSummary sessionSummary = sessionSummaryRepository.getLastSessionSummary(requestMessagePayload.getSessionId());
        Record lastSummary = sessionSummary.getLastSummary();
        if (lastSummary != null) {
            summaryMessageList = this.getSummaryMessageList(requestMessagePayload, lastSummary);
        }

        // 最后一次请求的Token长度大于阈值则触发摘要
        Integer tokenThreshold = this.getTokenThreshold(requestContext);
        if (sessionInfo.getLastTokenTotal() >= tokenThreshold) {
            super.debugLog("Reach the token threshold. Threshold: %s, Current: %s".formatted(tokenThreshold,
                    sessionInfo.getLastTokenTotal()));

            // 保留的原始消息数不得小于 RETAIN_MESSAGE_COUNT
            if ((summaryMessageList != null ? summaryMessageList.size() - 1 : fullContextMessages.size()) <= RETAIN_MESSAGE_COUNT) {
                super.debugLog("Skip summary because of message count. Retain message count: " + RETAIN_MESSAGE_COUNT);
                return;
            }

            // 获取需要摘要的目标消息列表
            Integer startIndex = 0;
            Integer endIndex = fullContextMessages.size() - RETAIN_MESSAGE_COUNT;
            List<SessionMessage> summaryTargetMessageList;
            if (summaryMessageList != null) {
                summaryTargetMessageList = new ArrayList<>(summaryMessageList.subList(0, summaryMessageList.size() - RETAIN_MESSAGE_COUNT));
            } else {
                summaryTargetMessageList = new ArrayList<>(
                        fullContextMessages.subList(0, fullContextMessages.size() - RETAIN_MESSAGE_COUNT));
            }

            super.infoLog("Start summarizing, message count: " + summaryTargetMessageList.size());

            // 进行摘要并保存记录
            requestContext.setMetadata("summary", true);
            String summaryContent = summaryService.summary(summaryTargetMessageList);
            Record record = new Record(startIndex, endIndex, summaryContent);
            sessionSummary.setLastSummary(record);
            sessionSummaryRepository.saveSessionSummary(sessionSummary);

            super.infoLog("Summary completed");

            summaryMessageList = this.getSummaryMessageList(requestMessagePayload, record);
        }

        if (summaryMessageList != null) {
            requestMessagePayload.setContextMessages(summaryMessageList);
        }
    }

    /**
     * 因为需要发送消息，而 requestFit 中还没有 responseContext，所以只能放到 requestBefore
     *
     * @param requestMessagePayload 请求消息载体
     * @param responseContext       响应上下文
     */
    @Override
    public void requestBefore(RequestMessagePayload requestMessagePayload, ResponseContext responseContext) {
        if (Boolean.TRUE.equals(responseContext.getRequestContext().getMetadata("summary"))) {
            responseContext.getMessageChannel().onTell("部分历史消息已进行摘要总结");
        }
    }

    private List<SessionMessage> getSummaryMessageList(RequestMessagePayload requestMessagePayload, Record lastSummary) {
        List<SessionMessage> contextMessages = requestMessagePayload.getContextMessages();
        List<SessionMessage> messageList = new ArrayList<>(contextMessages.subList(lastSummary.getEndIndex(), contextMessages.size()));
        RequestSessionMessage summaryMessage = new RequestSessionMessage(String.join("\n", Arrays.asList(
                String.format("[Current Summary (%s conversation rounds have been compressed.This message was set by the system.)]",
                        lastSummary.getEndIndex() - lastSummary.getStartIndex()),
                lastSummary.getContent()
        )));
        messageList.add(0, summaryMessage);
        return messageList;
    }

    private Integer getTokenThreshold(RequestContext requestContext) {
        Double tokenThresholdRatio = (Double) requestContext.getPluginMetadata(this).get("tokenThresholdRatio");
        if (tokenThresholdRatio == null) {
            tokenThresholdRatio = DEFAULT_TOKEN_THRESHOLD_RATIO;
        }
        String modelId = requestContext.getAgentInstance().getConfig().getModelId();
        ChatModelConfig chatModelConfig = chatModelManager.getConfig(modelId);
        return (int) (chatModelConfig.getMaxTokens() * tokenThresholdRatio);
    }

}
