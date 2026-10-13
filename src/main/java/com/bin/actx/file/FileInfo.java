package com.bin.actx.file;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件信息
 *
 * @author Bin
 * @since 2026/5/18 11:17
 */
@Data
public class FileInfo implements Serializable {

    private String fileName;

    private String filePath;

    private String extension;

    private Boolean isDirectory;

    private Long size;

    private LocalDateTime creationTime;

    private LocalDateTime lastModifiedTime;

}
