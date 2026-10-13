package com.bin.actx.file;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 文件内容
 *
 * @author Bin
 * @since 2026/5/18 11:17
 */
@Data
public class FileContent implements Serializable {

    private FileInfo fileInfo;

    private Integer lineCount;

    private Integer startLine;

    private Integer endLine;

    private List<String> content;

    private Boolean isTruncated;

    private Boolean isEnd;

}
