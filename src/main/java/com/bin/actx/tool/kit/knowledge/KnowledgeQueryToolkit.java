package com.bin.actx.tool.kit.knowledge;

import com.bin.actx.storage.graph.repository.GraphRepository;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.model.ToolCallResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 知识图谱查询工具包
 *
 * @author Bin
 * @since 2026/9/10
 */
@Toolkit(name = "知识查询", description = "查询知识图谱中的节点、关系和语义搜索", autoDiscover = false)
public class KnowledgeQueryToolkit {

    @Autowired
    private GraphRepository graphRepository;

    @Tool(name = "get_node", description = "按ID精确获取知识节点，返回节点的全部属性")
    public ToolCallResult<Map<String, Object>> getNode(
            @ToolParam(description = "节点唯一ID") String nodeId,
            @ToolParam(description = "节点类型标签，如 BusinessConcept、Table 等，不传则搜索所有 Knowledge 节点", required = false) String label
    ) {
        Map<String, Object> node = graphRepository.getNode(label, nodeId);
        if (node == null) {
            return ToolCallResult.of(null, "Node not found: " + nodeId);
        }
        return ToolCallResult.of(buildNodeResult(node));
    }

    @Tool(name = "list_nodes", description = "按类型列出知识节点，支持属性过滤、排序和分页")
    public ToolCallResult<Map<String, Object>> listNodes(
            @ToolParam(description = "节点类型标签，如 BusinessConcept、Table、Column、KnowledgeClaim 等") String label,
            @ToolParam(description = "属性精确匹配过滤，如 {\"status\": \"INFERRED\"}", required = false) Map<String, Object> filter,
            @ToolParam(description = "排序属性名，默认 name", required = false) String sortBy,
            @ToolParam(description = "是否升序，默认 true", required = false) Boolean ascending,
            @ToolParam(description = "跳过条数，默认 0", required = false) Integer skip,
            @ToolParam(description = "返回条数，默认 20，最大 100", required = false) Integer limit
    ) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "name";
        }
        if (ascending == null) {
            ascending = true;
        }
        if (skip == null) {
            skip = 0;
        }
        if (limit == null) {
            limit = 20;
        }
        limit = Math.min(limit, 100);

        List<Map<String, Object>> nodes = graphRepository.listNodes(label, filter, sortBy, ascending, skip, limit);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            results.add(buildNodeResult(node));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", results);
        result.put("count", results.size());
        return ToolCallResult.of(result);
    }

    @Tool(name = "vector_search", description = "语义向量搜索，通过自然语言描述查找语义相似的知识节点")
    public ToolCallResult<List<Map<String, Object>>> vectorSearch(
            @ToolParam(description = "自然语言查询文本") String query,
            @ToolParam(description = "限定搜索范围的节点类型，不传则搜索所有 Knowledge 节点", required = false) String label,
            @ToolParam(description = "返回条数，默认 10", required = false) Integer topK
    ) {
        if (topK == null) {
            topK = 10;
        }
        List<Map<String, Object>> nodes = graphRepository.vectorSearch(query, label, topK);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            Map<String, Object> item = buildNodeResult(node);
            item.put("score", node.get("score"));
            results.add(item);
        }
        return ToolCallResult.of(results);
    }

    @Tool(name = "traverse_relationships", description = "遍历节点的关系邻居，支持方向、关系类型、目标类型和深度过滤")
    public ToolCallResult<List<Map<String, Object>>> traverseRelationships(
            @ToolParam(description = "起始节点ID") String nodeId,
            @ToolParam(description = "遍历方向: outgoing（默认）、incoming、both", required = false) String direction,
            @ToolParam(description = "关系类型，如 REPRESENTED_BY、CONTAINS 等，不传则返回所有关系", required = false) String relationshipType,
            @ToolParam(description = "限定目标节点类型，如 Table、Column", required = false) String targetLabel,
            @ToolParam(description = "遍历深度，默认 1，最大 3", required = false) Integer depth,
            @ToolParam(description = "返回条数，默认 50", required = false) Integer limit
    ) {
        if (direction == null || direction.isEmpty()) {
            direction = "outgoing";
        }
        if (depth == null) {
            depth = 1;
        }
        depth = Math.min(depth, 3);
        if (limit == null) {
            limit = 50;
        }

        List<Map<String, Object>> edges = graphRepository.traverseRelationships(nodeId, direction, relationshipType, targetLabel, depth,
                limit);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> edge : edges) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("start_node", buildNodeResult((Map<String, Object>) edge.get("start_node")));
            result.put("relationship", edge.get("relationship"));
            result.put("end_node", buildNodeResult((Map<String, Object>) edge.get("end_node")));
            results.add(result);
        }
        return ToolCallResult.of(results);
    }

    /**
     * 构建节点返回结果，提取标签和属性
     */
    private Map<String, Object> buildNodeResult(Map<String, Object> node) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object labels = node.get("__labels__");
        if (labels instanceof List<?> labelList) {
            result.put("labels", labelList);
        }
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("__") || key.equals("embedding")) {
                continue;
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

}
