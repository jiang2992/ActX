package com.bin.actx.storage.local.dao;

import com.bin.actx.storage.local.entity.AttachmentEntity;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * 附件数据访问对象
 */
@RegisterBeanMapper(AttachmentEntity.class)
public interface AttachmentDao {

    /**
     * 创建表
     */
    @SqlUpdate("CREATE TABLE IF NOT EXISTS attachment (" +
            "id TEXT PRIMARY KEY, " +
            "file_name TEXT NOT NULL, " +
            "path TEXT NOT NULL, " +
            "extension TEXT, " +
            "size INTEGER, " +
            "is_text_file BOOLEAN, " +
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")")
    void createTable();

    /**
     * 插入或更新
     *
     * @param entity 数据对象
     */
    @SqlUpdate("INSERT INTO attachment (id, file_name, path, extension, size, is_text_file, create_time) " +
            "VALUES (:id, :fileName, :path, :extension, :size, :isTextFile, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "file_name = :fileName, path = :path, extension = :extension, " +
            "size = :size, is_text_file = :isTextFile, update_time = CURRENT_TIMESTAMP")
    void upsert(@BindBean AttachmentEntity entity);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return 数据对象，不存在返回null
     */
    @SqlQuery("SELECT * FROM attachment WHERE id = :id")
    AttachmentEntity findById(@Bind("id") String id);

    /**
     * 根据文件名查询
     *
     * @param fileName 文件名
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM attachment WHERE file_name = :fileName ORDER BY create_time DESC")
    List<AttachmentEntity> findByFileName(@Bind("fileName") String fileName);

    /**
     * 根据扩展名查询
     *
     * @param extension 扩展名
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM attachment WHERE extension = :extension ORDER BY create_time DESC")
    List<AttachmentEntity> findByExtension(@Bind("extension") String extension);

    /**
     * 查询所有
     *
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM attachment ORDER BY create_time DESC")
    List<AttachmentEntity> findAll();

    /**
     * 根据文件名搜索
     *
     * @param keyword 关键词
     * @return 数据列表
     */
    @SqlQuery("SELECT * FROM attachment WHERE file_name LIKE :keyword ORDER BY create_time DESC")
    List<AttachmentEntity> searchByFileName(@Bind("keyword") String keyword);

    /**
     * 删除
     *
     * @param id id
     */
    @SqlUpdate("DELETE FROM attachment WHERE id = :id")
    void delete(@Bind("id") String id);

    /**
     * 统计数量
     *
     * @return 数量
     */
    @SqlQuery("SELECT COUNT(*) FROM attachment")
    int count();

    /**
     * 清空
     */
    @SqlUpdate("DELETE FROM attachment")
    void clear();

}
