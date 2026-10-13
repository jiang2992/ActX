package com.bin.actx.storage.local.entity;

import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 会话消息持久化实体
 *
 * @author Bin
 * @since 2026/6/1 11:20
 */
@Data
public class SessionMessageEntity {

    @ColumnName("id")
    private String id;

    @ColumnName("session_id")
    private String sessionId;

    @ColumnName("type")
    private String type;

    @ColumnName("content")
    private String content;

    @ColumnName("attachments")
    private String attachments;

    @ColumnName("sort")
    private Integer sort;

    @ColumnName("token_total")
    private Integer tokenTotal;

}
