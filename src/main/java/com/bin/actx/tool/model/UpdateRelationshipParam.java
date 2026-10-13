package com.bin.actx.tool.model;

import java.util.Map;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 更新关系属性参数
 *
 * @author Bin
 * @since 2026/9/10
 */
@Data
public class UpdateRelationshipParam {

    @ToolParam(description = "关系类型: CONTAINS_CONCEPT, RELATED_TO, REPRESENTED_BY, HAS_RULE, REFERS_TO, INVOLVES, HAS_STEP, PRODUCES, APPLIES_TO, CALCULATED_FROM, SUBJECT_TO, USES_COLUMN, CONTAINS")
    private String relationshipType;

    @ToolParam(description = "起点节点ID")
    private String fromNodeId;

    @ToolParam(description = "终点节点ID")
    private String toNodeId;

    @ToolParam(description = "要更新的属性")
    private Map<String, Object> properties;

}
