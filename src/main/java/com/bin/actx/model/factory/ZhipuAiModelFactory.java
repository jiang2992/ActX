package com.bin.actx.model.factory;

import com.bin.actx.model.ChatModelConfig;
import com.bin.actx.model.EmbeddingModelConfig;
import com.bin.actx.model.ModelHttpLogger;
import com.bin.actx.model.factory.getter.ModelMessageGetter;
import com.bin.actx.model.factory.getter.ZhipuModelMessageGetter;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi.Builder;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi.ChatCompletionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 智谱AI模型工厂
 *
 * @author Bin
 * @since 2026/5/12 14:40
 */
@Component
public class ZhipuAiModelFactory implements ModelFactory {

    @Autowired
    private ModelHttpLogger modelHttpLogger;

    private ZhiPuAiApi getOrCreateApi(String baseUrl, String apiKey) {
        Builder builder = ZhiPuAiApi.builder();
        if (StringUtils.notEmpty(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        builder.restClientBuilder(modelHttpLogger.getModelRestClientBuilder());
        builder.webClientBuilder(modelHttpLogger.getModelWebClientBuilder());
        return builder.apiKey(apiKey).build();
    }

    @Override
    public ChatModel getChatModel(ChatModelConfig chatModelConfig) {
        ZhiPuAiApi api = this.getOrCreateApi(chatModelConfig.getBaseUrl(), chatModelConfig.getApiKey());
        ZhiPuAiChatOptions.Builder optionsBuilder = ZhiPuAiChatOptions.builder();
        optionsBuilder.model(chatModelConfig.getModelName());
        optionsBuilder.thinking(ChatCompletionRequest.Thinking.enabled());
        if (chatModelConfig.getEnableThink() != null) {
            optionsBuilder.thinking(
                    chatModelConfig.getEnableThink() ? ChatCompletionRequest.Thinking.enabled()
                            : ChatCompletionRequest.Thinking.disabled());
        }
        if ("json".equals(chatModelConfig.getResponseFormat())) {
            optionsBuilder.responseFormat(ChatCompletionRequest.ResponseFormat.jsonObject());
        }
        return new ZhiPuAiChatModel(api, optionsBuilder.build());
    }

    @Override
    public EmbeddingModel getEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        ZhiPuAiApi api = this.getOrCreateApi(embeddingModelConfig.getBaseUrl(), embeddingModelConfig.getApiKey());
        ZhiPuAiEmbeddingOptions.Builder optionsBuilder = ZhiPuAiEmbeddingOptions.builder()
                .model(embeddingModelConfig.getModelName())
                .dimensions(embeddingModelConfig.getDimensions());
        return new ZhiPuAiEmbeddingModel(api, MetadataMode.EMBED, optionsBuilder.build());
    }

    @Override
    public ModelMessageGetter getModelMessageGetter() {
        return new ZhipuModelMessageGetter();
    }

}
