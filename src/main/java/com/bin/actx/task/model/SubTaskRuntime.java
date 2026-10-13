package com.bin.actx.task.model;

import java.util.List;
import lombok.Data;

@Data
public class SubTaskRuntime {

    /**
     * 运行时id
     */
    private String id;

    /**
     * 任务Code
     */
    private String code;

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

    /**
     * 分支
     */
    private List<Branch> branches;

    /**
     * 任务信息
     */
    private TaskPlan.SubTask taskInfo;

    public boolean hasBranch() {
        return branches != null && !branches.isEmpty();
    }

    @Data
    public static class Branch {

        /**
         * 运行时id
         */
        private String id;

        /**
         * 类型
         */
        private String type;

        /**
         * 描述
         */
        private String description;

        /**
         * 状态
         */
        private String status;

        /**
         * 输出
         */
        private String output;

        /**
         * 附加信息
         */
        private String additional;

    }

}
