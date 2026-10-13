package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.SessionEntity;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 会话DAO接口
 *
 * @author Bin
 * @since 2026/5/28
 */
@RegisterBeanMapper(SessionEntity.class)
public interface SessionDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS session (" +
            "id TEXT PRIMARY KEY, " +
            "user_id TEXT NULL, " +
            "name TEXT NULL, " +
            "type TEXT NULL, " +
            "last_token_total INTEGER NULL, " +
            "last_message_sort INTEGER NULL, " +
            "last_message_time TIMESTAMP NULL, " +
            "del_flag INTEGER DEFAULT 0, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createTable();

    /**
     * 插入或更新
     *
     * @param entity 数据对象
     */
    @SqlUpdate("INSERT INTO session (id, user_id, name, type, last_token_total, last_message_sort, last_message_time, del_flag, create_time) " +
            "VALUES (:id, :userId, :name, :type, :lastTokenTotal, :lastMessageSort, :lastMessageTime, 0, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "name = :name, user_id = :userId, type = :type, last_token_total = :lastTokenTotal, last_message_sort = :lastMessageSort, " +
            "last_message_time = :lastMessageTime, del_flag = 0, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean SessionEntity entity);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM session WHERE id = :id AND del_flag = 0")
    SessionEntity findById(@Bind("id") String id);

    /**
     * 根据用户id查询
     *
     * @param userId 用户id
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session WHERE user_id = :userId AND del_flag = 0 ORDER BY create_time DESC")
    List<SessionEntity> findByUserId(@Bind("userId") String userId);

    /**
     * 根据用户id和类型查询
     *
     * @param userId 用户id
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session WHERE user_id = :userId AND type = :type AND del_flag = 0 ORDER BY create_time DESC")
    List<SessionEntity> findByUserId(@Bind("userId") String userId, @Bind("type") String type);

    /**
     * 根据用户id和类型查询最新的会话
     *
     * @param userId 用户id
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session WHERE user_id = :userId AND type = :type AND del_flag = 0 ORDER BY create_time DESC LIMIT 1")
    SessionEntity findLatestByUserId(@Bind("userId") String userId, @Bind("type") String type);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session WHERE del_flag = 0 ORDER BY create_time DESC")
    List<SessionEntity> findAll();

    /**
     * 根据名称搜索
     *
     * @param keyword 关键词
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session WHERE name LIKE :keyword AND del_flag = 0 ORDER BY create_time DESC")
    List<SessionEntity> searchByName(@Bind("keyword") String keyword);

    /**
     * 逻辑删除（设置del_flag=1）
     *
     * @param id id
     */
    @SqlUpdate("UPDATE session SET del_flag = 1, update_time = CURRENT_TIMESTAMP WHERE id = :id")
    void softDelete(@Bind("id") String id);

    /**
     * 物理删除
     *
     * @param id id
     */
    @SqlUpdate("DELETE FROM session WHERE id = :id")
    void delete(@Bind("id") String id);

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM session WHERE del_flag = 0")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM session")
    void clear();

}
