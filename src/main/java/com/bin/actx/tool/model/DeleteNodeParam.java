package com.bin.actx.tool.model;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 删除节点参数
 *
 * @author Bin
 * @since 2026/9/10
 */
@Data
public class DeleteNodeParam {

    @ToolParam(description = "节点ID")
    private String id;

}
