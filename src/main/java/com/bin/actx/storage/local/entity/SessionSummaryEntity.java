package com.bin.actx.storage.local.entity;

import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 会话摘要持久化实体
 *
 * @author Bin
 * @since 2026/6/1 11:20
 */
@Data
public class SessionSummaryEntity {

    @ColumnName("id")
    private String id;

    @ColumnName("session_id")
    private String sessionId;

    @ColumnName("start_index")
    private Integer startIndex;

    @ColumnName("end_index")
    private Integer endIndex;

    @ColumnName("content")
    private String content;

}
