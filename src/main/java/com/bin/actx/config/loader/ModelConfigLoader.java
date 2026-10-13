package com.bin.actx.config.loader;

import com.bin.actx.config.loader.payload.ModelConfigPayload;
import com.bin.actx.model.ChatModelConfig;
import com.bin.actx.model.EmbeddingModelConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * 模型配置读取器，用于从 JSON 文件中加载模型配置
 *
 * @author Bin
 * @since 2026/6/2 15:48
 */
@Component
public class ModelConfigLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从指定的 JSON 文件中加载模型配置
     *
     * @param path 配置文件路径
     * @return 模型配置列表
     * @throws IOException 文件读取失败时抛出
     */
    public Result load(String path) throws IOException {
        File configFile = new File(path);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        // 读取JSON文件
        ModelConfigPayload modelConfigPayload = objectMapper.readValue(configFile, ModelConfigPayload.class);

        // 转换模型配置
        List<ChatModelConfig> chatModelConfigList = new ArrayList<>(modelConfigPayload.getChatModels().size());
        if (modelConfigPayload.getChatModels() != null) {
            for (ModelConfigPayload.ModelConfigItem item : modelConfigPayload.getChatModels()) {
                ChatModelConfig chatModelConfig = convertToModelConfig(item);
                chatModelConfigList.add(chatModelConfig);
            }
        }

        List<EmbeddingModelConfig> embeddingModelConfigList = new ArrayList<>(modelConfigPayload.getEmbeddingModels().size());
        if (modelConfigPayload.getEmbeddingModels() != null) {
            for (ModelConfigPayload.EmbeddingModelConfigItem item : modelConfigPayload.getEmbeddingModels()) {
                EmbeddingModelConfig embeddingModelConfig = convertToEmbeddingModelConfig(item);
                embeddingModelConfigList.add(embeddingModelConfig);
            }
        }

        return new Result(chatModelConfigList, embeddingModelConfigList);
    }

    private ChatModelConfig convertToModelConfig(ModelConfigPayload.ModelConfigItem item) {
        ChatModelConfig chatModelConfig = new ChatModelConfig();
        chatModelConfig.setId(item.getId());
        chatModelConfig.setSupplierName(item.getSupplierName());
        chatModelConfig.setBaseUrl(item.getBaseUrl());
        chatModelConfig.setApiKey(item.getApiKey());
        chatModelConfig.setModelName(item.getModelName());
        chatModelConfig.setTemperature(item.getTemperature());
        chatModelConfig.setMaxTokens(item.getMaxTokens());
        chatModelConfig.setEnableThink(item.getEnableThink());
        chatModelConfig.setResponseFormat(item.getResponseFormat());
        return chatModelConfig;
    }

    private EmbeddingModelConfig convertToEmbeddingModelConfig(ModelConfigPayload.EmbeddingModelConfigItem item) {
        EmbeddingModelConfig embeddingModelConfig = new EmbeddingModelConfig();
        embeddingModelConfig.setId(item.getId());
        embeddingModelConfig.setSupplierName(item.getSupplierName());
        embeddingModelConfig.setBaseUrl(item.getBaseUrl());
        embeddingModelConfig.setApiKey(item.getApiKey());
        embeddingModelConfig.setModelName(item.getModelName());
        embeddingModelConfig.setDimensions(item.getDimensions());
        return embeddingModelConfig;
    }

    public static class Result {

        @Getter
        private final List<ChatModelConfig> chatModelConfigs;

        @Getter
        private final List<EmbeddingModelConfig> embeddingModelConfigs;

        public Result(List<ChatModelConfig> chatModelConfigs, List<EmbeddingModelConfig> embeddingModelConfigs) {
            this.chatModelConfigs = chatModelConfigs;
            this.embeddingModelConfigs = embeddingModelConfigs;
        }

    }

}
