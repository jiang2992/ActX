package com.bin.actx.plugin.core.file;

import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.file.FileManager;
import com.bin.actx.plugin.BasePlugin;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.plugin.hook.ToolHook;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.tool.kit.file.FileManageToolkit;
import com.bin.actx.tool.model.ToolCallResult;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jiang.tools.json.JsonUtils;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;

/**
 * 文件插件
 *
 * @author Bin
 * @since 2026/8/21 17:04
 */
public class FilePlugin extends BasePlugin implements RequestHook, ToolHook {

    @Resource
    private FileManager fileManager;

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        requestMessagePayload.addToolkit(FileManageToolkit.class);
        requestMessagePayload.addSystemPrompt(this.getPrompt());
    }

    /**
     * 获取系统提示词
     *
     * @return 系统提示词
     */
    private Collection<String> getPrompt() {
        return Arrays.asList(
                "---",
                "",
                "# 文件管理",
                "",
                "## 目录规划",
                "",
                "- `/attachment`：存储用户上传的所有附件，只读；必要修改时，复制副本到`/workspace`后修改副本。",
                "- `/workspace`：你的工作空间，拥有完整的读写权限。",
                "- `/archive`：归档目录，只读；通常由用户自行管理。",
                "",
                "## 操作限制",
                "",
                "- 目录管理：在`/workspace`目录下应进行合理的子目录规划，如按项目、任务等分类。",
                "- 文本写入：所有写操作都只能在`/workspace`目录或子目录下进行，且写入工具统一限制%s字符以内，如需写入大量内容，请通过追加或修补工具多次写入。".formatted(
                        FileManageToolkit.MAX_FILE_WRITE_LIMIT_SIZE),
                "- 文本读取：非必要时，不要一次性读取整个文件，应通过行数限制进行分段读取。",
                "",
                "## 其他",
                "",
                "- 图片识别：当你需要识别本地图片时，调用工具：" + String.join("、", FileManageToolkit.RETRIEVE_ATTACHMENT_TOOLS),
                "- 用户下载：当用户需要下载本地文件时，使用markdown格式输出超链接：#/file/文件路径（例如：#/file/workspace/example.pdf）",
                "- 图片预览：你可以直接使用markdown格式输出图片标签，如果是本地文件，则使用：/file/文件路径（例如：/file/workspace/example.png）",
                ""
        );
    }

    @Override
    public Prompt toolCallAfter(Prompt prompt, ResponseContext responseContext) {
        Collection<String> paths = this.extractVisualFilePath(prompt);
        if (!paths.isEmpty()) {
            super.debugLog("load file paths: " + paths);
            prompt = prompt.augmentUserMessage(userMessage -> {
                List<Media> mediaList = new ArrayList<>(userMessage.getMedia());
                mediaList.addAll(fileManager.getMediaList(paths));
                return userMessage.mutate().media(mediaList).build();
            });
        }
        return prompt;
    }

    /**
     * 解析出LLM获取的视觉文件路径集合
     *
     * @param prompt prompt
     * @return 视觉文件路径集合
     */
    private Collection<String> extractVisualFilePath(Prompt prompt) {
        Set<String> paths = new HashSet<>();
        List<Message> messageList = prompt.getInstructions();
        if (messageList.isEmpty()) {
            return paths;
        }
        Message lastMessage = messageList.get(messageList.size() - 1);
        if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
            for (ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                boolean isReadFile =
                        toolResponse.name() != null && FileManageToolkit.RETRIEVE_ATTACHMENT_TOOLS.contains(toolResponse.name());
                if (!isReadFile) {
                    continue;
                }
                if (StringUtils.isEmpty(toolResponse.responseData())) {
                    continue;
                }
                ToolCallResult<List<String>> fileReadResult = JsonUtils.toBean(toolResponse.responseData(), ToolCallResult.class);
                if (fileReadResult.getData() == null || fileReadResult.getData().isEmpty()) {
                    continue;
                }
                paths.addAll(fileReadResult.getData());
            }
        }
        return paths;
    }

}
