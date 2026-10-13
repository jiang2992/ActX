package com.bin.actx.storage.local.entity;

import lombok.Data;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * 文件信息持久化实体
 *
 * @author Bin
 * @since 2026/6/1 11:20
 */
@Data
public class AttachmentEntity {

    @ColumnName("id")
    private String id;

    @ColumnName("file_name")
    private String fileName;

    @ColumnName("path")
    private String path;

    @ColumnName("extension")
    private String extension;

    @ColumnName("size")
    private Long size;

    @ColumnName("is_text_file")
    private Boolean isTextFile;

}
