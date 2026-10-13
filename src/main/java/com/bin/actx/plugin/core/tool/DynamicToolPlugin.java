package com.bin.actx.plugin.core.tool;

import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.tool.kit.system.SelectorToolkit;
import com.bin.actx.tool.model.ToolCallResult;
import com.bin.actx.tool.model.ToolKitInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jiang.tools.json.JsonUtils;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

/**
 * 动态工具插件，为请求消息装配动态工具，仅暴露 load_toolkit 给 LLM，由 LLM 动态选择工具包
 *
 * @author Bin
 * @since 2026/4/17 11:05
 */
public class DynamicToolPlugin extends ToolBasePlugin implements RequestHook {

    public static final String TOOLKIT_SELECTOR_NAME = "ToolSelector";

    public static final String TOOLKIT_SELECTOR_TOOL_NAME = "select_toolkit";

    public static final String METADATA_KEY_TOOL_KIT_NAME_LIST = "toolKitNameList";

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        Set<Class<?>> toolKitList = super.extractToolkitsForMetadata(requestContext);

        if (!toolKitList.isEmpty()) {
            requestMessagePayload.addToolkit(toolKitList);
        }

        requestContext.setMetadata(METADATA_KEY_TOOL_KIT_NAME_LIST, new HashSet<>());
    }

    @Override
    public void requestBefore(RequestMessagePayload requestMessagePayload, ResponseContext responseContext) {
        Collection<ToolKitInfo> toolkitList = toolManager.getToolkitList(requestMessagePayload.getToolkits());
        if (toolkitList != null && !toolkitList.isEmpty()) {
            requestMessagePayload.addSystemPrompt(getToolKitPrompt(toolkitList));

            // 添加动态工具选择工具
            requestMessagePayload.addTool(toolManager.getToolCallbackList(Collections.singleton(SelectorToolkit.class)));
        }
    }

    /**
     * 获取可加载的工具包提示词
     *
     * @param dynamicToolkitList 工具包列表
     * @return 工具包选择提示词
     */
    private Collection<String> getToolKitPrompt(Collection<ToolKitInfo> dynamicToolkitList) {
        String toolkitListInfo = dynamicToolkitList.stream()
                .map(toolKitInfo -> "- " + toolKitInfo.getName() + " - " + toolKitInfo.getDescription())
                .collect(Collectors.joining("\n"));
        return Arrays.asList(
                "---",
                "",
                "# 工具调用",
                "",
                "## 核心原则（重要）",
                "",
                "- 每轮回答都是**独立的决策单元**。上一轮使用了什么工具，对当前轮次**没有任何影响**。",
                "- 每次回答前，都必须**重新独立判断**是否需要调用工具、需要哪些工具包的工具。",
                "- 禁止“惯性”调用：不得因为上一轮调用了某工具，就在本轮继续使用它。",
                "",
                "**正确示例**：",
                "- 第1轮“查天气”→ 调用网络包",
                "- 第2轮“写文件”→ 重新判断，调用文件管理包",
                "",
                "**错误示例**：",
                "- 第1轮“查天气”→ 调用网络包",
                "- 第2轮“写文件”→ 沿用网络包（错误）",
                "",
                "## 调用流程（严格遵守）",
                "",
                "1. 在需要调用工具时，思考需要哪些工具包的工具。",
                "2. 提前调用 `%s` 选择所需工具包（不可省略）。".formatted(TOOLKIT_SELECTOR_TOOL_NAME),
                "3. 查看选择后由系统暴露的工具列表，选择合适的工具并确定具体参数要求，然后进行实际调用。",
                "",
                "## 注意事项",
                "",
                "- 错误重试：若返回参数错误，应重新选择工具包以确定参数要求；若工具返回错误或空结果，可调整参数重试一次；仍失败则如实说明。",
                "- 批量调用：对于无互相依赖的多个工具调用，应合并调用。",
                "- 调用限制：单次请求内工具调用总次数存在上限，请精简。",
                "",
                "## 可用工具包",
                "",
                toolkitListInfo);
    }

    @Override
    public Prompt toolCallAfter(Prompt prompt, ResponseContext responseContext) {
        prompt = super.toolCallAfter(prompt, responseContext);
        return this.withTools(prompt, responseContext);
    }

    private Prompt withTools(Prompt prompt, ResponseContext responseContext) {
        Collection<String> toolKitNames = (Collection<String>) responseContext.getRequestContext()
                .getMetadata(METADATA_KEY_TOOL_KIT_NAME_LIST);
        if (toolKitNames == null) {
            return prompt;
        }
        toolKitNames.addAll(extractSelectedKitName(prompt));

        if (toolKitNames.isEmpty()) {
            return prompt;
        }

        if (prompt.getOptions() instanceof ToolCallingChatOptions toolOptions) {
            List<ToolCallback> currentTools = new ArrayList<>(toolOptions.getToolCallbacks());
            List<ToolCallback> additionalTools = toolManager.getToolCallbackListByNames(toolKitNames);
            additionalTools.stream()
                    .filter(toolCallback -> currentTools.stream()
                            .noneMatch(ct -> ct.getToolDefinition().name().equals(toolCallback.getToolDefinition().name())))
                    .forEach(currentTools::add);
            ToolCallingChatOptions toolOptionsCopy = toolOptions.copy();
            toolOptionsCopy.setToolCallbacks(currentTools);
            return prompt.mutate().chatOptions(toolOptionsCopy).build();
        } else {
            return prompt;
        }
    }

    private Collection<String> extractSelectedKitName(Prompt prompt) {
        Set<String> toolKitNames = new HashSet<>();
        List<Message> messageList = prompt.getInstructions();
        if (messageList.isEmpty()) {
            return toolKitNames;
        }
        Message lastMessage = messageList.get(messageList.size() - 1);
        if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
            for (ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                boolean isLoadToolkit = TOOLKIT_SELECTOR_TOOL_NAME.equals(toolResponse.name());
                if (!isLoadToolkit) {
                    continue;
                }
                if (StringUtils.isEmpty(toolResponse.responseData())) {
                    continue;
                }
                ToolCallResult<List<String>> toolCallResult = JsonUtils.toBean(toolResponse.responseData(), ToolCallResult.class);
                toolKitNames.addAll(toolCallResult.getData() != null ? toolCallResult.getData() : Collections.emptyList());
            }
        }
        return toolKitNames;
    }

}
