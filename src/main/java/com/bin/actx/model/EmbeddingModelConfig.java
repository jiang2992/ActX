package com.bin.actx.model;

import lombok.Data;

/**
 * 嵌入模型配置
 *
 * @author Bin
 * @since 2026/9/4 14:23
 */
@Data
public class EmbeddingModelConfig {

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
     * 嵌入向量的维度
     */
    private Integer dimensions;

}
