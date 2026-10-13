package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.SessionMessageEntity;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 会话消息数据访问对象
 */
@RegisterBeanMapper(SessionMessageEntity.class)
public interface SessionMessageDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS session_message (" +
            "id TEXT PRIMARY KEY, " +
            "session_id TEXT NOT NULL, " +
            "type TEXT NOT NULL, " +
            "content TEXT, " +
            "attachments TEXT, " +
            "sort INTEGER, " +
            "token_total INTEGER, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createTable();

    /**
     * 插入或更新
     *
     * @param entity 数据对象
     */
    @SqlUpdate("INSERT INTO session_message (id, session_id, type, content, attachments, sort, token_total, create_time) " +
            "VALUES (:id, :sessionId, :type, :content, :attachments, :sort, :tokenTotal, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "session_id = :sessionId, type = :type, content = :content, " +
            "attachments = :attachments, sort = :sort, token_total = :tokenTotal, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean SessionMessageEntity entity);

    /**
     * 批量插入或更新
     *
     * @param entities 数据对象列表
     */
    @SqlBatch("INSERT INTO session_message (id, session_id, type, content, attachments, sort, token_total, create_time) " +
            "VALUES (:id, :sessionId, :type, :content, :attachments, :sort, :tokenTotal, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "session_id = :sessionId, type = :type, content = :content, " +
            "attachments = :attachments, sort = :sort, token_total = :tokenTotal, update_time = CURRENT_TIMESTAMP")
    void upsertBatch(@BindBean List<SessionMessageEntity> entities);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM session_message WHERE id = :id")
    SessionMessageEntity findById(@Bind("id") String id);

    /**
     * 根据会话ID查询
     *
     * @param sessionId 会话ID
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session_message WHERE session_id = :sessionId ORDER BY sort ASC, create_time ASC")
    List<SessionMessageEntity> findBySessionId(@Bind("sessionId") String sessionId);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session_message ORDER BY create_time DESC")
    List<SessionMessageEntity> findAll();

    /**
     * 删除
     *
     * @param id id
     */
    @SqlUpdate("DELETE FROM session_message WHERE id = :id")
    void delete(@Bind("id") String id);

    /**
     * 根据会话ID删除
     *
     * @param sessionId 会话ID
     */
    @SqlUpdate("DELETE FROM session_message WHERE session_id = :sessionId")
    void deleteBySessionId(@Bind("sessionId") String sessionId);

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM session_message")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM session_message")
    void clear();

}
