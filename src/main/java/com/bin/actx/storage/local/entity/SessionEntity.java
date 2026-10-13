package com.bin.actx.storage.local.entity;

import java.time.LocalDateTime;
import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 会话信息持久化实体
 *
 * @author Bin
 * @since 2026/6/1 11:21
 */
@Data
public class SessionEntity {

    @ColumnName("id")
    private String id;

    @ColumnName("user_id")
    private String userId;

    @ColumnName("name")
    private String name;

    @ColumnName("type")
    private String type;

    @ColumnName("last_token_total")
    private Integer lastTokenTotal;

    @ColumnName("last_message_sort")
    private Integer lastMessageSort;

    @ColumnName("last_message_time")
    private LocalDateTime lastMessageTime;

    @ColumnName("del_flag")
    private Integer delFlag;

}
