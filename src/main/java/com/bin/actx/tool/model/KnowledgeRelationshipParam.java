package com.bin.actx.tool.model;

import com.bin.actx.tool.kit.knowledge.KnowledgeMutateToolkit;
import java.util.Map;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 知识关系创建参数
 *
 * @author Bin
 * @since 2026/9/10
 */
@Data
public class KnowledgeRelationshipParam {

    @ToolParam(description = "关系类型: " + KnowledgeMutateToolkit.VALID_RELATIONSHIP_TYPES_STRING)
    private String relationshipType;

    @ToolParam(description = "起点节点ID")
    private String fromNodeId;

    @ToolParam(description = "终点节点ID")
    private String toNodeId;

    @ToolParam(description = "关系属性", required = false)
    private Map<String, Object> properties;

}
