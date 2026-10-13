package com.bin.actx.plugin.core.mcp;

import com.bin.actx.context.RequestContext;
import com.bin.actx.mcp.McpClientManager;
import com.bin.actx.plugin.BasePlugin;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.service.request.RequestMessagePayload;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.tool.ToolCallback;

/**
 * MCP 插件
 *
 * @author Bin
 * @since 2026/8/19 15:48
 */
public class McpPlugin extends BasePlugin implements RequestHook {

    @Resource
    private McpClientManager mcpClientManager;

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        String enabledList = (String) requestContext.getPluginMetadata(this).get("enabledList");

        if (StringUtils.isEmpty(enabledList)) {
            enabledList = "*";
        }

        if ("*".equals(enabledList)) {
            List<ToolCallback> toolCallbacks = mcpClientManager.getAllToolCallbacks();
            requestMessagePayload.addTool(toolCallbacks);
        } else {
            Collection<String> toolKitNames = Arrays.stream(enabledList.split(",")).toList();
            List<ToolCallback> toolCallbacks = mcpClientManager.getToolCallbacksByNames(toolKitNames);
            requestMessagePayload.addTool(toolCallbacks);
        }

    }

}
