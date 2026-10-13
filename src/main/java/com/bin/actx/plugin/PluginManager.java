package com.bin.actx.plugin;

import com.bin.actx.agent.AgentInstance;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.plugin.hook.ResponseHook;
import com.bin.actx.plugin.hook.ToolHook;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 插件管理器
 *
 * @author Bin
 * @since 2026/5/28 10:10
 */
@Slf4j
@Component
public class PluginManager {

    public List<RequestHook> getRequestHooks(AgentInstance agentInstance) {
        return agentInstance.getPlugins().keySet().stream()
                .filter(plugin -> plugin instanceof RequestHook)
                .map(plugin -> (RequestHook) plugin)
                .toList();
    }

    public List<ToolHook> getToolHooks(AgentInstance agentInstance) {
        return agentInstance.getPlugins().keySet().stream()
                .filter(plugin -> plugin instanceof ToolHook)
                .map(plugin -> (ToolHook) plugin)
                .toList();
    }

    public List<ResponseHook> getResponseHooks(AgentInstance agentInstance) {
        return agentInstance.getPlugins().keySet().stream()
                .filter(plugin -> plugin instanceof ResponseHook)
                .map(plugin -> (ResponseHook) plugin)
                .toList();
    }

}
