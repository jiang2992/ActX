package com.bin.actx.storage.local.entity;

import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 任务评估结果持久化实体
 *
 * @author Bin
 * @since 2026/6/4 11:20
 */
@Data
public class TaskEvaluateResultEntity {

    /**
     * id
     */
    private String id;

    /**
     * 任务概要
     */
    @ColumnName("task_summary")
    private String taskSummary;

    /**
     * 步骤与分支评分 (1-5)
     */
    @ColumnName("steps_and_branches")
    private Integer stepsAndBranches;

    /**
     * 依赖评分 (1-5)
     */
    private Integer dependencies;

    /**
     * 知识广度评分 (1-5)
     */
    @ColumnName("knowledge_breadth")
    private Integer knowledgeBreadth;

    /**
     * 不确定性评分 (1-5)
     */
    private Integer uncertainty;

    /**
     * 执行风险评分 (1-5)
     */
    @ColumnName("execution_risk")
    private Integer executionRisk;

    /**
     * 总分 (5-25)
     */
    @ColumnName("total_score")
    private Integer totalScore;

    /**
     * 整体复杂度等级: Low, Medium, High
     */
    @ColumnName("overall_complexity_level")
    private String overallComplexityLevel;

    /**
     * 是否需要规划
     */
    @ColumnName("requires_planning")
    private Boolean requiresPlanning;

    /**
     * 重要风险因子 (JSON数组)
     */
    @ColumnName("key_risk_factors")
    private String keyRiskFactors;

    /**
     * 建议
     */
    private String suggestion;

}
