package com.bin.actx.storage.local.entity;

import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 任务计划持久化实体
 *
 * @author Bin
 * @since 2026/6/1 11:20
 */
@Data
public class TaskPlanEntity {

    /**
     * id
     */
    private String id;

    /**
     * 任务概要
     */
    private String taskSummary;

    /**
     * 任务描述
     */
    private String taskDescription;

    /**
     * 任务组json
     */
    @ColumnName("task_groups_json")
    private String taskGroupsJson;


}
