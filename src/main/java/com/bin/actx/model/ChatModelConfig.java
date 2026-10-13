package com.bin.actx.model;

import lombok.Data;

/**
 * 模型配置，包含创建ChatClient所需的供应商、连接和参数信息
 */
@Data
public class ChatModelConfig {

    /**
     * 模型唯一标识，创建时自动生成
     */
    private String id;

    /**
     * 供应商名称，对应ModelFactory的名称
     */
    private String supplierName;

    /**
     * 供应商API基础地址，为null时使用官方默认地址
     */
    private String baseUrl;

    /**
     * 供应商API密钥
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 温度参数，控制生成随机性
     */
    private Double temperature;

    /**
     * 最大上下文窗口
     */
    private Integer maxTokens;

    /**
     * 是否允许思考
     */
    private Boolean enableThink;

    /**
     * 响应格式，默认text
     */
    private String responseFormat;

}
