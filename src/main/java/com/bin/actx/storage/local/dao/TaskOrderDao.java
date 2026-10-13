package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.TaskOrderEntity;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 任务单数据访问对象
 */
@RegisterBeanMapper(TaskOrderEntity.class)
public interface TaskOrderDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS task_order (" +
            "id TEXT PRIMARY KEY, " +
            "plan_id TEXT NOT NULL, " +
            "status TEXT, " +
            "output TEXT, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createTable();

    /**
     * 插入或更新
     *
     * @param entity 数据对象
     */
    @SqlUpdate("INSERT INTO task_order (id, plan_id, status, output, create_time) " +
            "VALUES (:id, :planId, :status, :output, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "plan_id = :planId, status = :status, output = :output, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean TaskOrderEntity entity);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM task_order WHERE id = :id")
    TaskOrderEntity findById(@Bind("id") String id);

    /**
     * 根据计划ID查询
     *
     * @param planId 计划ID
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM task_order WHERE plan_id = :planId ORDER BY create_time ASC")
    List<TaskOrderEntity> findByPlanId(@Bind("planId") String planId);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM task_order ORDER BY create_time DESC")
    List<TaskOrderEntity> findAll();

    /**
     * 删除
     *
     * @param id id
     */
    @SqlUpdate("DELETE FROM task_order WHERE id = :id")
    void delete(@Bind("id") String id);

    /**
     * 根据计划ID删除
     *
     * @param planId 计划ID
     */
    @SqlUpdate("DELETE FROM task_order WHERE plan_id = :planId")
    void deleteByPlanId(@Bind("planId") String planId);

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM task_order")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM task_order")
    void clear();

}
