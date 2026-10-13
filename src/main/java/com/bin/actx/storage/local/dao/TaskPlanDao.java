package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.TaskPlanEntity;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 任务计划数据访问对象
 */
@RegisterBeanMapper(TaskPlanEntity.class)
public interface TaskPlanDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS task_plan (" +
            "id TEXT PRIMARY KEY, " +
            "task_summary TEXT, " +
            "task_description TEXT, " +
            "task_groups_json TEXT, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createTable();

    /**
     * 插入或更新
     *
     * @param entity 数据对象
     */
    @SqlUpdate("INSERT INTO task_plan (id, task_summary, task_description, task_groups_json, create_time) " +
            "VALUES (:id, :taskSummary, :taskDescription, :taskGroupsJson, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "task_summary = :taskSummary, task_description = :taskDescription, " +
            "task_groups_json = :taskGroupsJson, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean TaskPlanEntity entity);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM task_plan WHERE id = :id")
    TaskPlanEntity findById(@Bind("id") String id);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM task_plan ORDER BY create_time DESC")
    List<TaskPlanEntity> findAll();

    /**
     * 删除
     *
     * @param id id
     */
    @SqlUpdate("DELETE FROM task_plan WHERE id = :id")
    void delete(@Bind("id") String id);

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM task_plan")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM task_plan")
    void clear();

}
