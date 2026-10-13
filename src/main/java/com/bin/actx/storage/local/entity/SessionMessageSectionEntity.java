package com.bin.actx.storage.local.entity;

import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 会话消息片段持久化实体
 *
 * @author Bin
 * @since 2026/6/1 11:20
 */
@Data
public class SessionMessageSectionEntity {

    @ColumnName("id")
    private String id;

    @ColumnName("session_id")
    private String sessionId;

    @ColumnName("message_id")
    private String messageId;

    @ColumnName("sort")
    private Integer sort;

    @ColumnName("type")
    private String type;

    @ColumnName("content")
    private String content;

}
