package com.bin.actx.model.factory;

import com.bin.actx.model.ChatModelConfig;
import com.bin.actx.model.EmbeddingModelConfig;
import com.bin.actx.model.factory.getter.ModelMessageGetter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * 聊天模型工厂SPI接口，每种模型供应商实现此接口以创建对应的ChatModel实例。</br> 默认名称由类名推导：OpenAiModelFactory → OpenAi
 *
 * @author Bin
 * @since 2026/5/12 14:23
 */
public interface ModelFactory {

    /**
     * 获取供应商标识名称，默认从类名中去掉"ModelFactory"后缀得到
     */
    default String getName() {
        String simpleName = this.getClass().getSimpleName();
        if (!simpleName.endsWith("ModelFactory")) {
            throw new IllegalStateException("Need overwrite getName method.");
        }
        return simpleName.substring(0, simpleName.length() - "ModelFactory".length());
    }

    /**
     * 根据模型配置创建ChatModel实例
     *
     * @param chatModelConfig 模型配置
     * @return ChatModel实例
     */
    ChatModel getChatModel(ChatModelConfig chatModelConfig);

    /**
     * 根据模型配置创建EmbeddingModel实例
     *
     * @param embeddingModelConfig 向量模型配置
     * @return EmbeddingModel实例
     */
    EmbeddingModel getEmbeddingModel(EmbeddingModelConfig embeddingModelConfig);

    ModelMessageGetter getModelMessageGetter();

}
