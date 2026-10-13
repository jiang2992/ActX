package com.bin.actx.storage.graph.repository;

import com.bin.actx.embedding.TextEmbeddingService;
import com.bin.actx.tool.model.DeleteNodeParam;
import com.bin.actx.tool.model.DeleteNodePropertyParam;
import com.bin.actx.tool.model.DeleteRelationshipParam;
import com.bin.actx.tool.model.DeleteRelationshipPropertyParam;
import com.bin.actx.tool.model.KnowledgeNodeParam;
import com.bin.actx.tool.model.KnowledgeRelationshipParam;
import com.bin.actx.tool.model.UpdateNodeParam;
import com.bin.actx.tool.model.UpdateRelationshipParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 知识图谱仓储
 *
 * @author Bin
 * @since 2026/9/10
 */
@Component
public class GraphRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private TextEmbeddingService textEmbeddingService;

    /**
     * 执行 Cypher 表达式并返回执行结果
     *
     * @param cypher Cypher 表达式
     * @param params 参数
     * @return 执行结果记录列表，每条记录为字段名到字段值的映射
     */
    public List<Map<String, Object>> execute(String cypher, Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value == null) {
                continue;
            }
            if (key.startsWith("__embedding__")) {
                String text = value.toString();
                float[] embedding = textEmbeddingService.embed(text);
                params.put(key, embedding);
            }
        }
        try (Session session = driver.session()) {
            return session.run(cypher, params).stream()
                    .map(Record::asMap)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取单个节点
     */
    public Map<String, Object> getNode(String label, String nodeId) {
        String cypher;
        Map<String, Object> params = Map.of("id", nodeId);
        if (label != null && !label.isEmpty()) {
            cypher = "MATCH (n:Knowledge:" + label + " {id: $id}) RETURN labels(n) AS __labels__, properties(n) AS __props__";
        } else {
            cypher = "MATCH (n:Knowledge {id: $id}) RETURN labels(n) AS __labels__, properties(n) AS __props__";
        }
        try (Session session = driver.session()) {
            List<Record> records = session.run(cypher, params).list();
            if (records.isEmpty()) {
                return null;
            }
            Record record = records.get(0);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("__labels__", record.get("__labels__").asList());
            result.putAll(record.get("__props__").asMap());
            return result;
        }
    }

    /**
     * 列出节点（支持过滤、排序、分页）
     */
    public List<Map<String, Object>> listNodes(String label, Map<String, Object> filters, String sortBy, boolean ascending, int skip, int limit) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        if (filters != null && !filters.isEmpty()) {
            List<String> conditions = new ArrayList<>();
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String key = "_f_" + entry.getKey();
                conditions.add("n." + entry.getKey() + " = $" + key);
                params.put(key, entry.getValue());
            }
            where.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        String direction = ascending ? "ASC" : "DESC";
        params.put("__skip", skip);
        params.put("__limit", limit);

        String cypher = "MATCH (n:Knowledge:" + label + ")" + where +
                " RETURN labels(n) AS __labels__, properties(n) AS __props__" +
                " ORDER BY n." + sortBy + " " + direction +
                " SKIP $__skip LIMIT $__limit";

        try (Session session = driver.session()) {
            return session.run(cypher, params).stream()
                    .map(record -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("__labels__", record.get("__labels__").asList());
                        result.putAll(record.get("__props__").asMap());
                        return result;
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * 向量搜索
     */
    public List<Map<String, Object>> vectorSearch(String queryText, String label, int topK) {
        float[] embedding = textEmbeddingService.embed(queryText);

        StringBuilder cypher = new StringBuilder();
        cypher.append("CALL db.index.vector.queryNodes('knowledge_embedding_index', $topK, $embedding) YIELD node, score ");
        if (label != null && !label.isEmpty()) {
            cypher.append("WHERE node:").append(label).append(" ");
        }
        cypher.append("RETURN labels(node) AS __labels__, properties(node) AS __props__, score");

        Map<String, Object> params = Map.of("topK", topK, "embedding", embedding);

        try (Session session = driver.session()) {
            return session.run(cypher.toString(), params).stream()
                    .map(record -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("__labels__", record.get("__labels__").asList());
                        result.putAll(record.get("__props__").asMap());
                        result.put("score", record.get("score").asDouble());
                        return result;
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * 遍历关系
     */
    public List<Map<String, Object>> traverseRelationships(String nodeId, String direction, String relationshipType, String targetLabel, int depth, int limit) {
        String dirPattern = switch (direction) {
            case "incoming" -> "<-[r:%s]-";
            case "both" -> "-[r:%s]-";
            default -> "-[r:%s]->";
        };

        String relFilter = (relationshipType != null && !relationshipType.isEmpty()) ? relationshipType : "";
        String relPattern = String.format(dirPattern, relFilter.isEmpty() ? "" : relFilter);
        if (relFilter.isEmpty()) {
            relPattern = switch (direction) {
                case "incoming" -> "<-[r]-";
                case "both" -> "-[r]-";
                default -> "-[r]->";
            };
        }

        Set<String> visitedNodes = new HashSet<>();
        visitedNodes.add(nodeId);
        List<Map<String, Object>> allResults = new ArrayList<>();
        Set<String> currentFrontier = new HashSet<>();
        currentFrontier.add(nodeId);

        for (int d = 0; d < depth && !currentFrontier.isEmpty() && allResults.size() < limit; d++) {
            StringBuilder cypher = new StringBuilder();
            cypher.append("MATCH (start:Knowledge) WHERE start.id IN $frontierIds ");
            cypher.append("MATCH (start)").append(relPattern).append("(end:Knowledge) ");
            if (targetLabel != null && !targetLabel.isEmpty()) {
                cypher.append("WHERE end:").append(targetLabel).append(" ");
            }
            cypher.append("RETURN start.id AS startId, labels(start) AS startLabels, properties(start) AS startProps, ");
            cypher.append("type(r) AS relType, properties(r) AS relProps, ");
            cypher.append("end.id AS endId, labels(end) AS endLabels, properties(end) AS endProps ");
            cypher.append("LIMIT $limit");

            Map<String, Object> params = Map.of("frontierIds", new ArrayList<>(currentFrontier), "limit", limit - allResults.size());

            try (Session session = driver.session()) {
                List<Record> records = session.run(cypher.toString(), params).list();
                Set<String> nextFrontier = new HashSet<>();

                for (Record record : records) {
                    String startId = record.get("startId").asString();
                    String endId = record.get("endId").asString();
                    String edgeKey = startId + "|" + record.get("relType").asString() + "|" + endId;

                    if (!visitedNodes.contains(endId) || allResults.stream().noneMatch(r -> edgeKey.equals(r.get("edgeKey")))) {
                        Map<String, Object> result = new LinkedHashMap<>();
                        Map<String, Object> startNode = new LinkedHashMap<>();
                        startNode.put("id", startId);
                        startNode.put("__labels__", record.get("startLabels").asList());
                        startNode.putAll(record.get("startProps").asMap());

                        Map<String, Object> endNode = new LinkedHashMap<>();
                        endNode.put("id", endId);
                        endNode.put("__labels__", record.get("endLabels").asList());
                        endNode.putAll(record.get("endProps").asMap());

                        result.put("start_node", startNode);
                        result.put("relationship", Map.of("type", record.get("relType").asString(), "properties", record.get("relProps").asMap()));
                        result.put("end_node", endNode);
                        result.put("edgeKey", edgeKey);

                        allResults.add(result);
                        nextFrontier.add(endId);
                        visitedNodes.add(endId);
                    }
                }

                currentFrontier = nextFrontier;
            }
        }

        return allResults;
    }

    /**
     * 批量创建或全量更新节点（按 label 分组执行）
     */
    public Map<String, Object> mergeNodes(List<KnowledgeNodeParam> nodes) {
        Map<String, List<KnowledgeNodeParam>> grouped = nodes.stream()
                .collect(Collectors.groupingBy(KnowledgeNodeParam::getLabel));

        List<String> allIds = new ArrayList<>();
        int totalMerged = 0;

        for (Map.Entry<String, List<KnowledgeNodeParam>> entry : grouped.entrySet()) {
            String label = entry.getKey();
            List<KnowledgeNodeParam> labelNodes = entry.getValue();

            List<Map<String, Object>> nodeDataList = new ArrayList<>();
            for (KnowledgeNodeParam node : labelNodes) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", node.getId() != null ? node.getId() : UUID.randomUUID().toString());
                data.put("name", node.getName());
                data.put("description", node.getDescription());
                String embeddingText = node.getName() + ". " + node.getDescription();
                data.put("embedding", textEmbeddingService.embed(embeddingText));
                if (node.getProperties() != null) {
                    data.putAll(node.getProperties());
                }
                nodeDataList.add(data);
            }

            String cypher = "UNWIND $nodes AS nodeData " +
                    "MERGE (n:Knowledge:" + label + " {id: nodeData.id}) " +
                    "SET n = nodeData " +
                    "RETURN n.id AS id";

            Map<String, Object> params = Map.of("nodes", nodeDataList);

            try (Session session = driver.session()) {
                List<String> ids = session.run(cypher, params).stream()
                        .map(record -> record.get("id").asString())
                        .collect(Collectors.toList());
                allIds.addAll(ids);
                totalMerged += ids.size();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("merged_count", totalMerged);
        result.put("ids", allIds);
        return result;
    }

    /**
     * 批量删除节点（DETACH DELETE）
     */
    public int deleteNodes(List<DeleteNodeParam> nodes) {
        List<String> ids = nodes.stream().map(DeleteNodeParam::getId).collect(Collectors.toList());
        String cypher = "UNWIND $ids AS id MATCH (n:Knowledge {id: id}) DETACH DELETE n RETURN count(n) AS deleted";
        Map<String, Object> params = Map.of("ids", ids);

        try (Session session = driver.session()) {
            return session.run(cypher, params).single().get("deleted").asInt();
        }
    }

    /**
     * 批量增量更新节点属性
     */
    public int updateNodeProperties(List<UpdateNodeParam> nodes) {
        // 查询需要更新 embedding 的节点的当前 name/description
        List<String> nodeIds = nodes.stream()
                .filter(n -> n.getName() != null || n.getDescription() != null)
                .map(UpdateNodeParam::getId)
                .collect(Collectors.toList());

        Map<String, Map<String, Object>> existingNodes = new HashMap<>();
        if (!nodeIds.isEmpty()) {
            String queryCypher = "MATCH (n:Knowledge) WHERE n.id IN $ids RETURN n.id AS id, n.name AS name, n.description AS description";
            try (Session session = driver.session()) {
                session.run(queryCypher, Map.of("ids", nodeIds)).forEachRemaining(record -> {
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("name", record.get("name").isNull() ? null : record.get("name").asString());
                    nodeInfo.put("description", record.get("description").isNull() ? null : record.get("description").asString());
                    existingNodes.put(record.get("id").asString(), nodeInfo);
                });
            }
        }

        // 构建更新数据，在 Java 侧计算 embedding
        List<Map<String, Object>> nodeDataList = new ArrayList<>();
        for (UpdateNodeParam node : nodes) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", node.getId());
            data.put("name", node.getName());
            data.put("description", node.getDescription());
            data.put("properties", node.getProperties());

            if (node.getName() != null || node.getDescription() != null) {
                Map<String, Object> existing = existingNodes.getOrDefault(node.getId(), Map.of());
                String currentName = node.getName() != null ? node.getName() : (String) existing.get("name");
                String currentDesc = node.getDescription() != null ? node.getDescription() : (String) existing.get("description");
                String embeddingText = (currentName != null ? currentName : "") + ". " + (currentDesc != null ? currentDesc : "");
                data.put("embedding", textEmbeddingService.embed(embeddingText));
            }

            nodeDataList.add(data);
        }

        String cypher = "UNWIND $nodes AS nodeData " +
                "MATCH (n:Knowledge {id: nodeData.id}) " +
                "SET n += CASE WHEN nodeData.properties IS NOT NULL THEN nodeData.properties ELSE {} END " +
                "WITH n, nodeData " +
                "SET n += CASE WHEN nodeData.name IS NOT NULL THEN {name: nodeData.name} ELSE {} END, " +
                "     n += CASE WHEN nodeData.description IS NOT NULL THEN {description: nodeData.description} ELSE {} END " +
                "WITH n, nodeData " +
                "WHERE nodeData.embedding IS NOT NULL " +
                "SET n.embedding = nodeData.embedding " +
                "RETURN count(n) AS updated";

        Map<String, Object> params = Map.of("nodes", nodeDataList);

        try (Session session = driver.session()) {
            return session.run(cypher, params).single().get("updated").asInt();
        }
    }

    /**
     * 批量删除节点属性
     */
    public int deleteNodeProperties(List<DeleteNodePropertyParam> nodes) {
        String cypher = "UNWIND $nodes AS nodeData " +
                "MATCH (n:Knowledge {id: nodeData.id}) " +
                "WITH n, nodeData.propertyKeys AS keys " +
                "CALL { WITH n, keys UNWIND keys AS key SET n[key] = null } " +
                "RETURN count(n) AS updated";

        List<Map<String, Object>> nodeDataList = new ArrayList<>();
        for (DeleteNodePropertyParam node : nodes) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", node.getId());
            data.put("propertyKeys", node.getPropertyKeys());
            nodeDataList.add(data);
        }

        Map<String, Object> params = Map.of("nodes", nodeDataList);

        try (Session session = driver.session()) {
            return session.run(cypher, params).single().get("updated").asInt();
        }
    }

    /**
     * 批量创建关系（按 relationshipType 分组执行）
     */
    public int createRelationships(List<KnowledgeRelationshipParam> relationships) {
        Map<String, List<KnowledgeRelationshipParam>> grouped = relationships.stream()
                .collect(Collectors.groupingBy(KnowledgeRelationshipParam::getRelationshipType));

        int totalCount = 0;

        for (Map.Entry<String, List<KnowledgeRelationshipParam>> entry : grouped.entrySet()) {
            String relType = entry.getKey();
            List<KnowledgeRelationshipParam> rels = entry.getValue();

            List<Map<String, Object>> relDataList = new ArrayList<>();
            for (KnowledgeRelationshipParam rel : rels) {
                Map<String, Object> data = new HashMap<>();
                data.put("fromNodeId", rel.getFromNodeId());
                data.put("toNodeId", rel.getToNodeId());
                data.put("properties", rel.getProperties() != null ? rel.getProperties() : Map.of());
                relDataList.add(data);
            }

            String cypher = "UNWIND $rels AS relData " +
                    "MATCH (a:Knowledge {id: relData.fromNodeId}) " +
                    "MATCH (b:Knowledge {id: relData.toNodeId}) " +
                    "MERGE (a)-[r:" + relType + "]->(b) " +
                    "SET r += relData.properties " +
                    "RETURN count(r) AS count";

            Map<String, Object> params = Map.of("rels", relDataList);

            try (Session session = driver.session()) {
                totalCount += session.run(cypher, params).single().get("count").asInt();
            }
        }

        return totalCount;
    }

    /**
     * 批量删除关系
     */
    public int deleteRelationships(List<DeleteRelationshipParam> relationships) {
        String cypher = "UNWIND $rels AS relData " +
                "MATCH (a:Knowledge {id: relData.fromNodeId})-[r]->(b:Knowledge {id: relData.toNodeId}) " +
                "WHERE type(r) = relData.relationshipType " +
                "DELETE r " +
                "RETURN count(r) AS deleted";

        List<Map<String, Object>> relDataList = new ArrayList<>();
        for (DeleteRelationshipParam rel : relationships) {
            Map<String, Object> data = new HashMap<>();
            data.put("fromNodeId", rel.getFromNodeId());
            data.put("toNodeId", rel.getToNodeId());
            data.put("relationshipType", rel.getRelationshipType());
            relDataList.add(data);
        }

        Map<String, Object> params = Map.of("rels", relDataList);

        try (Session session = driver.session()) {
            return session.run(cypher, params).single().get("deleted").asInt();
        }
    }

    /**
     * 批量更新关系属性
     */
    public int updateRelationshipProperties(List<UpdateRelationshipParam> relationships) {
        String cypher = "UNWIND $rels AS relData " +
                "MATCH (a:Knowledge {id: relData.fromNodeId})-[r]->(b:Knowledge {id: relData.toNodeId}) " +
                "WHERE type(r) = relData.relationshipType " +
                "SET r += relData.properties " +
                "RETURN count(r) AS updated";

        List<Map<String, Object>> relDataList = new ArrayList<>();
        for (UpdateRelationshipParam rel : relationships) {
            Map<String, Object> data = new HashMap<>();
            data.put("fromNodeId", rel.getFromNodeId());
            data.put("toNodeId", rel.getToNodeId());
            data.put("relationshipType", rel.getRelationshipType());
            data.put("properties", rel.getProperties());
            relDataList.add(data);
        }

        Map<String, Object> params = Map.of("rels", relDataList);

        try (Session session = driver.session()) {
            return session.run(cypher, params).single().get("updated").asInt();
        }
    }

    /**
     * 批量删除关系属性
     */
    public int deleteRelationshipProperties(List<DeleteRelationshipPropertyParam> relationships) {
        String cypher = "UNWIND $rels AS relData " +
                "MATCH (a:Knowledge {id: relData.fromNodeId})-[r]->(b:Knowledge {id: relData.toNodeId}) " +
                "WHERE type(r) = relData.relationshipType " +
                "WITH r, relData.propertyKeys AS keys " +
                "CALL { WITH r, keys UNWIND keys AS key SET r[key] = null } " +
                "RETURN count(r) AS updated";

        List<Map<String, Object>> relDataList = new ArrayList<>();
        for (DeleteRelationshipPropertyParam rel : relationships) {
            Map<String, Object> data = new HashMap<>();
            data.put("fromNodeId", rel.getFromNodeId());
            data.put("toNodeId", rel.getToNodeId());
            data.put("relationshipType", rel.getRelationshipType());
            data.put("propertyKeys", rel.getPropertyKeys());
            relDataList.add(data);
        }

        Map<String, Object> params = Map.of("rels", relDataList);

        try (Session session = driver.session()) {
            return session.run(cypher, params).single().get("updated").asInt();
        }
    }

}
