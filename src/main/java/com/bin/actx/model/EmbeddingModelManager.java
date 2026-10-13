package com.bin.actx.model;

import com.bin.actx.exception.ModelConfigException;
import com.bin.actx.model.factory.ModelFactory;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingModelManager {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 模型工厂映射：供应商标识 → 工厂实例
     */
    private final Map<String, ModelFactory> modelFactoryMap = new ConcurrentHashMap<>();

    /**
     * 向量模型模型客户端映射：向量模型id → 向量模型客户端
     */
    private final Map<String, EmbeddingModel> modelMap = new ConcurrentHashMap<>();

    /**
     * 向量模型配置映射：模型id → 向量模型配置
     */
    private final Map<String, EmbeddingModelConfig> configMapper = new ConcurrentHashMap<>();

    public void register() {
        // 自动发现并注册所有ChatModelFactory实现
        Map<String, ModelFactory> beans = applicationContext.getBeansOfType(ModelFactory.class);
        beans.forEach((name, bean) -> modelFactoryMap.put(bean.getName(), bean));
    }

    /**
     * 添加向量模型配置
     *
     * @param embeddingModelConfig 向量模型配置
     * @return 模型id
     */
    public String addConfig(EmbeddingModelConfig embeddingModelConfig) {
        ModelFactory modelFactory = modelFactoryMap.get(embeddingModelConfig.getSupplierName());
        if (modelFactory == null) {
            throw new ModelConfigException("Unsupported supplier.");
        }

        String modelId = embeddingModelConfig.getId() != null ? embeddingModelConfig.getId() : IdUtils.uuid();
        embeddingModelConfig.setId(modelId);
        this.configMapper.put(modelId, embeddingModelConfig);

        return modelId;
    }

    public EmbeddingModelConfig getConfig(String modelId) {
        return this.configMapper.get(modelId);
    }

    public EmbeddingModel getModel(String modelId) {
        EmbeddingModel embeddingModel = this.modelMap.get(modelId);
        if (embeddingModel != null) {
            return embeddingModel;
        }
        EmbeddingModelConfig modelConfig = this.configMapper.get(modelId);
        if (modelConfig == null) {
            return null;
        }
        EmbeddingModel model = this.createModel(modelConfig);
        this.modelMap.put(modelId, model);
        return model;
    }

    public EmbeddingModel getFirstModel() {
        if (this.configMapper.isEmpty()) {
            return null;
        }
        return this.getModel(this.configMapper.keySet().iterator().next());
    }

    private EmbeddingModel createModel(EmbeddingModelConfig modelConfig) {
        ModelFactory modelFactory = modelFactoryMap.get(modelConfig.getSupplierName());
        return modelFactory.getEmbeddingModel(modelConfig);
    }

    /**
     * 获取所有已创建的向量模型配置列表
     */
    public Collection<EmbeddingModelConfig> getConfigList() {
        return this.configMapper.values();
    }

}
