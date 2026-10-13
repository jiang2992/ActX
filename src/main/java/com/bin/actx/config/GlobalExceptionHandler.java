package com.bin.actx.config;

import com.bin.actx.exception.FileCopyException;
import com.bin.actx.exception.FileDeleteException;
import com.bin.actx.exception.FileDownloadException;
import com.bin.actx.exception.FileMoveException;
import com.bin.actx.exception.FilePermissionException;
import com.bin.actx.exception.FileReadException;
import com.bin.actx.exception.FileSaveException;
import com.bin.actx.exception.McpRegisterException;
import com.bin.actx.exception.ModelConfigException;
import com.bin.actx.exception.PlaywrightException;
import com.bin.actx.exception.SandboxInitializeException;
import com.bin.actx.exception.ToolCallingException;
import com.bin.actx.model.RequestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Bin
 * @since 2026/8/24
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public RequestResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return RequestResult.error(400, e.getMessage());
    }

    @ExceptionHandler(FilePermissionException.class)
    public RequestResult<Void> handleFilePermission(FilePermissionException e) {
        log.warn("文件权限异常: {}", e.getMessage());
        return RequestResult.error(403, e.getMessage());
    }

    @ExceptionHandler(FileDownloadException.class)
    public RequestResult<Void> handleFileDownload(FileDownloadException e) {
        log.error("文件下载失败", e);
        return RequestResult.error(500, "文件下载失败: " + e.getMessage());
    }

    @ExceptionHandler(FileSaveException.class)
    public RequestResult<Void> handleFileSave(FileSaveException e) {
        log.error("文件保存失败", e);
        return RequestResult.error(500, "文件保存失败: " + e.getMessage());
    }

    @ExceptionHandler(FileDeleteException.class)
    public RequestResult<Void> handleFileDelete(FileDeleteException e) {
        log.error("文件删除失败", e);
        return RequestResult.error(500, "文件删除失败: " + e.getMessage());
    }

    @ExceptionHandler(FileReadException.class)
    public RequestResult<Void> handleFileRead(FileReadException e) {
        log.error("文件读取失败", e);
        return RequestResult.error(500, "文件读取失败: " + e.getMessage());
    }

    @ExceptionHandler(FileMoveException.class)
    public RequestResult<Void> handleFileMove(FileMoveException e) {
        log.error("文件移动失败", e);
        return RequestResult.error(500, "文件移动失败: " + e.getMessage());
    }

    @ExceptionHandler(FileCopyException.class)
    public RequestResult<Void> handleFileCopy(FileCopyException e) {
        log.error("文件复制失败", e);
        return RequestResult.error(500, "文件复制失败: " + e.getMessage());
    }

    @ExceptionHandler(McpRegisterException.class)
    public RequestResult<Void> handleMcpRegister(McpRegisterException e) {
        log.error("MCP客户端注册失败", e);
        return RequestResult.error(500, "MCP客户端注册失败: " + e.getMessage());
    }

    @ExceptionHandler(ModelConfigException.class)
    public RequestResult<Void> handleModelConfig(ModelConfigException e) {
        log.error("模型配置异常", e);
        return RequestResult.error(500, e.getMessage());
    }

    @ExceptionHandler(PlaywrightException.class)
    public RequestResult<Void> handlePlaywright(PlaywrightException e) {
        log.error("Playwright执行失败", e);
        return RequestResult.error(500, "Playwright执行失败: " + e.getMessage());
    }

    @ExceptionHandler(ToolCallingException.class)
    public RequestResult<Void> handleToolCalling(ToolCallingException e) {
        log.error("工具调用失败", e);
        return RequestResult.error(500, e.getMessage());
    }

    @ExceptionHandler(SandboxInitializeException.class)
    public RequestResult<Void> handleSandboxInit(SandboxInitializeException e) {
        log.error("沙箱初始化失败", e);
        return RequestResult.error(500, "沙箱初始化失败: " + e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public RequestResult<Void> handleRuntime(RuntimeException e) {
        log.error("系统异常", e);
        return RequestResult.error(500, "系统内部错误");
    }

    @ExceptionHandler(Exception.class)
    public RequestResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return RequestResult.error(500, "系统内部错误");
    }

}
