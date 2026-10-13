package com.bin.actx.file;

import com.bin.actx.config.properties.AppProperties;
import com.bin.actx.exception.FilePermissionException;
import com.bin.actx.exception.FileReadException;
import com.bin.actx.exception.FileSaveException;
import com.bin.actx.util.FileSecurityUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jiang.tools.file.FileUtils;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * 文件管理器
 *
 * @author Bin
 * @since 2026/5/29 17:21
 */
@Component
public class FileManager {

    @Autowired
    private AppProperties appProperties;

    public FileInfo getFileInfo(String path) throws IOException {
        Path rootPath = Path.of(appProperties.getFileStorageDir());
        Path finalPath = rootPath.resolve(path);
        return this.getFileInfo(finalPath);
    }

    public FileInfo getFileInfo(Path finalPath) throws IOException {
        Path rootPath = Path.of(appProperties.getFileStorageDir());
        String relativizePath = rootPath.relativize(finalPath).toString();

        this.verifyPath(rootPath, finalPath);
        if (!Files.exists(finalPath)) {
            throw new FileNotFoundException("目标不存在: " + relativizePath);
        }

        BasicFileAttributes attrs = Files.readAttributes(finalPath, BasicFileAttributes.class);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(finalPath.getFileName().toString());
        fileInfo.setFilePath(relativizePath);
        fileInfo.setIsDirectory(attrs.isDirectory());
        if (!fileInfo.getIsDirectory()) {
            fileInfo.setSize(attrs.size());
            fileInfo.setExtension(FileUtils.getExtension(finalPath));
        }
        fileInfo.setLastModifiedTime(attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        fileInfo.setCreationTime(attrs.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        return fileInfo;
    }

    /**
     * 写入文件内容
     *
     * @param path             文件路径
     * @param conflictStrategy 冲突策略
     * @param content          写入内容（为null时仅执行覆盖删除，不写入新内容）
     * @return 文件信息
     */
    public FileInfo writeFileContent(String path, String conflictStrategy, String content) {
        try {
            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path workspacePath = rootPath.resolve("workspace/").normalize();
            Path fullPath = rootPath.resolve(path).normalize();

            this.verifyFilePath(workspacePath, fullPath);

            if (Files.exists(fullPath)) {
                switch (conflictStrategy) {
                    case "rename":
                        // 转为可用路径（防止同名文件）
                        fullPath = FileUtils.resolveAvailablePath(fullPath);
                        Files.createFile(fullPath);
                        break;
                    case "cover":
                        // 这里啥也不用干，默认就是覆盖
                        break;
                    case "stop":
                    default:
                        throw new FileNotFoundException("文件已存在: " + path);
                }
            } else {
                Files.createDirectories(fullPath.getParent());
                Files.createFile(fullPath);
            }

            Files.writeString(fullPath, content != null ? content : "");

            return this.getFileInfo(fullPath);
        } catch (IOException e) {
            throw new FileSaveException("文件保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入文件
     *
     * @param path             文件路径
     * @param conflictStrategy 冲突策略
     * @param bytes            字节数组
     * @return 文件信息
     */
    public FileInfo writeFile(String path, String conflictStrategy, byte[] bytes) {
        try {
            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path workspacePath = rootPath.resolve("workspace/").normalize();
            Path fullPath = rootPath.resolve(path).normalize();

            this.verifyFilePath(workspacePath, fullPath);

            if (Files.exists(fullPath)) {
                switch (conflictStrategy) {
                    case "rename":
                        // 转为可用路径（防止同名文件）
                        fullPath = FileUtils.resolveAvailablePath(fullPath);
                        Files.createFile(fullPath);
                        break;
                    case "cover":
                        // 这里啥也不用干，默认就是覆盖
                        break;
                    case "stop":
                    default:
                        throw new FileNotFoundException("文件已存在: " + path);
                }
            } else {
                Files.createDirectories(fullPath.getParent());
                Files.createFile(fullPath);
            }

            Files.write(fullPath, bytes);

            return this.getFileInfo(fullPath);
        } catch (IOException e) {
            throw new FileSaveException("文件保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 修补文件内容
     *
     * @param path       文件路径
     * @param startLine  开始写入的行
     * @param coverLines 覆盖的行数
     * @param content    写入内容（为null时仅执行覆盖删除，不写入新内容）
     * @return 文件信息
     */
    public FileInfo patchFileContent(String path, Integer startLine, Integer coverLines, String content) {
        try {
            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path workspacePath = rootPath.resolve("workspace/").normalize();
            Path fullPath = rootPath.resolve(path).normalize();

            this.verifyFilePath(workspacePath, fullPath);

            if (!Files.exists(fullPath)) {
                throw new FileNotFoundException("文件不存在: " + path);
            }

            List<String> existingLines = Files.readAllLines(fullPath);

            boolean hasContent = content != null && !content.isEmpty();
            List<String> contentLines = hasContent ? content.lines().toList() : List.of();

            int start = (startLine != null) ? startLine : existingLines.size() + 1;
            if (start < 1) {
                start = 1;
            }

            int cover = (coverLines != null && coverLines > 0) ? coverLines : 0;
            if (!hasContent && cover == 0) {
                throw new IllegalArgumentException("Content is empty and coverLines is not set.");
            }

            List<String> resultLines = new ArrayList<>();

            int beforeEnd = Math.min(start - 1, existingLines.size());
            for (int i = 0; i < beforeEnd; i++) {
                resultLines.add(existingLines.get(i));
            }

            resultLines.addAll(contentLines);

            int afterStart = start - 1 + cover;
            for (int i = afterStart; i < existingLines.size(); i++) {
                resultLines.add(existingLines.get(i));
            }

            Files.write(fullPath, resultLines);

            return this.getFileInfo(fullPath);
        } catch (IOException e) {
            throw new FileSaveException("文件保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 追加文件内容
     *
     * @param path    文件路径
     * @param content 追加内容
     * @return 文件信息
     */
    public FileInfo appendFileContent(String path, String content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Content is empty.");
        }

        try {
            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path workspacePath = rootPath.resolve("workspace/").normalize();
            Path fullPath = rootPath.resolve(path).normalize();

            this.verifyFilePath(workspacePath, fullPath);

            if (!Files.exists(fullPath)) {
                throw new FileNotFoundException("文件不存在: " + path);
            }

            Files.write(fullPath, content.getBytes(), StandardOpenOption.APPEND);

            return this.getFileInfo(fullPath);
        } catch (IOException e) {
            throw new FileSaveException("文件保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出目录内容
     *
     * @param dirPath  目录路径
     * @param depth    递归深度
     * @param regex    正则表达式
     * @param maxItems 最大返回条目数
     * @return 文件/目录信息列表
     */
    public DirectoryTreeInfo listDirectory(String dirPath, int depth, String regex, int maxItems) {
        try {
            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path fullPath = rootPath.resolve(dirPath).normalize();

            this.verifyDirPath(rootPath, fullPath);

            DirectoryTreeInfo directoryTreeInfo = new DirectoryTreeInfo();
            directoryTreeInfo.setProjectList(new ArrayList<>());
            List<FileInfo> result = directoryTreeInfo.getProjectList();

            if (!Files.exists(fullPath)) {
                return directoryTreeInfo;
            }

            try (Stream<Path> stream = Files.walk(fullPath, depth)) {
                stream.filter(p -> regex == null || p.getFileName().toString().matches(regex))
                        .filter(p -> !p.equals(fullPath))
                        .limit(maxItems)
                        .forEach(p -> {
                            try {
                                result.add(getFileInfo(p));
                            } catch (IOException e) {
                                // 跳过无法读取的文件
                            }
                        });
            }

            return directoryTreeInfo;
        } catch (IOException e) {
            throw new FileReadException("目录列表获取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件内容
     *
     * @param path         文件路径
     * @param startLine    开始行
     * @param limit        行数
     * @param maxCharCount 最大字符总数
     * @return 文件内容
     */
    public FileContent readFileContent(String path, Integer startLine, Integer limit, Integer maxCharCount) {
        try {
            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path fullPath = rootPath.resolve(path).normalize();

            this.verifyFilePath(rootPath, fullPath);

            if (!Files.exists(fullPath)) {
                throw new FileNotFoundException("文件不存在: " + path);
            }

            // 流式获取总行数，避免将全部行加载到内存
            int totalLineCount;
            try (Stream<String> lineStream = Files.lines(fullPath)) {
                totalLineCount = (int) lineStream.count();
            }

            FileContent fileContent = new FileContent();
            fileContent.setFileInfo(this.getFileInfo(fullPath));
            fileContent.setLineCount(totalLineCount);

            if (startLine < 1) {
                startLine = 1;
            }
            if (startLine > totalLineCount) {
                fileContent.setStartLine(startLine);
                fileContent.setEndLine(startLine);
                fileContent.setContent(List.of());
                fileContent.setIsTruncated(false);
                fileContent.setIsEnd(true);
                return fileContent;
            }

            List<String> resultLines = new ArrayList<>();
            int currentCharCount = 0;
            int endLine = startLine;
            boolean truncated = false;

            // 流式读取目标行，惰性加载不会将全文件载入内存
            try (Stream<String> lines = Files.lines(fullPath)) {
                Iterator<String> it = lines.skip(startLine - 1).limit(limit).iterator();
                int lineIdx = startLine;
                while (it.hasNext()) {
                    String line = it.next();
                    // 非首行时需计入换行符长度
                    int newCharCount = currentCharCount + line.length() + (resultLines.isEmpty() ? 0 : 1);
                    if (newCharCount > maxCharCount) {
                        truncated = true;
                        break;
                    }
                    resultLines.add(line);
                    currentCharCount = newCharCount;
                    endLine = lineIdx;
                    lineIdx++;
                }
            }

            fileContent.setStartLine(startLine);
            fileContent.setEndLine(endLine);
            fileContent.setContent(resultLines);
            fileContent.setIsTruncated(truncated);
            fileContent.setIsEnd(endLine >= totalLineCount);
            return fileContent;
        } catch (IOException e) {
            throw new FileReadException("文件读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在文件中搜索匹配正则表达式的行
     *
     * @param path       文件路径
     * @param regex      正则表达式
     * @param maxResults 最大返回结果数
     * @return 查找结果
     */
    public FileFindResult findInFile(String path, String regex, int maxResults) {
        if (regex == null || regex.isEmpty()) {
            throw new IllegalArgumentException("Regex is empty.");
        }

        try {
            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path fullPath = rootPath.resolve(path).normalize();

            this.verifyFilePath(rootPath, fullPath);

            if (!Files.exists(fullPath)) {
                throw new FileNotFoundException("文件不存在: " + path);
            }

            Pattern pattern = Pattern.compile(regex);

            List<FileFindResult.Line> matchedLines = new ArrayList<>();
            int lineCount = 0;
            int totalMatchCount = 0;
            boolean truncated = false;

            try (Stream<String> lines = Files.lines(fullPath)) {
                Iterator<String> it = lines.iterator();
                int lineNumber = 0;
                while (it.hasNext()) {
                    String line = it.next();
                    lineNumber++;
                    Matcher matcher = pattern.matcher(line);
                    int lineMatchCount = 0;
                    while (matcher.find()) {
                        lineMatchCount++;
                    }
                    if (lineMatchCount > 0) {
                        if (matchedLines.size() >= maxResults) {
                            truncated = true;
                            break;
                        }
                        FileFindResult.Line resultLine = new FileFindResult.Line();
                        resultLine.setLineNumber(lineNumber);
                        resultLine.setLineContent(line);
                        resultLine.setMatchCount(lineMatchCount);
                        matchedLines.add(resultLine);
                        totalMatchCount += lineMatchCount;
                    }
                }
                lineCount = lineNumber;
            }

            FileFindResult result = new FileFindResult();
            result.setFileInfo(this.getFileInfo(fullPath));
            result.setScanLineCount(lineCount);
            result.setMatchCount(totalMatchCount);
            result.setFoundLines(matchedLines);
            result.setIsTruncated(truncated);
            result.setIsEnd(!truncated);
            return result;
        } catch (IOException e) {
            throw new FileReadException("文件搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 替换文件中的字符串
     *
     * @param path        文件路径
     * @param target      目标字符串
     * @param replacement 替换字符串
     * @return 替换结果
     */
    public FileReplaceResult replaceInFile(String path, String target, String replacement) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("Target string is empty.");
        }

        try {
            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path workspacePath = rootPath.resolve("workspace/").normalize();
            Path fullPath = rootPath.resolve(path).normalize();

            this.verifyFilePath(workspacePath, fullPath);

            if (!Files.exists(fullPath)) {
                throw new FileNotFoundException("文件不存在: " + path);
            }

            List<String> allLines = Files.readAllLines(fullPath);
            List<String> resultLines = new ArrayList<>();
            int replaceCount = 0;
            int affectedLineCount = 0;

            for (String line : allLines) {
                int countBefore = countOccurrences(line, target);
                if (countBefore > 0) {
                    String newLine = line.replace(target, replacement != null ? replacement : "");
                    resultLines.add(newLine);
                    replaceCount += countBefore;
                    affectedLineCount++;
                } else {
                    resultLines.add(line);
                }
            }

            Files.write(fullPath, resultLines);

            FileReplaceResult result = new FileReplaceResult();
            result.setFileInfo(this.getFileInfo(fullPath));
            result.setReplaceCount(replaceCount);
            result.setAffectedLineCount(affectedLineCount);
            return result;
        } catch (IOException e) {
            throw new FileSaveException("文件替换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 统计字符串在行中出现的次数
     */
    private int countOccurrences(String line, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = line.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }

    /**
     * 删除文件或文件夹
     *
     * @param path      文件或文件夹路径
     * @param recursive 是否递归删除（文件夹非空时必须为true）
     * @return 被删除的文件信息
     */
    public FileInfo deleteFileOrDirectory(String path, boolean recursive) throws IOException {
        Path rootPath = Path.of(appProperties.getFileStorageDir());
        Path workspacePath = rootPath.resolve("workspace/").normalize();
        Path fullPath = rootPath.resolve(path).normalize();

        this.verifyPath(workspacePath, fullPath);

        FileInfo fileInfo = this.getFileInfo(fullPath);

        if (fileInfo.getIsDirectory()) {
            try (Stream<Path> list = Files.list(fullPath)) {
                if (!recursive && list.findAny().isPresent()) {
                    throw new IllegalArgumentException("Directory is not empty, use recursive=true to delete.");
                }
            }
            try (Stream<Path> walk = Files.walk(fullPath)) {
                List<Path> pathsToDelete = walk.sorted(Comparator.reverseOrder()).toList();
                for (Path p : pathsToDelete) {
                    Files.delete(p);
                }
            }
        } else {
            Files.delete(fullPath);
        }

        return fileInfo;
    }

    /**
     * 移动文件或文件夹
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @return 移动后的文件信息
     */
    public FileInfo moveFileOrDirectory(String sourcePath, String targetPath) throws IOException {
        Path rootPath = Path.of(appProperties.getFileStorageDir());
        Path workspacePath = rootPath.resolve("workspace/").normalize();

        Path srcFullPath = rootPath.resolve(sourcePath).normalize();
        Path targetFullPath = rootPath.resolve(targetPath).normalize();

        this.verifyPath(workspacePath, srcFullPath);
        this.verifyPath(workspacePath, targetFullPath);

        if (!FileSecurityUtils.isSubPath(rootPath, targetFullPath)) {
            throw new FilePermissionException("Target path is not inside the permission scope.");
        }

        Files.createDirectories(targetFullPath.getParent());
        Files.move(srcFullPath, targetFullPath, StandardCopyOption.REPLACE_EXISTING);

        return this.getFileInfo(targetFullPath);
    }

    /**
     * 复制文件或文件夹
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @return 复制后的文件信息
     */
    public FileInfo copyFileOrDirectory(String sourcePath, String targetPath) throws IOException {
        Path rootPath = Path.of(appProperties.getFileStorageDir());
        Path workspacePath = rootPath.resolve("workspace/").normalize();

        Path srcFullPath = rootPath.resolve(sourcePath).normalize();
        Path targetFullPath = rootPath.resolve(targetPath).normalize();

        // 复制源必须在存储目录下，目标必须在工作空间下
        this.verifyPath(rootPath, srcFullPath);
        this.verifyPath(workspacePath, targetFullPath);

        if (Files.isDirectory(srcFullPath)) {
            Files.createDirectories(targetFullPath);
            try (Stream<Path> walk = Files.walk(srcFullPath)) {
                List<Path> srcPaths = walk.filter(p -> !p.equals(srcFullPath)).toList();
                for (Path src : srcPaths) {
                    Path dest = targetFullPath.resolve(srcFullPath.relativize(src));
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } else {
            Files.createDirectories(targetFullPath.getParent());
            Files.copy(srcFullPath, targetFullPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return this.getFileInfo(targetFullPath);
    }

    /**
     * 下载文件
     *
     * @param path 文件相对路径
     * @return Resource 资源对象
     * @throws IOException IO异常
     */
    public Resource downloadFile(String path) throws IOException {
        Path rootPath = Path.of(appProperties.getFileStorageDir());
        Path finalPath = rootPath.resolve(path);

        this.verifyFilePath(rootPath, finalPath);

        if (!Files.exists(finalPath)) {
            throw new FileNotFoundException("文件不存在: " + path);
        }

        return new InputStreamResource(Files.newInputStream(finalPath));
    }

    /**
     * 验证路径存在且在工作空间内（支持文件和文件夹）
     */
    private void verifyPath(Path rootPath, Path path) throws IOException {
        if (!FileSecurityUtils.isSubPath(rootPath, path)) {
            throw new FilePermissionException("Path is not inside the permission scope.");
        }
    }

    private void verifyDirPath(Path rootPath, Path path) throws IOException {
        // 确保目录存在
        Files.createDirectories(rootPath);

        // 检查路径是否在工作空间内
        if (!FileSecurityUtils.isSubPath(rootPath, path)) {
            throw new FilePermissionException("Path is not inside the permission scope.");
        }

        if (Files.exists(path)) {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            if (!attrs.isDirectory()) {
                throw new IllegalArgumentException("Path is not a directory.");
            }
        }
    }

    private void verifyFilePath(Path rootPath, Path path) throws IOException {
        // 确保目录存在
        Files.createDirectories(rootPath);

        // 检查路径是否在工作空间内
        if (!FileSecurityUtils.isSubPath(rootPath, path)) {
            throw new FilePermissionException("Path is not inside the permission scope.");
        }

        if (Files.exists(path)) {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            if (attrs.isDirectory() || !attrs.isRegularFile()) {
                throw new IllegalArgumentException("Path is not a file.");
            }
            if (attrs.size() > 1024 * 1024 * 10) {
                throw new IllegalArgumentException("File size is too large.");
            }
        }
    }


    public List<Media> getMediaList(Collection<String> paths) {
        List<Media> mediaList = new ArrayList<>();
        for (String path : paths) {
            FileInfo fileInfo = null;
            try {
                fileInfo = this.getFileInfo(path);
            } catch (IOException ignored) {
                continue;
            }

            MimeType mimeType = switch (fileInfo.getExtension()) {
                case "jpeg", "jpg" -> MimeTypeUtils.IMAGE_JPEG;
                case "png" -> MimeTypeUtils.IMAGE_PNG;
                case "gif" -> MimeTypeUtils.IMAGE_GIF;
                case "html", "htm" -> MimeTypeUtils.TEXT_HTML;
                case "xml" -> MimeTypeUtils.TEXT_XML;
                case "txt", "md" -> MimeTypeUtils.TEXT_PLAIN;
                default -> MimeTypeUtils.ALL;
            };

            Path rootPath = Path.of(appProperties.getFileStorageDir());
            Path fullPath = rootPath.resolve(fileInfo.getFilePath()).normalize();

            Media media = Media.builder().mimeType(mimeType)
                    .id(IdUtils.uuid())
                    .name(fileInfo.getFileName())
                    .data(new FileSystemResource(fullPath))
                    .build();
            mediaList.add(media);
        }
        return mediaList;
    }

}
