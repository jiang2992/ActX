package com.bin.actx.tool.kit.knowledge;

import com.bin.actx.storage.graph.repository.GraphRepository;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.model.DeleteNodeParam;
import com.bin.actx.tool.model.DeleteNodePropertyParam;
import com.bin.actx.tool.model.DeleteRelationshipParam;
import com.bin.actx.tool.model.DeleteRelationshipPropertyParam;
import com.bin.actx.tool.model.KnowledgeNodeParam;
import com.bin.actx.tool.model.KnowledgeRelationshipParam;
import com.bin.actx.tool.model.ToolCallResult;
import com.bin.actx.tool.model.UpdateNodeParam;
import com.bin.actx.tool.model.UpdateRelationshipParam;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 知识图谱维护工具包
 *
 * @author Bin
 * @since 2026/9/10
 */
@Toolkit(name = "知识维护", description = "创建、更新和删除知识图谱中的节点和关系", autoDiscover = false)
public class KnowledgeMutateToolkit {

    public static final String VALID_LABELS_STRING = "BusinessDomain, BusinessConcept, BusinessTerm, BusinessProcess, BusinessRule, Metric, DataSource, Database, Schema, Table, Column, KnowledgeClaim";

    public static final Set<String> VALID_LABELS = Set.of(VALID_LABELS_STRING.split(", "));

    public static final String VALID_RELATIONSHIP_TYPES_STRING = "CONTAINS_CONCEPT, RELATED_TO, REPRESENTED_BY, HAS_RULE, REFERS_TO, INVOLVES, HAS_STEP, PRODUCES, APPLIES_TO, CALCULATED_FROM, SUBJECT_TO, USES_COLUMN, CONTAINS";

    public static final Set<String> VALID_RELATIONSHIP_TYPES = Set.of(VALID_RELATIONSHIP_TYPES_STRING.split(", "));

    @Autowired
    private GraphRepository graphRepository;

    @Tool(name = "merge_nodes", description = "批量创建或更新知识节点")
    public ToolCallResult<Map<String, Object>> mergeNodes(
            @ToolParam(description = "节点列表") List<KnowledgeNodeParam> nodes
    ) {
        if (nodes == null || nodes.isEmpty()) {
            return ToolCallResult.of(null, "nodes cannot be empty");
        }
        for (KnowledgeNodeParam node : nodes) {
            if (!VALID_LABELS.contains(node.getLabel())) {
                return ToolCallResult.of(null, "Invalid label: " + node.getLabel() + ". Valid labels: " + VALID_LABELS);
            }
        }

        Map<String, Object> result = graphRepository.mergeNodes(nodes);

        return ToolCallResult.of(result);
    }

    @Tool(name = "delete_nodes", description = "批量删除知识节点及其所有关系")
    public ToolCallResult<Map<String, Object>> deleteNodes(
            @ToolParam(description = "节点列表") List<DeleteNodeParam> nodes
    ) {
        if (nodes == null || nodes.isEmpty()) {
            return ToolCallResult.of(null, "nodes cannot be empty");
        }

        int deleted = graphRepository.deleteNodes(nodes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted_count", deleted);
        return ToolCallResult.of(result);
    }

    @Tool(name = "update_node_properties", description = "批量更新知识节点的属性，仅更新传入的字段")
    public ToolCallResult<Map<String, Object>> updateNodeProperties(
            @ToolParam(description = "节点列表") List<UpdateNodeParam> nodes
    ) {
        if (nodes == null || nodes.isEmpty()) {
            return ToolCallResult.of(null, "nodes cannot be empty");
        }

        int updated = graphRepository.updateNodeProperties(nodes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated_count", updated);
        return ToolCallResult.of(result);
    }

    @Tool(name = "delete_node_properties", description = "批量删除知识节点的指定属性")
    public ToolCallResult<Map<String, Object>> deleteNodeProperties(
            @ToolParam(description = "节点列表") List<DeleteNodePropertyParam> nodes
    ) {
        if (nodes == null || nodes.isEmpty()) {
            return ToolCallResult.of(null, "nodes cannot be empty");
        }

        int updated = graphRepository.deleteNodeProperties(nodes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated_count", updated);
        return ToolCallResult.of(result);
    }

    @Tool(name = "create_relationships", description = "批量创建知识节点之间的关系")
    public ToolCallResult<Map<String, Object>> createRelationships(
            @ToolParam(description = "关系列表") List<KnowledgeRelationshipParam> relationships
    ) {
        if (relationships == null || relationships.isEmpty()) {
            return ToolCallResult.of(null, "relationships cannot be empty");
        }
        for (KnowledgeRelationshipParam rel : relationships) {
            if (!VALID_RELATIONSHIP_TYPES.contains(rel.getRelationshipType())) {
                return ToolCallResult.of(null,
                        "Invalid relationship type: " + rel.getRelationshipType() + ". Valid types: " + VALID_RELATIONSHIP_TYPES);
            }
        }

        int count = graphRepository.createRelationships(relationships);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created_count", count);
        return ToolCallResult.of(result);
    }

    @Tool(name = "delete_relationships", description = "批量删除知识节点之间的关系")
    public ToolCallResult<Map<String, Object>> deleteRelationships(
            @ToolParam(description = "关系列表") List<DeleteRelationshipParam> relationships
    ) {
        if (relationships == null || relationships.isEmpty()) {
            return ToolCallResult.of(null, "relationships cannot be empty");
        }
        for (DeleteRelationshipParam rel : relationships) {
            if (!VALID_RELATIONSHIP_TYPES.contains(rel.getRelationshipType())) {
                return ToolCallResult.of(null, "Invalid relationship type: " + rel.getRelationshipType());
            }
        }

        int count = graphRepository.deleteRelationships(relationships);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted_count", count);
        return ToolCallResult.of(result);
    }

    @Tool(name = "update_relationship_properties", description = "批量更新知识节点之间关系的属性")
    public ToolCallResult<Map<String, Object>> updateRelationshipProperties(
            @ToolParam(description = "关系列表") List<UpdateRelationshipParam> relationships
    ) {
        if (relationships == null || relationships.isEmpty()) {
            return ToolCallResult.of(null, "relationships cannot be empty");
        }
        for (UpdateRelationshipParam rel : relationships) {
            if (!VALID_RELATIONSHIP_TYPES.contains(rel.getRelationshipType())) {
                return ToolCallResult.of(null, "Invalid relationship type: " + rel.getRelationshipType());
            }
        }

        int count = graphRepository.updateRelationshipProperties(relationships);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated_count", count);
        return ToolCallResult.of(result);
    }

    @Tool(name = "delete_relationship_properties", description = "批量删除知识节点之间关系的指定属性")
    public ToolCallResult<Map<String, Object>> deleteRelationshipProperties(
            @ToolParam(description = "关系列表") List<DeleteRelationshipPropertyParam> relationships
    ) {
        if (relationships == null || relationships.isEmpty()) {
            return ToolCallResult.of(null, "relationships cannot be empty");
        }
        for (DeleteRelationshipPropertyParam rel : relationships) {
            if (!VALID_RELATIONSHIP_TYPES.contains(rel.getRelationshipType())) {
                return ToolCallResult.of(null, "Invalid relationship type: " + rel.getRelationshipType());
            }
        }

        int count = graphRepository.deleteRelationshipProperties(relationships);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated_count", count);
        return ToolCallResult.of(result);
    }

}
