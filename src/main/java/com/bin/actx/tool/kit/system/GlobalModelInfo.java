package com.bin.actx.tool.kit.system;

import com.bin.actx.model.ChatModelConfig;
import com.bin.actx.model.EmbeddingModelConfig;
import java.util.Collection;
import lombok.Data;

/**
 * 模型配置信息，聚合了模型列表和供应商列表，供LLM了解当前模型全貌
 *
 * @author Bin
 * @since 2026/5/12 16:12
 */
@Data
public class GlobalModelInfo {

    /**
     * 已创建的模型配置列表
     */
    private Collection<ChatModelConfig> chatModelList;

    /**
     * 已创建的向量模型配置列表
     */
    private Collection<EmbeddingModelConfig> embeddingModelList;

    /**
     * 支持的供应商名称列表
     */
    private Collection<String> supportSupplierList;

}
