package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.SandboxPoolStateEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 沙箱池状态DAO接口
 *
 * @author Bin
 * @since 2026/7/28
 */
@RegisterBeanMapper(SandboxPoolStateEntity.class)
public interface SandboxPoolStateDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS sandbox_pool_state (" +
            "id TEXT PRIMARY KEY, " +
            "pool_name TEXT NOT NULL, " +
            "sandbox_id TEXT NOT NULL, " +
            "expire_time TIMESTAMP NOT NULL, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createTable();

    /**
     * 新增
     *
     * @param entity 数据对象
     */
    @SqlUpdate("INSERT INTO sandbox_pool_state (id, pool_name, sandbox_id, expire_time, create_time) " +
            "VALUES (:id, :poolName, :sandboxId, :expireTime, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "pool_name = :poolName, sandbox_id = :sandboxId, expire_time = :expireTime, update_time = CURRENT_TIMESTAMP")
    void insert(@BindBean SandboxPoolStateEntity entity);

    /**
     * 根据池名称查询所有数据
     *
     * @param poolName 池名称
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM sandbox_pool_state WHERE pool_name = :poolName ORDER BY create_time DESC")
    List<SandboxPoolStateEntity> findByPoolName(@Bind("poolName") String poolName);

    /**
     * 根据池名称查询所有未过期数据
     *
     * @param poolName 池名称
     * @param checkTime 检查时间点
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM sandbox_pool_state WHERE pool_name = :poolName AND expire_time > :checkTime ORDER BY expire_time ASC")
    List<SandboxPoolStateEntity> findNotExpiredByPoolName(@Bind("poolName") String poolName, @Bind("checkTime") LocalDateTime checkTime);

    /**
     * 根据池名称查询所有已过期数据
     *
     * @param poolName 池名称
     * @param checkTime 检查时间点
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM sandbox_pool_state WHERE pool_name = :poolName AND expire_time <= :checkTime ORDER BY expire_time ASC")
    List<SandboxPoolStateEntity> findExpiredByPoolName(@Bind("poolName") String poolName, @Bind("checkTime") LocalDateTime checkTime);

    /**
     * 根据ID更新过期时间
     *
     * @param id         沙箱池状态ID
     * @param expireTime 新的过期时间
     */
    @SqlUpdate("UPDATE sandbox_pool_state SET expire_time = :expireTime, update_time = CURRENT_TIMESTAMP WHERE id = :id")
    void updateExpireTimeById(@Bind("id") String id, @Bind("expireTime") LocalDateTime expireTime);

    /**
     * 根据ID列表批量删除数据
     *
     * @param ids ID列表
     */
    @SqlUpdate("DELETE FROM sandbox_pool_state WHERE id IN (<ids>)")
    void deleteByIds(@BindList("ids") Collection<String> ids);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM sandbox_pool_state WHERE id = :id")
    SandboxPoolStateEntity findById(@Bind("id") String id);

    /**
     * 根据池名称和沙箱ID查询
     *
     * @param poolName  池名称
     * @param sandboxId 沙箱ID
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM sandbox_pool_state WHERE sandbox_id = :sandboxId AND pool_name = :poolName")
    SandboxPoolStateEntity findBySandboxId(@Bind("poolName") String poolName, @Bind("sandboxId") String sandboxId);

    /**
     * 根据池名称和沙箱ID列表查询
     *
     * @param poolName   池名称
     * @param sandboxIds 沙箱ID列表
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM sandbox_pool_state WHERE sandbox_id IN (<sandboxIds>) AND pool_name = :poolName ORDER BY expire_time ASC")
    List<SandboxPoolStateEntity> findBySandboxIds(@Bind("poolName") String poolName, @BindList("sandboxIds") Collection<String> sandboxIds);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM sandbox_pool_state ORDER BY expire_time ASC")
    List<SandboxPoolStateEntity> findAll();

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM sandbox_pool_state")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM sandbox_pool_state")
    void clear();
}