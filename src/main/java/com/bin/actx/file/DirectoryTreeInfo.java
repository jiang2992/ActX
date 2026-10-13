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
public class DirectoryTreeInfo implements Serializable {

    private List<FileInfo> projectList;

}
