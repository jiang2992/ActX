package com.bin.actx.agent;

import com.bin.actx.agent.definition.AgentDefinition.PluginConfig;
import com.bin.actx.exception.PluginCreateInstanceException;
import com.bin.actx.plugin.BasePlugin;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.stereotype.Component;

/**
 * Agent 管理器
 *
 * @author Bin
 * @since 2026/5/9 17:29
 */
@Component
public class AgentManager {

    /**
     * Agent 配置映射：配置Id → 配置
     */
    private final Map<String, AgentConfig> configMap = new ConcurrentHashMap<>();

    /**
     * Agent 实例映射：实例Id → 实例
     */
    private final Map<String, AgentInstance> instanceMap = new ConcurrentHashMap<>();

    /**
     * 添加一个 Agent 配置
     *
     * @param config 配置
     */
    public String addConfig(AgentConfig config) {
        String configId = config.getId() != null ? config.getId() : IdUtils.uuid();
        config.setId(configId);
        this.configMap.put(config.getId(), config);
        return configId;
    }

    /**
     * 获取一个 Agent 配置
     *
     * @param configId 配置id
     * @return 配置
     */
    public AgentConfig getConfig(String configId) {
        return this.configMap.get(configId);
    }

    /**
     * 获取一个 Agent 实例
     *
     * @param instanceId 实例id
     * @return 实例
     */
    public AgentInstance getInstance(String instanceId) {
        return this.instanceMap.get(instanceId);
    }

    /**
     * 获取 Agent 实例从配置id
     *
     * @param configId 配置id
     * @return 实例
     */
    public List<AgentInstance> getInstanceByConfigId(String configId) {
        return this.instanceMap.values().stream().filter(instance -> instance.getConfig().getId().equals(configId)).toList();
    }

    /**
     * 创建一个 Agent 实例
     *
     * @param config 配置
     * @return 实例
     */
    public AgentInstance createInstance(AgentConfig config) {
        return createInstance(config, false);
    }

    /**
     * 创建一个 Agent 实例
     *
     * @param config 配置
     * @return 实例
     */
    public AgentInstance createInstance(AgentConfig config, Boolean systemPrivatized) {
        AgentInstance agentInstance = new AgentInstance(config, systemPrivatized);

        for (PluginConfig pluginConfig : config.getDefinition().getPluginConfigs()) {
            try {
                Object bean = Class.forName(pluginConfig.getClassName()).getDeclaredConstructor().newInstance();
                if (!(bean instanceof BasePlugin)) {
                    throw new ClassCastException("pluginConfig class must be extends BasePlugin");
                }
                agentInstance.addPlugin((BasePlugin) bean,
                        pluginConfig.getMetadata() != null ? pluginConfig.getMetadata() : new ConcurrentHashMap<>());
            } catch (Exception e) {
                throw new PluginCreateInstanceException(e);
            }
        }

        instanceMap.put(agentInstance.getId(), agentInstance);
        return agentInstance;
    }

    public void deleteInstance(String instanceId) {
        this.instanceMap.remove(instanceId);
    }

    public Collection<AgentConfig> getConfigList() {
        return this.configMap.values();
    }

    public Collection<AgentInstance> getInstanceList() {
        return this.instanceMap.values();
    }

}
