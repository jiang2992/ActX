package com.bin.actx.plugin.core.task;

import com.bin.actx.channel.MessageChannel;
import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.message.AnswerSessionMessageBuilder;
import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.plugin.BasePlugin;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.plugin.hook.ResponseHook;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.tool.ToolManager;
import com.bin.actx.tool.kit.task.TaskToolkit;
import com.bin.actx.util.SpringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * 任务创建
 * <br/> 1. 评估用户任务：调用评估Agent -> 返回评估结果
 * <br/> 2. 计划执行任务：调用计划Agent -> 创建任务 -> 执行任务 -> 输出最终结果or需要提供额外信息
 * <br/> 3. 继续执行任务：补充任务信息 -> 继续执行任务 -> 输出最终结果or需要提供额外信息
 * <br/>
 *
 * @author Bin
 * @since 2026/5/20 17:23
 */
public class TaskPlugin extends BasePlugin implements RequestHook, ResponseHook {

    public static final String TOOL_EVALUATE_USER_TASK_NAME = "evaluate_user_task";
    public static final String TOOL_PLANNING_AND_EXECUTE_TASK_NAME = "planning_and_execute_task";
    public static final String TOOL_CONTINUE_TASK_NAME = "continue_task";

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        ToolManager toolManager = SpringUtils.getBean(ToolManager.class);
        requestMessagePayload.addTool(toolManager.getToolCallbackList(Collections.singleton(TaskToolkit.class)));
        requestMessagePayload.addSystemPrompt(getSystemPrompt(requestContext));
    }

    /**
     * 获取系统提示
     *
     * @return 系统提示
     */
    private Collection<String> getSystemPrompt(RequestContext requestContext) {
        return new ArrayList<>(Arrays.asList(
                "---",
                "",
                "# 任务处理",
                "",
                "## 评估",
                "",
                "- 收到用户任务后，开始处理前快速判断其复杂度（最高优先级）：明显简单的问题（如常识问答、单一计算）无需额外分析，直接开始处理并返回结果。",
                "- 对无法一眼确认复杂度的任务，直接调用系统提供的评估工具进行复杂度分析。",
                "",
                "## 规划",
                "",
                "- 根据评估结果，如果任务复杂度较高，则不要再探索细节，而是直接调用任务规划与执行工具，将任务交给调度器进行拆解、规划和实际执行。",
                "",
                "## 执行",
                "",
                "- 调用后等待执行结果返回；在执行期间，你无需干预具体步骤。",
                "- 执行过程中如果用户追加需求、提出修改或给出决策，调用 `%s` 工具将这些新信息融入当前任务，继续推进。".formatted(
                        TOOL_CONTINUE_TASK_NAME),
                "",
                "## 失败处理",
                "",
                "- 如果调度器返回的任务执行失败或未能达到预期目标，你必须：",
                "  1. 将失败原因以清晰、易懂的方式告知用户，包括关键的错误信息或瓶颈点。",
                "  2. 询问用户接下来的处理方式，重试、由你直接执行、调整需求等。",
                "",
                "## 汇总",
                "",
                "- 收到调度器返回的最终执行结果后，进行简洁梳理与整合。",
                "- 以清晰的结构将核心信息、结论或产出物呈现给用户，必要时可附上简要说明或后续建议。"
        ));
    }

    @Override
    public MessageChannel responseProc(ResponseContext responseContext, RequestSessionMessage requestSessionMessage,
            AnswerSessionMessageBuilder answerSessionMessageBuilder) {
        return new TaskEventRecorder(responseContext, answerSessionMessageBuilder);
    }

}
