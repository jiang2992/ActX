package com.bin.actx.model;

import com.bin.actx.exception.ModelConfigException;
import com.bin.actx.model.factory.ModelFactory;
import com.bin.actx.model.factory.getter.ModelMessageGetter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.ChatOptions.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 聊天客户端管理器
 *
 * @author Bin
 * @since 2026/5/12 14:21
 */
@Component
public class ChatModelManager {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 模型工厂映射：供应商标识 → 工厂实例
     */
    private final Map<String, ModelFactory> modelFactoryMap = new ConcurrentHashMap<>();

    /**
     * 模型客户端映射：模型id → 模型客户端
     */
    private final Map<String, ChatClient> chatClientMap = new ConcurrentHashMap<>();

    /**
     * 模型配置映射：模型id → 模型配置
     */
    private final Map<String, ChatModelConfig> configMapper = new ConcurrentHashMap<>();


    public void register() {
        // 自动发现并注册所有ChatModelFactory实现
        Map<String, ModelFactory> beans = applicationContext.getBeansOfType(ModelFactory.class);
        beans.forEach((name, bean) -> modelFactoryMap.put(bean.getName(), bean));
    }

    /**
     * 添加模型配置，此阶段不会创建客户端
     *
     * @param chatModelConfig 模型配置
     * @return 模型id
     */
    public String addConfig(ChatModelConfig chatModelConfig) {
        ModelFactory modelFactory = modelFactoryMap.get(chatModelConfig.getSupplierName());
        if (modelFactory == null) {
            throw new ModelConfigException("Unsupported supplier.");
        }

        String modelId = chatModelConfig.getId() != null ? chatModelConfig.getId() : IdUtils.uuid();
        chatModelConfig.setId(modelId);
        this.configMapper.put(modelId, chatModelConfig);

        return modelId;
    }

    public ChatModelConfig getConfig(String modelId) {
        return this.configMapper.get(modelId);
    }

    /**
     * 根据模型id获取客户端，若客户端未创建根据配置创建，如果未配置则返回null
     *
     * @param modelId 模型id
     * @return 客户端
     */
    public synchronized ChatClient getClient(String modelId) {
        ChatClient chatClient = this.chatClientMap.get(modelId);
        if (chatClient != null) {
            return chatClient;
        }
        ChatModelConfig chatModelConfig = this.configMapper.get(modelId);
        if (chatModelConfig == null) {
            return null;
        }
        ChatClient client = this.createClient(chatModelConfig);
        this.chatClientMap.put(modelId, client);
        return client;
    }

    private ChatClient createClient(ChatModelConfig chatModelConfig) {
        ModelFactory modelFactory = modelFactoryMap.get(chatModelConfig.getSupplierName());
        ChatModel chatModel = modelFactory.getChatModel(chatModelConfig);
        return ChatClient.builder(chatModel)
                .defaultOptions(this.getChatOptions(chatModelConfig))
                .build();
    }

    private ChatOptions getChatOptions(ChatModelConfig chatModelConfig) {
        Builder chatOptionsBuilder = ChatOptions.builder();
        if (chatModelConfig.getTemperature() != null) {
            chatOptionsBuilder.temperature(chatModelConfig.getTemperature());
        }
        return chatOptionsBuilder.build();
    }

    /**
     * 获取所有已注册的供应商名称
     */
    public Set<String> getSupplierList() {
        return this.modelFactoryMap.keySet();
    }

    /**
     * 获取所有已创建的模型配置列表
     */
    public Collection<ChatModelConfig> getConfigList() {
        return this.configMapper.values();
    }

    public ModelMessageGetter getModelMessageGetter(String modelId) {
        ChatModelConfig chatModelConfig = this.configMapper.get(modelId);
        ModelFactory modelFactory = modelFactoryMap.get(chatModelConfig.getSupplierName());
        return modelFactory.getModelMessageGetter();
    }

}
