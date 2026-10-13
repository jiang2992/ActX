package com.bin.actx.controller;

import com.bin.actx.file.AttachmentInfo;
import com.bin.actx.file.AttachmentManager;
import com.bin.actx.file.FileInfo;
import com.bin.actx.file.FileManager;
import com.bin.actx.model.RequestResult;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件控制器
 *
 * @author Bin
 * @since 2026/5/18 09:28
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private AttachmentManager attachmentManager;

    @Autowired
    private FileManager fileManager;

    /**
     * 上传文件
     *
     * @param multipartFile 上传的文件
     * @return 文件ID
     */
    @PostMapping
    public RequestResult<AttachmentInfo> uploadFile(@RequestParam("file") MultipartFile multipartFile) {
        return RequestResult.success(attachmentManager.saveAttachment(multipartFile));
    }
    
    /**
     * 文件下载接口
     *
     * @param filePath 文件路径
     * @return 下载响应
     */
    @GetMapping(value = "/{*filePath}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filePath) throws IOException {
        filePath = formatPath(filePath);

        FileInfo fileInfo = fileManager.getFileInfo(filePath);
        Resource resource = fileManager.downloadFile(filePath);
        
        // 尝试确定文件的媒体类型
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            contentType = Files.probeContentType(Path.of(filePath));
        } catch (Exception e) {
            log.debug("无法确定文件类型: {}", filePath);
        }
        
        if (contentType == null || contentType.isEmpty()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileInfo.getFileName(), StandardCharsets.UTF_8))
                .body(resource);
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