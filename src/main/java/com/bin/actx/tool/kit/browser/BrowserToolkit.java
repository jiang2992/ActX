package com.bin.actx.tool.kit.browser;

import com.bin.actx.config.properties.AppProperties;
import com.bin.actx.network.html.HtmlPageData;
import com.bin.actx.network.html.HtmlRequestService;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.model.ToolCallResult;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 浏览器工具包（尚未完善，建议先通过MCP来接入）
 *
 * @author Bin
 * @since 2026/9/21 14:13
 */
@Toolkit(name = "浏览器", description = "允许使用临时浏览器访问页面、解析页面文本、截图等", autoDiscover = false)
public class BrowserToolkit {

    @Autowired
    private HtmlRequestService htmlRequestService;

    @Autowired
    private AppProperties appProperties;

    @Tool(name = "send_html_request", description = "请求HTML，返回解析后的页面内容")
    public ToolCallResult<List<HtmlPageData>> sendHtmlRequest(
            @ToolParam(description = "页面的URL,最多5条") List<String> urls
    ) {
        if (!appProperties.getPlaywrightEnable()) {
            return ToolCallResult.of(null, "Error: Playwright is not enabled");
        }
        try {
            if (urls == null || urls.isEmpty()) {
                return ToolCallResult.of(null, "Error: URL cannot be empty");
            }
            String message = null;
            if (urls.size() > 5) {
                urls = urls.subList(0, 5);
                message = "Warning: Only the first five URLs will be processed.";
            }
            List<HtmlPageData> plainTextPage = htmlRequestService.getPlainTextPage(urls);
            return ToolCallResult.of(plainTextPage, message);
        } catch (Exception e) {
            return ToolCallResult.of(null, "Error: Unexpected error occurred - " + e.getMessage());
        }
    }

}
