package com.bin.actx.file;

import java.io.Serializable;
import lombok.Data;

/**
 * 附件信息
 *
 * @author Bin
 * @since 2026/5/18 11:17
 */
@Data
public class AttachmentInfo implements Serializable {

    private String id;

    private String fileName;

    private String path;

    private String extension;

    private Long size;

    private Boolean isTextFile;

}
