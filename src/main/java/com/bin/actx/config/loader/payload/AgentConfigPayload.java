package com.bin.actx.config.loader.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * Agent配置载体类
 *
 * @author Bin
 * @since 2026/6/2 15:48
 */
@Data
public class AgentConfigPayload {

    /**
     * 配置id（唯一标识）
     */
    private String id;

    /**
     * Agent名称
     */
    private String name;

    /**
     * Agent描述
     */
    private String description;

    /**
     * 使用的模型code（需要在加载时转换为modelId）
     */
    private String model;

    /**
     * 系统提示词（与systemPromptFile二选一，systemPromptFile优先级更高）
     */
    private String systemPrompt;

    /**
     * 系统提示词文件路径（配置后读取文件内容作为系统提示词）
     */
    private String systemPromptFile;

    /**
     * 是否私有化
     */
    private Boolean privatize;

    /**
     * 工具包
     */
    private String toolkits;

    /**
     * 工具模式（支持 static、dynamic，默认 static）
     */
    private String toolMode;

    /**
     * 插件配置列表（默认装配基础插件与工具插件，其余插件在此配置）
     */
    private List<PluginConfigPayload> plugins;

    /**
     * 插件配置
     */
    @Data
    public static class PluginConfigPayload {

        /**
         * 插件类名
         */
        @JsonProperty("class")
        private String className;

        /**
         * 插件元数据
         */
        private Map<String, Object> metadata;

    }

}
