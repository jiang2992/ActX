package com.bin.actx.storage.local.repository;

import com.bin.actx.exception.SectionCreateException;
import com.bin.actx.file.AttachmentInfo;
import com.bin.actx.file.AttachmentManager;
import com.bin.actx.file.FileManager;
import com.bin.actx.message.AnswerSessionMessage;
import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.message.SessionMessage;
import com.bin.actx.message.SessionMessageCollection;
import com.bin.actx.message.section.AnswerSection;
import com.bin.actx.message.section.ErrorAnswerSection;
import com.bin.actx.message.section.MessageAnswerSection;
import com.bin.actx.message.section.StopAnswerSection;
import com.bin.actx.message.section.TellAnswerSection;
import com.bin.actx.message.section.ThinkingAnswerSection;
import com.bin.actx.message.section.ToolAnswerSection;
import com.bin.actx.storage.local.dao.SessionDao;
import com.bin.actx.storage.local.dao.SessionMessageDao;
import com.bin.actx.storage.local.dao.SessionMessageSectionDao;
import com.bin.actx.storage.local.entity.SessionEntity;
import com.bin.actx.storage.local.entity.SessionMessageEntity;
import com.bin.actx.storage.local.entity.SessionMessageSectionEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 会话消息存储
 *
 * @author Bin
 * @since 2026/5/13 16:39
 */
@Component
public class SessionMessageRepository {

    public static final Map<String, Class<? extends AnswerSection>> PRESET_SECTION_MAP = Map.of(
            "answer", MessageAnswerSection.class,
            "thinking", ThinkingAnswerSection.class,
            "tool_call", ToolAnswerSection.class,
            "error", ErrorAnswerSection.class,
            "stop", StopAnswerSection.class,
            "tell", TellAnswerSection.class
    );

    @Autowired
    private SessionMessageDao sessionMessageDao;

    @Autowired
    private SessionMessageSectionDao sessionMessageSectionDao;

    @Autowired
    private SessionDao sessionDao;

    @Autowired
    private AttachmentManager attachmentManager;

    @Autowired
    private FileManager fileManager;

    public SessionMessageCollection getSessionMessages(String sessionId) {
        // 从数据库中获取该会话所有消息
        List<SessionMessageEntity> messageEntityList = sessionMessageDao.findBySessionId(sessionId);
        if (messageEntityList.isEmpty()) {
            return new SessionMessageCollection(sessionId);
        }

        // 从数据库中获取该会话所有消息判断（应答消息）
        List<SessionMessageSectionEntity> sectionEntityList = sessionMessageSectionDao.findBySessionId(sessionId);
        Map<String, List<SessionMessageSectionEntity>> sectionMap = sectionEntityList.stream().collect(
                Collectors.groupingBy(SessionMessageSectionEntity::getMessageId));

        SessionMessageCollection sessionMessageCollection = new SessionMessageCollection(sessionId);
        for (SessionMessageEntity messageEntity : messageEntityList) {

            // 处理用户消息
            if ("user".equals(messageEntity.getType())) {
                List<String> attachments =
                        messageEntity.getAttachments() != null ? Arrays.asList(messageEntity.getAttachments().split(",")) : null;
                RequestSessionMessage requestSessionMessage = new RequestSessionMessage(messageEntity.getContent());
                if (attachments != null && !attachments.isEmpty()) {
                    List<AttachmentInfo> attachmentInfoList = attachments.stream().map(attachmentManager::getAttachment)
                            .filter(Objects::nonNull)
                            .toList();
                    if (!attachmentInfoList.isEmpty()) {
                        requestSessionMessage.setAttachments(attachmentInfoList);
                    }
                }
                sessionMessageCollection.addMessage(requestSessionMessage);
                continue;
            }

            // 处理应答消息
            List<SessionMessageSectionEntity> answerSectionEntityList = sectionMap.get(messageEntity.getId());
            if (answerSectionEntityList == null) {
                answerSectionEntityList = new ArrayList<>();
            }
            List<AnswerSection> sections = new ArrayList<>(answerSectionEntityList.size());
            for (SessionMessageSectionEntity sectionEntity : answerSectionEntityList) {

                // 适配动态扩展的类型，Type格式为：type:className
                String type = sectionEntity.getType();
                Class<? extends AnswerSection> sectionClass = PRESET_SECTION_MAP.get(type);
                if (sectionClass == null) {
                    String[] split = type.split(":");
                    if (split.length != 2) {
                        throw new IllegalArgumentException("Unknown section: " + type);
                    }
                    try {
                        Class<?> aClass = Class.forName(split[1]);
                        if (!AnswerSection.class.isAssignableFrom(aClass)) {
                            throw new IllegalArgumentException("Unknown section: " + type);
                        }
                        sectionClass = (Class<? extends AnswerSection>) aClass;
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException("Section class not found: " + split[1], e);
                    }
                }

                try {
                    AnswerSection section = sectionClass.getDeclaredConstructor().newInstance();
                    section.fromText(sectionEntity.getContent());
                    sections.add(section);
                } catch (Exception e) {
                    throw new SectionCreateException("Failed to instantiate section: " + sectionClass.getName(), e);
                }

            }

            AnswerSessionMessage answerSessionMessage = new AnswerSessionMessage();
            answerSessionMessage.setContent(messageEntity.getContent());
            answerSessionMessage.setSections(sections);
            sessionMessageCollection.addMessage(answerSessionMessage);
        }

        return sessionMessageCollection;
    }

    @Transaction
    public void addMessage(String sessionId, SessionMessage... sessionMessages) {
        SessionEntity sessionEntity = sessionDao.findById(sessionId);
        if (sessionEntity == null) {
            return;
        }

        List<SessionMessageEntity> messageEntityList = new ArrayList<>(sessionMessages.length);
        List<SessionMessageSectionEntity> sectionEntityList = new ArrayList<>(sessionMessages.length);
        int nextMessageSort = sessionEntity.getLastMessageSort() + 10;
        for (SessionMessage sessionMessage : sessionMessages) {
            SessionMessageEntity messageEntity = new SessionMessageEntity();
            messageEntity.setId(IdUtils.uuid());
            messageEntity.setSessionId(sessionId);
            messageEntity.setType(sessionMessage.getType());
            messageEntity.setContent(sessionMessage.getContent());
            messageEntity.setSort(nextMessageSort);

            sessionEntity.setLastMessageSort(nextMessageSort);

            nextMessageSort += 10;

            // 保存请求消息
            if (sessionMessage instanceof RequestSessionMessage requestSessionMessage) {
                messageEntity.setAttachments(requestSessionMessage.getAttachments() != null ?
                        requestSessionMessage.getAttachments().stream().map(AttachmentInfo::getId).collect(Collectors.joining(",")) : null);
                messageEntityList.add(messageEntity);
                continue;
            }

            // 应答消息处理
            AnswerSessionMessage answerSessionMessage = (AnswerSessionMessage) sessionMessage;
            sessionEntity.setLastTokenTotal(answerSessionMessage.getTokenTotal());
            messageEntity.setTokenTotal(answerSessionMessage.getTokenTotal());
            messageEntityList.add(messageEntity);
            if (answerSessionMessage.getSections() == null) {
                continue;
            }

            // 应答消息片段处理
            int nextSectionSort = 10;
            for (AnswerSection section : answerSessionMessage.getSections()) {
                String type = PRESET_SECTION_MAP.containsKey(section.getType()) ? section.getType()
                        : (section.getType() + ":" + section.getClass().getName());
                SessionMessageSectionEntity messageSectionEntity = new SessionMessageSectionEntity();
                messageSectionEntity.setId(IdUtils.uuid());
                messageSectionEntity.setSessionId(sessionId);
                messageSectionEntity.setMessageId(messageEntity.getId());
                messageSectionEntity.setSort(nextSectionSort);
                messageSectionEntity.setType(type);
                messageSectionEntity.setContent(section.toText());
                sectionEntityList.add(messageSectionEntity);
                nextSectionSort += 10;
            }
        }

        sessionEntity.setLastMessageTime(LocalDateTime.now());
        sessionDao.upsert(sessionEntity);
        sessionMessageDao.upsertBatch(messageEntityList);
        sessionMessageSectionDao.upsertBatch(sectionEntityList);
    }

}
