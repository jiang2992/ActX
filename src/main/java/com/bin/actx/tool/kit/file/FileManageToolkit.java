package com.bin.actx.tool.kit.file;

import com.bin.actx.exception.FileCopyException;
import com.bin.actx.exception.FileDeleteException;
import com.bin.actx.exception.FileMoveException;
import com.bin.actx.file.DirectoryTreeInfo;
import com.bin.actx.file.FileContent;
import com.bin.actx.file.FileFindResult;
import com.bin.actx.file.FileInfo;
import com.bin.actx.file.FileManager;
import com.bin.actx.file.FileReplaceResult;
import com.bin.actx.tool.ToolStrategy;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.model.ToolCallResult;
import java.io.IOException;
import java.util.List;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 文件相关工具包
 *
 * @author Bin
 * @since 2026/5/19 09:32
 */
@Toolkit(name = "文件管理", description = "对文件进行读写操作", autoDiscover = false)
public class FileManageToolkit {

    public static final List<String> RETRIEVE_ATTACHMENT_TOOLS = List.of("read_visual_file");
    public static final int MAX_FILE_WRITE_LIMIT_SIZE = 10000;

    @Autowired
    private FileManager fileManager;

    @Tool(name = "read_visual_file", description = "读取视觉文件(图片)，读取后会附带在请求的media中（通常在需要识别图片时调用）")
    public ToolCallResult<List<String>> loadVisualFile(
            @ToolParam(description = "路径") List<String> paths
    ) {
        if (paths == null || paths.isEmpty()) {
            return ToolCallResult.of(null, "Paths cannot be empty.");
        }
        paths = paths.stream().map(this::formatPath).toList();
        return ToolCallResult.of(paths);
    }

    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = "write_file", description = "写入文本到文件中（支持指定策略）")
    public ToolCallResult<FileInfo> writeFile(
            @ToolParam(description = "文件路径（必须基于 '/workspace' 目录）") String path,
            @ToolParam(description = "文件存在并且不为空时的处理策略（rename：自动重命名；cover：强行覆盖；stop：停止写入；默认为stop）", required = false) String strategy,
            @ToolParam(description = "文件内容（严格" + MAX_FILE_WRITE_LIMIT_SIZE + "字符以内）", required = false) String content,
            ToolContext toolContext
    ) {
        if (StringUtils.isEmpty(path)) {
            return ToolCallResult.of(null, "Param 'path' cannot be empty.");
        }
        if (content != null && content.length() > (MAX_FILE_WRITE_LIMIT_SIZE * 1.5)) {
            return ToolCallResult.of(null, "Content length cannot exceed %s characters.".formatted(MAX_FILE_WRITE_LIMIT_SIZE));
        }
        if (strategy == null) {
            strategy = "stop";
        }
        path = formatPath(path);
        FileInfo fileInfo = fileManager.writeFileContent(path, strategy, content);
        return ToolCallResult.of(fileInfo, "success");
    }

    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = "patch_file", description = "修补文件中的文本内容")
    public ToolCallResult<FileInfo> patchFile(
            @ToolParam(description = "文件路径（必须基于 '/workspace' 目录）") String path,
            @ToolParam(description = "写入的开始行") Integer startLine,
            @ToolParam(description = "覆盖的行数（默认为0，表示不做任何覆盖，直接从startLine插入内容；如果想要覆盖整个文件，可以传入一个足够大的数值来实现）", required = false) Integer coverLines,
            @ToolParam(description = "文件内容（严格" + MAX_FILE_WRITE_LIMIT_SIZE
                    + "字符以内，不传的话就是单纯覆盖删除指定行）", required = false) String content,
            ToolContext toolContext
    ) {
        if (StringUtils.isEmpty(path)) {
            return ToolCallResult.of(null, "Param 'path' cannot be empty.");
        }
        if (content != null && content.length() > (MAX_FILE_WRITE_LIMIT_SIZE * 1.5)) {
            return ToolCallResult.of(null, "Content length cannot exceed %s characters.".formatted(MAX_FILE_WRITE_LIMIT_SIZE));
        }
        if (startLine < 1) {
            return ToolCallResult.of(null, "Start line cannot be less than 1.");
        }
        if (coverLines == null || coverLines < 0) {
            coverLines = 0;
        }
        path = formatPath(path);
        FileInfo fileInfo = fileManager.patchFileContent(path, startLine, coverLines, content);
        return ToolCallResult.of(fileInfo, "success");
    }

    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = "append_file", description = "追加文本内容到文件尾部")
    public ToolCallResult<FileInfo> appendFile(
            @ToolParam(description = "文件路径（必须基于 '/workspace' 目录）") String path,
            @ToolParam(description = "文件内容（严格" + MAX_FILE_WRITE_LIMIT_SIZE + "字符以内）") String content,
            ToolContext toolContext
    ) {
        if (StringUtils.isEmpty(path)) {
            return ToolCallResult.of(null, "Param 'path' cannot be empty.");
        }
        if (content.length() > (MAX_FILE_WRITE_LIMIT_SIZE * 1.5)) {
            return ToolCallResult.of(null, "Content length cannot exceed %s characters.".formatted(MAX_FILE_WRITE_LIMIT_SIZE));
        }
        path = formatPath(path);
        FileInfo fileInfo = fileManager.appendFileContent(path, content);
        return ToolCallResult.of(fileInfo, "success");
    }

    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = "get_file_info", description = "获取文件元数据")
    public ToolCallResult<FileInfo> getFileInfo(
            @ToolParam(description = "文件路径") String path,
            ToolContext toolContext
    ) throws IOException {
        if (StringUtils.isEmpty(path)) {
            return ToolCallResult.of(null, "Param 'path' cannot be empty.");
        }
        path = formatPath(path);
        FileInfo fileInfo = fileManager.getFileInfo(path);
        return ToolCallResult.of(fileInfo, "success");
    }

    @Tool(name = "read_file", description = "从文件读取文本内容（最多返回100000个字符）")
    public ToolCallResult<FileContent> readFile(
            @ToolParam(description = "文件路径") String path,
            @ToolParam(description = "读取的行数（最高1000）") Integer limit,
            @ToolParam(description = "读取的开始行（默认1）", required = false) Integer startLine,
            ToolContext toolContext
    ) {
        if (StringUtils.isEmpty(path)) {
            return ToolCallResult.of(null, "Param 'path' cannot be empty.");
        }
        if (limit > 1000) {
            return ToolCallResult.of(null, "Limit cannot exceed 1000.");
        }
        path = formatPath(path);
        startLine = startLine == null ? 1 : startLine;
        FileContent fileContent = fileManager.readFileContent(path, startLine, limit, 100000);
        String message;
        if (fileContent.getIsTruncated()) {
            message = "文件读取因字符限制中断，最后完整读取的行：" + fileContent.getEndLine();
        } else if (fileContent.getIsEnd()) {
            message = "文件已读取到结尾，没有更多的行";
        } else {
            message = "文件读取成功，剩余行数：" + (fileContent.getLineCount() - fileContent.getEndLine());
        }
        return ToolCallResult.of(fileContent, message);
    }

    @Tool(name = "find_directory", description = "列出或按文件名查找目录下的项目（最多返回1000个项目）")
    public ToolCallResult<DirectoryTreeInfo> findDirectory(
            @ToolParam(description = "文件夹路径") String path,
            @ToolParam(description = "正则表达式（为空时默认列出全部）", required = false) String regex,
            @ToolParam(description = "递归深度（默认为1，仅列出当前目录中内容，最大为3）", required = false) Integer depth,
            ToolContext toolContext
    ) {
        if (StringUtils.isEmpty(path)) {
            return ToolCallResult.of(null, "Param 'path' cannot be empty.");
        }
        if (depth == null || depth < 1) {
            depth = 1;
        }
        if (depth > 3) {
            depth = 3;
        }
        path = formatPath(path);
        DirectoryTreeInfo result = fileManager.listDirectory(path, depth, regex, 1000);
        String message = "列出目录：%s，深度为%s，共找到%s个项目".formatted(path, depth, result.getProjectList().size());
        return ToolCallResult.of(result, message);
    }

    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = "delete_file", description = "删除单个文件或文件夹")
    public ToolCallResult<FileInfo> deleteFile(
            @ToolParam(description = "文件或文件夹路径（必须基于 '/workspace' 目录）") String path,
            @ToolParam(description = "是否递归删除（文件夹非空时必须为true方可删除，防止误操作）", required = false) Boolean recursive,
            ToolContext toolContext
    ) {
        if (StringUtils.isEmpty(path)) {
            return ToolCallResult.of(null, "Param 'path' cannot be empty.");
        }
        path = formatPath(path);
        boolean isRecursive = recursive != null && recursive;
        try {
            FileInfo fileInfo = fileManager.deleteFileOrDirectory(path, isRecursive);
            return ToolCallResult.of(fileInfo, "success");
        } catch (IOException e) {
            throw new FileDeleteException("删除失败: " + e.getMessage(), e);
        }
    }

    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = "move_file", description = "移动单个文件或文件夹")
    public ToolCallResult<FileInfo> moveFile(
            @ToolParam(description = "源文件/文件夹路径") String sourcePath,
            @ToolParam(description = "目标路径（必须基于 '/workspace' 目录，不存在的文件夹会自动创建）") String targetPath,
            ToolContext toolContext
    ) {
        if (sourcePath == null || sourcePath.isEmpty()) {
            return ToolCallResult.of(null, "Source path cannot be empty.");
        }
        if (targetPath == null || targetPath.isEmpty()) {
            return ToolCallResult.of(null, "Target path cannot be empty.");
        }
        sourcePath = formatPath(sourcePath);
        targetPath = formatPath(targetPath);
        try {
            FileInfo fileInfo = fileManager.moveFileOrDirectory(sourcePath, targetPath);
            return ToolCallResult.of(fileInfo, "success");
        } catch (IOException e) {
            throw new FileMoveException("移动失败: " + e.getMessage(), e);
        }
    }

    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = "copy_file", description = "复制单个文件或文件夹")
    public ToolCallResult<FileInfo> copyFile(
            @ToolParam(description = "源文件/文件夹路径") String sourcePath,
            @ToolParam(description = "目标路径（必须基于 '/workspace' 目录，不存在的文件夹会自动创建）") String targetPath,
            ToolContext toolContext
    ) {
        if (sourcePath == null || sourcePath.isEmpty()) {
            return ToolCallResult.of(null, "Source path cannot be empty.");
        }
        if (targetPath == null || targetPath.isEmpty()) {
            return ToolCallResult.of(null, "Target path cannot be empty.");
        }
        sourcePath = formatPath(sourcePath);
        targetPath = formatPath(targetPath);
        try {
            FileInfo fileInfo = fileManager.copyFileOrDirectory(sourcePath, targetPath);
            return ToolCallResult.of(fileInfo, "success");
        } catch (IOException e) {
            throw new FileCopyException("复制失败: " + e.getMessage(), e);
        }
    }

    @Tool(name = "find_in_file", description = "在文本文件中按正则表达式搜索匹配的行（返回行号和行内容）")
    public ToolCallResult<FileFindResult> findInFile(
            @ToolParam(description = "文件路径") String path,
            @ToolParam(description = "正则表达式") String regex,
            @ToolParam(description = "最大返回结果数（默认100，最高1000）", required = false) Integer maxResults,
            ToolContext toolContext
    ) {
        if (StringUtils.isEmpty(path)) {
            return ToolCallResult.of(null, "Param 'path' cannot be empty.");
        }
        if (regex == null || regex.isEmpty()) {
            return ToolCallResult.of(null, "Param 'regex' cannot be empty.");
        }
        if (maxResults == null || maxResults < 1) {
            maxResults = 100;
        }
        if (maxResults > 1000) {
            maxResults = 1000;
        }
        path = formatPath(path);
        FileFindResult result = fileManager.findInFile(path, regex, maxResults);
        int foundCount = result.getFoundLines() != null ? result.getFoundLines().size() : 0;
        String message;
        if (result.getIsTruncated()) {
            message = "搜索因达到最大结果数而截断，已返回 " + foundCount + " 条匹配行";
        } else {
            message = "搜索完成，共找到 " + foundCount + " 条匹配行";
        }
        return ToolCallResult.of(result, message);
    }

    @ToolStrategy(operationType = ToolStrategy.OPERATION_TYPE_WRITE)
    @Tool(name = "replace_in_file", description = "替换文本文件中的字符串，返回影响的字符串数和行数")
    public ToolCallResult<FileReplaceResult> replaceInFile(
            @ToolParam(description = "文件路径（必须基于 '/workspace' 目录）") String path,
            @ToolParam(description = "目标字符串（严格精确匹配，非正则表达式）") String target,
            @ToolParam(description = "替换字符串", required = false) String replacement,
            ToolContext toolContext
    ) {
        if (StringUtils.isEmpty(path)) {
            return ToolCallResult.of(null, "Param 'path' cannot be empty.");
        }
        if (target == null || target.isEmpty()) {
            return ToolCallResult.of(null, "Param 'target' cannot be empty.");
        }
        path = formatPath(path);
        FileReplaceResult result = fileManager.replaceInFile(path, target, replacement);
        String message = "替换完成，共替换 " + result.getReplaceCount() + " 处，涉及 " + result.getAffectedLineCount() + " 行";
        return ToolCallResult.of(result, message);
    }

    private String formatPath(String path) {
        if (path == null || path.isEmpty()) {
            return "./";
        }
        if (path.startsWith("/")) {
            return "." + path;
        }
        return path;
    }

}
