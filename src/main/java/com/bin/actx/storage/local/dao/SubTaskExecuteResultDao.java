package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.SubTaskExecuteResultEntity;
import java.util.Collection;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 子任务执行结果Dao
 *
 * @author Bin
 * @since 2026/6/23 16:36
 */
@RegisterBeanMapper(SubTaskExecuteResultEntity.class)
public interface SubTaskExecuteResultDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS sub_task_execute_result (" +
            "id TEXT PRIMARY KEY, " +
            "code TEXT NOT NULL, " +
            "task_order_id TEXT NOT NULL, " +
            "status TEXT, " +
            "output TEXT, " +
            "additional TEXT, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createTable();

    /**
     * 插入或更新
     *
     * @param entity 数据对象
     */
    @SqlUpdate("INSERT INTO sub_task_execute_result (id, code, task_order_id, status, output, additional, create_time) " +
            "VALUES (:id, :code, :taskOrderId, :status, :output, :additional, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "code = :code, task_order_id = :taskOrderId, status = :status, output = :output, additional = :additional, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean SubTaskExecuteResultEntity entity);

    /**
     * 插入或更新
     *
     * @param entities 数据对象
     */
    @SqlBatch("INSERT INTO sub_task_execute_result (id, code, task_order_id, status, output, additional, create_time) " +
            "VALUES (:id, :code, :taskOrderId, :status, :output, :additional, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "code = :code, task_order_id = :taskOrderId, status = :status, output = :output, additional = :additional, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean List<SubTaskExecuteResultEntity> entities);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM sub_task_execute_result WHERE id = :id")
    SubTaskExecuteResultEntity findById(@Bind("id") String id);

    /**
     * 根据code查询
     *
     * @param taskOrderId 任务单id
     * @param code        code
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM sub_task_execute_result WHERE task_order_id = :taskOrderId AND code = :code")
    SubTaskExecuteResultEntity findByCode(@Bind("taskOrderId") String taskOrderId, @Bind("code") String code);

    /**
     * 根据任务单ID查询
     *
     * @param taskOrderId 任务单ID
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM sub_task_execute_result WHERE task_order_id = :taskOrderId ORDER BY create_time ASC")
    List<SubTaskExecuteResultEntity> findByTaskOrderId(@Bind("taskOrderId") String taskOrderId);

    /**
     * 根据任务单ID和编码查询
     *
     * @param taskOrderId 任务单ID
     * @param codes       编码列表
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM sub_task_execute_result WHERE task_order_id = :taskOrderId AND code IN (<codes>) ORDER BY create_time ASC")
    List<SubTaskExecuteResultEntity> findByTaskOrderIdAndCodes(@Bind("taskOrderId") String taskOrderId,
            @BindList("codes") Collection<String> codes);

    /**
     * 根据任务单ID查询已完成的
     *
     * @param taskOrderId 任务单ID
     * @return 数据列表
     */
    @SqlQuery("SELECT id, code FROM sub_task_execute_result WHERE status = 'completed' AND task_order_id = :taskOrderId")
    List<SubTaskExecuteResultEntity> findCompletedCodeByTaskOrderId(@Bind("taskOrderId") String taskOrderId);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM sub_task_execute_result ORDER BY create_time DESC")
    List<SubTaskExecuteResultEntity> findAll();

    /**
     * 删除
     *
     * @param id id
     */
    @SqlUpdate("DELETE FROM sub_task_execute_result WHERE id = :id")
    void delete(@Bind("id") String id);

    /**
     * 根据任务单ID删除
     *
     * @param taskOrderId 任务单ID
     */
    @SqlUpdate("DELETE FROM sub_task_execute_result WHERE task_order_id = :taskOrderId")
    void deleteByTaskOrderId(@Bind("taskOrderId") String taskOrderId);

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM sub_task_execute_result")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM sub_task_execute_result")
    void clear();

}
