package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.SessionSummaryEntity;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 会话摘要数据访问对象
 */
@RegisterBeanMapper(SessionSummaryEntity.class)
public interface SessionSummaryDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS session_summary (" +
            "id TEXT PRIMARY KEY, " +
            "session_id TEXT NOT NULL, " +
            "start_index INTEGER NOT NULL, " +
            "end_index INTEGER NOT NULL, " +
            "content TEXT, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createTable();

    /**
     * 插入或更新
     *
     * @param entity 数据对象
     */
    @SqlUpdate("INSERT INTO session_summary (id, session_id, start_index, end_index, content, create_time) " +
            "VALUES (:id, :sessionId, :startIndex, :endIndex, :content, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "session_id = :sessionId, start_index = :startIndex, end_index = :endIndex, " +
            "content = :content, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean SessionSummaryEntity entity);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM session_summary WHERE id = :id")
    SessionSummaryEntity findById(@Bind("id") String id);

    /**
     * 根据会话ID查询
     *
     * @param sessionId 会话ID
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session_summary WHERE session_id = :sessionId ORDER BY end_index ASC")
    List<SessionSummaryEntity> findBySessionId(@Bind("sessionId") String sessionId);

    /**
     * 根据会话ID查询最后一条数据
     *
     * @param sessionId 会话ID
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM session_summary WHERE session_id = :sessionId ORDER BY end_index DESC LIMIT 1")
    SessionSummaryEntity findLastBySessionId(@Bind("sessionId") String sessionId);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session_summary ORDER BY create_time DESC")
    List<SessionSummaryEntity> findAll();

    /**
     * 删除
     *
     * @param id id
     */
    @SqlUpdate("DELETE FROM session_summary WHERE id = :id")
    void delete(@Bind("id") String id);

    /**
     * 根据会话ID删除
     *
     * @param sessionId 会话ID
     */
    @SqlUpdate("DELETE FROM session_summary WHERE session_id = :sessionId")
    void deleteBySessionId(@Bind("sessionId") String sessionId);

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM session_summary")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM session_summary")
    void clear();

}