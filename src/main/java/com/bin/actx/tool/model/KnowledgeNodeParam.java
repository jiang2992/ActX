package com.bin.actx.tool.model;

import com.bin.actx.tool.kit.knowledge.KnowledgeMutateToolkit;
import java.util.Map;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 知识节点创建/更新参数
 *
 * @author Bin
 * @since 2026/9/10
 */
@Data
public class KnowledgeNodeParam {

    @ToolParam(description = "节点类型标签: " + KnowledgeMutateToolkit.VALID_LABELS_STRING)
    private String label;

    @ToolParam(description = "节点名称")
    private String name;

    @ToolParam(description = "节点描述")
    private String description;

    @ToolParam(description = "节点唯一ID（选填，不传则系统自动生成）", required = false)
    private String id;

    @ToolParam(description = "自定义属性", required = false)
    private Map<String, Object> properties;

}
