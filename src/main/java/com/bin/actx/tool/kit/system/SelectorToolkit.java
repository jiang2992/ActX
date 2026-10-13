package com.bin.actx.tool.kit.system;

import com.bin.actx.context.ResponseContext;
import com.bin.actx.context.ResponseContextManager;
import com.bin.actx.plugin.core.tool.DynamicToolPlugin;
import com.bin.actx.tool.ToolManager;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.model.ToolCallResult;
import com.bin.actx.tool.model.ToolKitInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 工具选择器
 *
 * @author Bin
 * @since 2026/5/12 15:13
 */
@Toolkit(name = DynamicToolPlugin.TOOLKIT_SELECTOR_NAME, autoDiscover = false)
public class SelectorToolkit {

    @Autowired
    private ToolManager toolManager;
    @Autowired
    private ResponseContextManager responseContextManager;

    @Tool(name = DynamicToolPlugin.TOOLKIT_SELECTOR_TOOL_NAME, description = "选择一个或多个工具包中的工具")
    public ToolCallResult<Collection<String>> loadToolkit(
            @ToolParam(description = "工具包名称") List<String> toolKitNames,
            ToolContext toolContext) {
        if (toolKitNames == null || toolKitNames.isEmpty()) {
            return ToolCallResult.of(null, "Please specify the toolkit name.");
        }
        Collection<String> toolKitNameList = toolManager.getToolkitListByNames(toolKitNames)
                .stream()
                .map(ToolKitInfo::getName)
                .collect(Collectors.toSet());
        if (toolKitNameList.isEmpty()) {
            return ToolCallResult.of(null, "No toolkit was found.");
        }

        ResponseContext responseContext = responseContextManager.getByToolContext(toolContext.getContext());
        Collection<String> historyToolkitNames = (Collection<String>) responseContext.getRequestContext()
                .getMetadata(DynamicToolPlugin.METADATA_KEY_TOOL_KIT_NAME_LIST);
        Collection<String> accumulatedToolkitNames = new ArrayList<>(toolKitNameList);
        if (historyToolkitNames != null) {
            accumulatedToolkitNames.addAll(historyToolkitNames);
        }

        return ToolCallResult.of(toolKitNameList,
                "The currently exposed toolkit: [%s] (The specific tools have been listed in the 'tools' attribute)".formatted(
                        String.join(",", toolKitNameList)));

    }

}
