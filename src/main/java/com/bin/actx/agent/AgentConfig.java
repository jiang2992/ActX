package com.bin.actx.agent;

import com.bin.actx.agent.definition.AgentDefinition;
import lombok.Data;

/**
 * Agent 配置
 *
 * @author Bin
 * @since 2026/5/9 17:26
 */
@Data
public class AgentConfig {

    /**
     * Agent 配置唯一标识
     */
    private String id;

    /**
     * 使用的模型id
     */
    private String modelId;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否私有化
     */
    private Boolean privatize;

    /**
     * Agent 定义
     */
    private AgentDefinition definition;

}
