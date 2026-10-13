package com.bin.actx.task.model;

import java.io.Serializable;
import lombok.Data;

/**
 * 任务信息<br/> 任务 -> 子任务组 -> 子任务<br/> 任务：任务信息及进度状态等 <br/> 子任务组：任务的子任务组，包含多个子任务，通常用于规划任务的执行顺序<br/> 子任务：任务的具体执行单元
 *
 * @author Bin
 * @since 2026/5/20 17:23
 */
@Data
public class TaskOrder implements Serializable {

    private String id;

    private TaskPlan plan;

    private String status;

    private String output;

}
