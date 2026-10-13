package com.bin.actx.storage.local.entity;

import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 任务单持久化实体
 *
 * @author Bin
 * @since 2026/6/1 11:20
 */
@Data
public class TaskOrderEntity {

    /**
     * id
     */
    private String id;

    /**
     * 计划id
     */
    @ColumnName("plan_id")
    private String planId;

    /**
     * 状态
     */
    private String status;

    /**
     * 任务输出
     */
    private String output;

}
