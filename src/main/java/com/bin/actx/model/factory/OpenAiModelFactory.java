package com.bin.actx.model.factory;

import com.bin.actx.model.ChatModelConfig;
import com.bin.actx.model.EmbeddingModelConfig;
import com.bin.actx.model.ModelHttpLogger;
import com.bin.actx.model.factory.getter.ModelMessageGetter;
import com.bin.actx.model.factory.getter.OpenAiModelMessageGetter;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.openai.api.ResponseFormat.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OpenAI模型工厂
 *
 * @author Bin
 * @since 2026/5/12 14:40
 */
@Component
public class OpenAiModelFactory implements ModelFactory {

    @Autowired
    private ModelHttpLogger modelHttpLogger;

    private OpenAiApi getOrCreateApi(String baseUrl, String apiKey) {
        OpenAiApi.Builder builder = OpenAiApi.builder();
        if (baseUrl != null) {
            baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            builder.baseUrl(baseUrl);
            if (baseUrl.matches(".*/v\\d+$")) {
                builder.completionsPath("/chat/completions");
                builder.embeddingsPath("/embeddings");
            }
        }
        builder.apiKey(apiKey);
        builder.restClientBuilder(modelHttpLogger.getModelRestClientBuilder());
        builder.webClientBuilder(modelHttpLogger.getModelWebClientBuilder());
        return builder.build();
    }

    @Override
    public ChatModel getChatModel(ChatModelConfig chatModelConfig) {
        OpenAiApi api = this.getOrCreateApi(chatModelConfig.getBaseUrl(), chatModelConfig.getApiKey());
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();
        optionsBuilder.model(chatModelConfig.getModelName());
        optionsBuilder.streamUsage(true);
        if (chatModelConfig.getEnableThink() != null) {
            optionsBuilder.extraBody(Map.of("thinking", Map.of("type", chatModelConfig.getEnableThink() ? "enabled" : "disabled")));
        }
        if ("json".equals(chatModelConfig.getResponseFormat())) {
            optionsBuilder.responseFormat(ResponseFormat.builder().type(Type.JSON_OBJECT).build());
        }
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    @Override
    public EmbeddingModel getEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        OpenAiApi api = this.getOrCreateApi(embeddingModelConfig.getBaseUrl(), embeddingModelConfig.getApiKey());
        OpenAiEmbeddingOptions.Builder options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModelConfig.getModelName())
                .dimensions(embeddingModelConfig.getDimensions());
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options.build());
    }

    @Override
    public ModelMessageGetter getModelMessageGetter() {
        return new OpenAiModelMessageGetter();
    }

}
