package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.SessionMessageSectionEntity;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 会话消息片段数据访问对象
 */
@RegisterBeanMapper(SessionMessageSectionEntity.class)
public interface SessionMessageSectionDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS session_message_section (" +
            "id TEXT PRIMARY KEY, " +
            "session_id TEXT NOT NULL, " +
            "message_id TEXT NOT NULL, " +
            "sort INTEGER NOT NULL, " +
            "type TEXT NOT NULL, " +
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
    @SqlUpdate("INSERT INTO session_message_section (id, session_id, message_id, sort, type, content, create_time) " +
            "VALUES (:id, :sessionId, :messageId, :sort, :type, :content, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "session_id = :sessionId, message_id = :messageId, sort = :sort, " +
            "type = :type, content = :content, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean SessionMessageSectionEntity entity);

    /**
     * 批量插入或更新
     *
     * @param entities 数据对象列表
     */
    @SqlBatch("INSERT INTO session_message_section (id, session_id, message_id, sort, type, content, create_time) " +
            "VALUES (:id, :sessionId, :messageId, :sort, :type, :content, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "session_id = :sessionId, message_id = :messageId, sort = :sort, " +
            "type = :type, content = :content, update_time = CURRENT_TIMESTAMP")
    void upsertBatch(@BindBean List<SessionMessageSectionEntity> entities);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM session_message_section WHERE id = :id")
    SessionMessageSectionEntity findById(@Bind("id") String id);

    /**
     * 根据会话ID查询
     *
     * @param sessionId 会话ID
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session_message_section WHERE session_id = :sessionId ORDER BY sort ASC")
    List<SessionMessageSectionEntity> findBySessionId(@Bind("sessionId") String sessionId);

    /**
     * 根据消息ID查询
     *
     * @param messageId 消息ID
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session_message_section WHERE message_id = :messageId ORDER BY sort ASC")
    List<SessionMessageSectionEntity> findByMessageId(@Bind("messageId") String messageId);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM session_message_section ORDER BY create_time DESC")
    List<SessionMessageSectionEntity> findAll();

    /**
     * 删除
     *
     * @param id id
     */
    @SqlUpdate("DELETE FROM session_message_section WHERE id = :id")
    void delete(@Bind("id") String id);

    /**
     * 根据会话ID删除
     *
     * @param sessionId 会话ID
     */
    @SqlUpdate("DELETE FROM session_message_section WHERE session_id = :sessionId")
    void deleteBySessionId(@Bind("sessionId") String sessionId);

    /**
     * 根据消息ID删除
     *
     * @param messageId 消息ID
     */
    @SqlUpdate("DELETE FROM session_message_section WHERE message_id = :messageId")
    void deleteByMessageId(@Bind("messageId") String messageId);

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM session_message_section")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM session_message_section")
    void clear();

}
