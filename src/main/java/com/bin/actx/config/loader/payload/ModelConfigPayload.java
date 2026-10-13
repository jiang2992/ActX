package com.bin.actx.config.loader.payload;

import java.util.List;
import lombok.Data;

/**
 * 模型配置载体类
 *
 * @author Bin
 * @since 2026/6/2 15:48
 */
@Data
public class ModelConfigPayload {

    /**
     * 模型配置列表
     */
    private List<ModelConfigItem> chatModels;

    /**
     * 向量模型配置列表
     */
    private List<EmbeddingModelConfigItem> embeddingModels;

    /**
     * 单个模型配置项
     */
    @Data
    public static class ModelConfigItem {

        /**
         * 配置id（唯一标识）
         */
        private String id;

        /**
         * 供应商名称
         */
        private String supplierName;

        /**
         * API基础地址
         */
        private String baseUrl;

        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 模型名称
         */
        private String modelName;

        /**
         * 温度参数
         */
        private Double temperature;

        /**
         * 最大Token数
         */
        private Integer maxTokens;

        /**
         * 是否启用思考
         */
        private Boolean enableThink;

        /**
         * 响应格式，默认text
         */
        private String responseFormat;

    }

    /**
     * 单个向量模型配置项
     */
    @Data
    public static class EmbeddingModelConfigItem {

        /**
         * 配置id（唯一标识）
         */
        private String id;

        /**
         * 供应商名称
         */
        private String supplierName;

        /**
         * API基础地址
         */
        private String baseUrl;

        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 模型名称
         */
        private String modelName;

        /**
         * 向量维度
         */
        private Integer dimensions;

    }

}
