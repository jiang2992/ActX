package com.bin.actx.agent;

import com.bin.actx.plugin.BasePlugin;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Agent 实例
 *
 * @author Bin
 * @since 2026/5/9 17:27
 */
@Getter
public class AgentInstance {

    /**
     * Agent 实例唯一标识
     */
    private final String id;

    /**
     * Agent 配置
     */
    private final AgentConfig config;

    /**
     * 系统私有
     */
    private final Boolean systemPrivatized;

    /**
     * 配置的装配器列表
     */
    private final Map<BasePlugin, Map<String, Object>> plugins = new LinkedHashMap<>();

    public AgentInstance(AgentConfig config) {
        this(config, false);
    }

    public AgentInstance(AgentConfig config, Boolean systemPrivatized) {
        this.id = config.getId();
        this.config = config;
        this.systemPrivatized = systemPrivatized;
    }

    /**
     * 添加一个插件
     *
     * @param plugin   消息插件
     * @param metadata 元数据
     */
    public void addPlugin(BasePlugin plugin, Map<String, Object> metadata) {
        plugins.put(plugin, metadata);
    }

}
