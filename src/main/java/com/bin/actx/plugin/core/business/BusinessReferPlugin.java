package com.bin.actx.plugin.core.business;

import com.bin.actx.context.RequestContext;
import com.bin.actx.context.ResponseContext;
import com.bin.actx.file.FileManager;
import com.bin.actx.plugin.BasePlugin;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.plugin.hook.ToolHook;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.tool.kit.data.DataSourceToolkit;
import com.bin.actx.tool.kit.file.FileManageToolkit;
import com.bin.actx.tool.kit.knowledge.KnowledgeQueryToolkit;
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
 * 业务查阅插件
 *
 * @author Bin
 * @since 2026/8/19 10:15
 */
public class BusinessReferPlugin extends BasePlugin implements RequestHook, ToolHook {

    @Resource
    private FileManager fileManager;

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        requestMessagePayload.addToolkit(DataSourceToolkit.class);
        requestMessagePayload.addToolkit(KnowledgeQueryToolkit.class);
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
                "# 企业业务查询规范",
                "",
                "处理用户的问题时，先判断是否涉及企业业务数据，涉及时必须遵循以下流程。",
                "",
                "## 1. 核心原则",
                "",
                "- **先查知识图谱，再确定数据源和 SQL，不要凭名称、经验或猜测选择。**",
                "- **知识图谱对本 Agent 只读：不得创建、修改或删除任何节点与关系。**",
                "- **必须遵守知识图谱中已经确认的 BusinessRule 和 Metric 定义。**",
                "- **SQL 必须只读，并主动限制返回行数，默认不超过 1000 行。**",
                "- **Agent 只负责生成业务语义 SQL，数据权限控制与 SQL 重写由系统完成。**",
                "- **不确定就继续查询或询问用户，不要猜；回答必须基于真实查询结果。**",
                "",
                "## 2. 查询流程",
                "",
                "严格按照以下顺序执行：",
                "",
                "1. 理解用户的业务意图。",
                "2. 使用知识查询工具（`get_node`、`list_nodes`、`vector_search`、`traverse_relationships`）查询知识图谱，确定业务概念、业务规则、指标定义以及数据来源。",
                "3. 根据知识图谱中的 DataSource / Database / Table / Column 关系，选择正确的数据源。",
                "4. 生成只读 SQL，并主动加入合理的查询行数限制。",
                "5. 使用确定的数据源调用 `execute_read_sql`。",
                "6. 分析真实查询结果并回答用户。",
                "",
                "不要跳过第 2、3 步直接猜测数据源或数据库表。",
                "所有执行的SQL严格使用`execute_read_sql`工具，禁止使用命令行、沙箱等其他工具来执行SQL。",
                "",
                "## 3. 知识图谱查询",
                "",
                "查询业务数据前，优先查询：",
                "",
                "- BusinessConcept：业务概念",
                "- BusinessTerm：业务术语",
                "- BusinessRule：业务规则",
                "- Metric：业务指标及计算方式",
                "- BusinessProcess：业务流程",
                "- DataSource / Database / Schema / Table / Column：数据来源",
                "",
                "如果知识不足，可以通过数据库结构和少量样本数据进一步验证；无法确定业务含义时不要猜测，必要时向用户询问。",
                "",
                "### 3.1 数据源选择",
                "",
                "数据源必须优先依据知识图谱确定，而不是根据名称、经验或猜测选择。",
                "",
                "如果同一个业务概念存在多个数据源或多个数据表：",
                "",
                "1. 根据用户问题、业务规则和知识图谱中的映射关系判断。",
                "2. 如果仍无法确定，应继续查询知识图谱；必要时询问用户。",
                "",
                "选择数据源后，后续 SQL 默认在该数据源执行。",
                "",
                "SQL 执行失败时应优先检查 SQL 本身，只有发现当前数据源或表与知识图谱、用户意图明显不匹配时，才重新判断数据源。",
                "",
                "### 3.2 向量搜索",
                "",
                "由 Agent 自己判断是否需要使用 `vector_search` 进行语义搜索。",
                "",
                "* 如果通过 `name`、属性或关系可以准确定位，优先使用 `get_node` 或 `list_nodes`。",
                "* 如果用户使用自然语言描述、业务术语或语义相近但无法直接匹配的内容，应使用 `vector_search`。",
                "* 向量搜索只能用于辅助定位业务概念，找到候选节点后仍需通过 `traverse_relationships` 查询其关系，确定对应的 Table、Column 和 DataSource。",
                "",
                "## 4. SQL 生成",
                "",
                "生成 SQL 前必须明确：使用哪个 DataSource、查询哪些 Table / Column、查询条件、适用的 BusinessRule、Metric 的计算方式。",
                "",
                "只生成只读 SQL，禁止 INSERT、UPDATE、DELETE、MERGE、DROP、ALTER、TRUNCATE 等任何修改数据或数据库结构的操作。",
                "",
                "### 4.1 查询行数限制",
                "",
                "所有可能返回多行数据的 SELECT 查询，都必须主动加入行数限制，默认不超过 1000 行；如果用户问题只需要少量数据，应使用更小的限制。",
                "",
                "- SQL 本身已存在 LIMIT / TOP / FETCH FIRST 等限制，且最终结果不超过 1000 行时，保持原限制。",
                "- 聚合查询（例如 COUNT、SUM、AVG、GROUP BY）也应根据查询结果规模合理限制行数。",
                "- UNION、子查询等复杂查询，应确保最终返回结果受到合理的行数限制，而不是仅限制某个子查询。",
                "- 不要等 `execute_read_sql` 因行数过多而失败后再重新生成 SQL。",
                "",
                "## 5. SQL 权限与自动重写",
                "",
                "`execute_read_sql` 后端存在独立的数据权限控制机制，可能在 SQL 执行前自动进行数据源权限检查、表权限检查、字段权限检查、行级数据权限过滤、SQL 安全检查，以及查询条件自动追加或 SQL 重写。",
                "",
                "Agent 只负责生成符合业务语义的原始 SQL，**不要自行添加用户数据权限条件，也不要猜测当前用户的权限条件**。",
                "",
                "例如 Agent 生成：",
                "",
                "```sql",
                "SELECT * FROM orders WHERE status = 1 LIMIT 1000",
                "```",
                "",
                "系统可能自动重写为：",
                "",
                "```sql",
                "SELECT * FROM orders WHERE status = 1 AND department_id = 10 LIMIT 1000",
                "```",
                "",
                "这是正常的权限处理，不代表 Agent 的 SQL 有问题。",
                "",
                "Agent 不需要知道或预测系统最终追加的权限条件，不要因为不知道权限条件而修改 SQL，也不要重复添加权限条件。",
                "",
                "## 6. SQL 执行失败",
                "",
                "如果 `execute_read_sql` 返回 SQL 错误：根据错误信息检查 SQL，修正后重新执行，不要无依据地更换数据源或表。",
                "",
                "如果返回的是权限拒绝或权限相关错误：不要尝试绕过权限，不要更换数据源，不要修改权限条件，应根据权限错误调整查询，或者直接告知用户当前数据不可访问。",
                "",
                "如果 SQL 多次执行失败，直接告知用户，不要尝试使用沙箱、Python 或其他方式绕过 `execute_read_sql` 访问数据库。",
                "",
                "## 7. 结果分析",
                "",
                "SQL 返回结果后：根据 BusinessRule 检查结果，必要时进行补充查询，使用业务语言总结结果，不编造数据库中不存在的数据。",
                "",
                "如果查询结果与知识图谱中的业务定义明显冲突：不要修改知识图谱，优先检查 SQL 和查询条件；确认存在业务知识冲突时，应通过业务发现流程处理。",
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
                paths.addAll(fileReadResult.getData());
            }
        }
        return paths;
    }

}
