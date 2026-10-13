package com.bin.actx.storage.local.entity;

import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 子任务运行时持久化实体
 *
 * @author Bin
 * @since 2026/6/1 11:20
 */
@Data
public class SubTaskRuntimeEntity {

    /**
     * id
     */
    private String id;

    /**
     * 编码(plan.group.subTask.id)
     */
    private String code;

    /**
     * 任务单id
     */
    @ColumnName("task_order_id")
    private String taskOrderId;

    /**
     * 类型（main、lack）
     */
    private String type;

    /**
     * 状态
     */
    private String status;

    /**
     * 描述
     */
    private String description;

    /**
     * 输出
     */
    private String output;

    /**
     * 附加信息
     */
    private String additional;

}
