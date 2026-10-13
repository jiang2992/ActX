package com.bin.actx.model.factory;

import com.bin.actx.exception.CreateModelException;
import com.bin.actx.model.ChatModelConfig;
import com.bin.actx.model.EmbeddingModelConfig;
import com.bin.actx.model.ModelHttpLogger;
import com.bin.actx.model.factory.getter.DeepseekModelMessageGetter;
import com.bin.actx.model.factory.getter.ModelMessageGetter;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.deepseek.api.DeepSeekApi.Builder;
import org.springframework.ai.deepseek.api.ResponseFormat;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deepseek模型工厂
 *
 * @author Bin
 * @since 2026/6/26 10:15
 */
@Component
public class DeepseekModelFactory implements ModelFactory {

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Autowired
    private ModelHttpLogger modelHttpLogger;

    private DeepSeekApi getOrCreateApi(String baseUrl, String apiKey) {
        Builder builder = DeepSeekApi.builder();
        if (StringUtils.notEmpty(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        builder.restClientBuilder(modelHttpLogger.getModelRestClientBuilder());
        builder.webClientBuilder(modelHttpLogger.getModelWebClientBuilder());
        return builder.apiKey(apiKey).build();
    }

    @Override
    public ChatModel getChatModel(ChatModelConfig chatModelConfig) {
        DeepSeekApi api = this.getOrCreateApi(chatModelConfig.getBaseUrl(), chatModelConfig.getApiKey());
        DeepSeekChatOptions.Builder optionsBuilder = DeepSeekChatOptions.builder();
        optionsBuilder.model(chatModelConfig.getModelName());
        if ("json".equals(chatModelConfig.getResponseFormat())) {
            optionsBuilder.responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build());
        }
        return DeepSeekChatModel.builder().deepSeekApi(api)
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(toolCallingManager)
                .build();
    }

    @Override
    public EmbeddingModel getEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        throw new CreateModelException("Deepseek embedding model is not supported");
    }

    @Override
    public ModelMessageGetter getModelMessageGetter() {
        return new DeepseekModelMessageGetter();
    }

}
