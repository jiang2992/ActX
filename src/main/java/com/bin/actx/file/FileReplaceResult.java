package com.bin.actx.file;

import java.io.Serializable;
import lombok.Data;

/**
 * 文件替换结果
 *
 * @author Bin
 * @since 2026/7/15
 */
@Data
public class FileReplaceResult implements Serializable {

    private FileInfo fileInfo;

    /**
     * 替换的总次数
     */
    private Integer replaceCount;

    /**
     * 受影响的行数
     */
    private Integer affectedLineCount;

}
