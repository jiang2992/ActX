package com.bin.actx.tool.kit.network;

import com.bin.actx.file.FileInfo;
import com.bin.actx.network.http.HttpRequestParam;
import com.bin.actx.network.http.HttpRequestService;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.model.ToolCallResult;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 网络工具包
 *
 * @author Bin
 * @since 2026/5/16 12:49
 */
@Toolkit(name = "网络", description = "允许访问或下载网络资源")
public class NetworkToolkit {

    @Autowired
    private HttpRequestService httpRequestService;

    @Tool(name = "send_http_request", description = "发送HTTP请求，返回响应内容")
    public ToolCallResult<String> sendHttpRequest(
            @ToolParam(description = "HTTP请求方式(GET、POST、PUT、DELETE)") String method,
            @ToolParam(description = "请求的URL") String url,
            @ToolParam(description = "请求头键值对", required = false) Map<String, String> headers,
            @ToolParam(description = "请求体", required = false) String body,
            @ToolParam(description = "请求超时时间(默认30秒)", required = false) Integer timeout
    ) {
        try {
            // Validate method
            if (StringUtils.isEmpty(method)) {
                return ToolCallResult.of(null, "Error: HTTP method cannot be empty");
            }

            // Validate URL
            if (StringUtils.isEmpty(url)) {
                return ToolCallResult.of(null, "Error: URL cannot be empty");
            }

            HttpRequestParam httpRequestParam = new HttpRequestParam();
            httpRequestParam.setMethod(method);
            httpRequestParam.setUrl(url);
            httpRequestParam.setHeaders(headers);
            httpRequestParam.setBody(body);
            httpRequestParam.setTimeout(timeout);
            HttpResponse<String> response = httpRequestService.sendHttpRequest(httpRequestParam);

            // Build result message
            String resultMessage = "Status Code: " + response.statusCode()
                    + "\nHeaders: " + response.headers().map();

            return ToolCallResult.of(response.body(), resultMessage);

        } catch (IOException e) {
            return ToolCallResult.of(null, "Error: IO exception occurred - " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolCallResult.of(null, "Error: Request was interrupted - " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ToolCallResult.of(null, "Error: Invalid URL or parameters - " + e.getMessage());
        } catch (Exception e) {
            return ToolCallResult.of(null, "Error: Unexpected error occurred - " + e.getMessage());
        }
    }

    @Tool(name = "save_http_resource", description = "保存HTTP资源，返回保存后的文件信息")
    public ToolCallResult<FileInfo> saveHttpResource(
            @ToolParam(description = "HTTP请求方式(GET、POST、PUT、DELETE)") String method,
            @ToolParam(description = "请求的URL") String url,
            @ToolParam(description = "请求头键值对", required = false) Map<String, String> headers,
            @ToolParam(description = "请求体", required = false) String body,
            @ToolParam(description = "请求超时时间(默认30秒)", required = false) Integer timeout,
            @ToolParam(description = "保存路径（例如：/workspace/test/example.png）") String savePath
    ) {
        try {
            // Validate parameters
            if (StringUtils.isEmpty(savePath)) {
                return ToolCallResult.of(null, "Error: Request parameters cannot be null");
            }

            // Validate method
            if (StringUtils.isEmpty(method)) {
                return ToolCallResult.of(null, "Error: HTTP method cannot be empty");
            }

            // Validate URL
            if (StringUtils.isEmpty(url)) {
                return ToolCallResult.of(null, "Error: URL cannot be empty");
            }

            savePath = formatPath(savePath);

            HttpRequestParam httpRequestParam = new HttpRequestParam();
            httpRequestParam.setMethod(method);
            httpRequestParam.setUrl(url);
            httpRequestParam.setHeaders(headers);
            httpRequestParam.setBody(body);
            httpRequestParam.setTimeout(timeout);
            FileInfo fileInfo = httpRequestService.downloadHttpResource(httpRequestParam, savePath);

            return ToolCallResult.of(fileInfo, "The file has been saved to: " + fileInfo.getFilePath());
        } catch (IOException e) {
            return ToolCallResult.of(null, "Error: IO exception occurred - " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolCallResult.of(null, "Error: Request was interrupted - " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ToolCallResult.of(null, "Error: Invalid URL or parameters - " + e.getMessage());
        } catch (Exception e) {
            return ToolCallResult.of(null, "Error: Unexpected error occurred - " + e.getMessage());
        }
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
