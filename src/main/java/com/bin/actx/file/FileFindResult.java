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
public class FileFindResult implements Serializable {

    private FileInfo fileInfo;

    private Integer scanLineCount;

    /**
     * 总匹配次数
     */
    private Integer matchCount;

    private List<Line> foundLines;

    private Boolean isTruncated;

    private Boolean isEnd;

    @Data
    public static class Line {

        private Integer lineNumber;

        private String lineContent;

        /**
         * 当前行的匹配次数
         */
        private Integer matchCount;

    }

}
