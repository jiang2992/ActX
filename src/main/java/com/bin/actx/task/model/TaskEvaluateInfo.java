package com.bin.actx.task.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class TaskEvaluateInfo implements Serializable {

    /**
     * 任务摘要
     */
    private String taskSummary;

    /**
     * 维度评分
     */
    private DimensionScores dimensionScores;

    /**
     * 总分 (5-25)
     */
    private Integer totalScore;

    /**
     * 整体复杂度等级: Low, Medium, High
     */
    private String overallComplexityLevel;

    /**
     * 是否需要规划
     */
    private Boolean requiresPlanning;

    /**
     * 重要风险因子
     */
    private List<String> keyRiskFactors;

    /**
     * 建议
     */
    private String suggestion;

    @Data
    public static class DimensionScores implements Serializable {

        /**
         * 步骤与分支评分 (1-5)
         */
        private Integer stepsAndBranches;

        /**
         * 依赖评分 (1-5)
         */
        private Integer dependencies;

        /**
         * 知识广度评分 (1-5)
         */
        private Integer knowledgeBreadth;

        /**
         * 不确定性评分 (1-5)
         */
        private Integer uncertainty;

        /**
         * 执行风险评分 (1-5)
         */
        private Integer executionRisk;
    }

}
