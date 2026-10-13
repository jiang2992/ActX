package com.bin.actx.tool.model;

import java.util.List;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 删除节点属性参数
 *
 * @author Bin
 * @since 2026/9/10
 */
@Data
public class DeleteNodePropertyParam {

    @ToolParam(description = "节点ID")
    private String id;

    @ToolParam(description = "要删除的属性名列表")
    private List<String> propertyKeys;

}
