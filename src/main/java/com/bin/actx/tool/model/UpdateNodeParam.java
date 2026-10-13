package com.bin.actx.tool.model;

import java.util.Map;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 节点属性更新参数
 *
 * @author Bin
 * @since 2026/9/10
 */
@Data
public class UpdateNodeParam {

    @ToolParam(description = "节点ID")
    private String id;

    @ToolParam(description = "节点名称", required = false)
    private String name;

    @ToolParam(description = "节点描述", required = false)
    private String description;

    @ToolParam(description = "自定义属性（仅更新传入的属性）", required = false)
    private Map<String, Object> properties;

}
