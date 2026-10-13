package com.bin.actx.tool.model;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 删除关系参数
 *
 * @author Bin
 * @since 2026/9/10
 */
@Data
public class DeleteRelationshipParam {

    @ToolParam(description = "关系类型")
    private String relationshipType;

    @ToolParam(description = "起点节点ID")
    private String fromNodeId;

    @ToolParam(description = "终点节点ID")
    private String toNodeId;

}
