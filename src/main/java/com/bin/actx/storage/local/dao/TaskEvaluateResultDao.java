package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.TaskEvaluateResultEntity;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 任务评估结果数据访问对象
 */
@RegisterBeanMapper(TaskEvaluateResultEntity.class)
public interface TaskEvaluateResultDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS task_evaluate_result (" +
            "id TEXT PRIMARY KEY, " +
            "task_summary TEXT, " +
            "steps_and_branches INTEGER, " +
            "dependencies INTEGER, " +
            "knowledge_breadth INTEGER, " +
            "uncertainty INTEGER, " +
            "execution_risk INTEGER, " +
            "total_score INTEGER, " +
            "overall_complexity_level TEXT, " +
            "requires_planning INTEGER, " +
            "key_risk_factors TEXT, " +
            "suggestion TEXT, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createTable();

    /**
     * 插入或更新
     *
     * @param entity 数据对象
     */
    @SqlUpdate("INSERT INTO task_evaluate_result (id, task_summary, steps_and_branches, dependencies, " +
            "knowledge_breadth, uncertainty, execution_risk, total_score, overall_complexity_level, " +
            "requires_planning, key_risk_factors, suggestion, create_time) " +
            "VALUES (:id, :taskSummary, :stepsAndBranches, :dependencies, " +
            ":knowledgeBreadth, :uncertainty, :executionRisk, :totalScore, :overallComplexityLevel, " +
            ":requiresPlanning, :keyRiskFactors, :suggestion, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "task_summary = :taskSummary, steps_and_branches = :stepsAndBranches, " +
            "dependencies = :dependencies, knowledge_breadth = :knowledgeBreadth, " +
            "uncertainty = :uncertainty, execution_risk = :executionRisk, " +
            "total_score = :totalScore, overall_complexity_level = :overallComplexityLevel, " +
            "requires_planning = :requiresPlanning, key_risk_factors = :keyRiskFactors, " +
            "suggestion = :suggestion, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean TaskEvaluateResultEntity entity);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM task_evaluate_result WHERE id = :id")
    TaskEvaluateResultEntity findById(@Bind("id") String id);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM task_evaluate_result ORDER BY create_time DESC")
    List<TaskEvaluateResultEntity> findAll();

    /**
     * 删除
     *
     * @param id id
     */
    @SqlUpdate("DELETE FROM task_evaluate_result WHERE id = :id")
    void delete(@Bind("id") String id);

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM task_evaluate_result")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM task_evaluate_result")
    void clear();

}
