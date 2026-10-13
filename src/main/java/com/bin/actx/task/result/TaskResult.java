package com.bin.actx.task.result;

import lombok.Data;

/**
 * 任务执行结果
 *
 * @author Bin
 * @since 2026/6/1 16:58
 */
@Data
public abstract class TaskResult {

    private String id;

    private String output;

}
