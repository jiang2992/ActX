package com.bin.actx.tool.kit.system;

import com.bin.actx.agent.AgentManager;
import com.bin.actx.config.GlobalStatusManager;
import com.bin.actx.config.constant.SceneConstant;
import com.bin.actx.mcp.McpClientInstance;
import com.bin.actx.mcp.McpClientManager;
import com.bin.actx.mcp.McpServerConfig;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.model.EmbeddingModelManager;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.kit.system.GlobalAgentInfo.AgentConfigInfo;
import com.bin.actx.tool.kit.system.GlobalAgentInfo.AgentInstanceInfo;
import com.bin.actx.tool.model.ToolCallResult;
import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import org.jiang.tools.date.EasyDate;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 系统相关工具包
 *
 * @author Bin
 * @since 2026/5/12 15:13
 */
@Toolkit(name = "系统", description = "查看本地时间、系统信息、当前配置的模型、向量模型、Agent、MCP等")
public class SystemToolkit {

    @Autowired
    private McpClientManager mcpClientManager;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private GlobalStatusManager globalStatusManager;
    @Autowired
    private ChatModelManager chatModelManager;
    @Autowired
    private EmbeddingModelManager embeddingModelManager;

    @Tool(name = "get_current_time", description = "获取当前日期时间")
    public ToolCallResult<String> getCurrentTime(
            @ToolParam(description = "日期时间格式（默认\"yyyy-MM-dd HH:mm:ss\"）", required = false) String format) {
        if (StringUtils.isEmpty(format)) {
            format = "yyyy-MM-dd HH:mm:ss";
        }
        return ToolCallResult.of(EasyDate.now().stringValue(format));
    }


    @Tool(name = "list_agent", description = "获取全部Agent信息，包括Agent配置、Agent实例以及场景与Agent实例之间的绑定关系")
    public ToolCallResult<GlobalAgentInfo> getAgentList() {
        GlobalAgentInfo globalAgentInfo = new GlobalAgentInfo();
        List<AgentConfigInfo> modelConfigList = agentManager.getConfigList().stream().map(agentConfig -> {
            AgentConfigInfo agentConfigInfo = new AgentConfigInfo();
            agentConfigInfo.setId(agentConfig.getId());
            agentConfigInfo.setModelId(agentConfig.getModelId());
            agentConfigInfo.setName(agentConfig.getName());
            agentConfigInfo.setDescription(agentConfig.getDescription());
            return agentConfigInfo;
        }).toList();
        List<AgentInstanceInfo> modelInstanceList = agentManager.getInstanceList().stream().map(agentInstance -> {
            AgentInstanceInfo agentInstanceInfo = new AgentInstanceInfo();
            agentInstanceInfo.setId(agentInstance.getId());
            agentInstanceInfo.setConfigId(agentInstance.getConfig().getId());
            agentInstanceInfo.setSystemPrivatized(agentInstance.getSystemPrivatized());
            return agentInstanceInfo;
        }).toList();
        globalAgentInfo.setAgentConfigList(modelConfigList);
        globalAgentInfo.setAgentInstanceList(modelInstanceList);
        globalAgentInfo.setSceneAgentMapper(globalStatusManager.getSceneAgentMapper());
        return ToolCallResult.of(globalAgentInfo, "Current Scene: %s. Current Agent: %s".formatted(SceneConstant.CHAT,
                globalStatusManager.getSceneAgentId(SceneConstant.CHAT)));
    }

    @Tool(name = "list_model", description = "获取全部模型信息以及系统支持的模型供应商")
    public ToolCallResult<GlobalModelInfo> getModelList() {
        GlobalModelInfo globalModelInfo = new GlobalModelInfo();
        globalModelInfo.setChatModelList(chatModelManager.getConfigList());
        globalModelInfo.setEmbeddingModelList(embeddingModelManager.getConfigList());
        globalModelInfo.setSupportSupplierList(chatModelManager.getSupplierList());
        return ToolCallResult.of(globalModelInfo);
    }

    @Tool(name = "list_mcp_server", description = "获取当前配置的所有MCP服务器")
    public ToolCallResult<Collection<McpServerConfig>> getMcpServerList() {
        List<McpClientInstance> instanceList = mcpClientManager.getAllInstances();
        List<McpServerConfig> configList = instanceList.stream().map(McpClientInstance::getConfig).toList();
        return ToolCallResult.of(configList, null);
    }

    @Tool(name = "get_system_info", description = "获取系统环境信息及资源使用情况，包括操作系统、CPU、内存、磁盘、JVM等")
    public ToolCallResult<SystemInfo> getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long mb = 1024 * 1024;

        SystemInfo info = new SystemInfo();
        info.setOsName(System.getProperty("os.name"));
        info.setOsVersion(System.getProperty("os.version"));
        info.setOsArch(System.getProperty("os.arch"));
        info.setJavaVersion(System.getProperty("java.version"));
        info.setCpuCores(runtime.availableProcessors());
        info.setJvmTotalMemoryMB(runtime.totalMemory() / mb);
        info.setJvmFreeMemoryMB(runtime.freeMemory() / mb);
        info.setJvmMaxMemoryMB(runtime.maxMemory() / mb);
        info.setTotalPhysicalMemoryMB(osBean.getTotalMemorySize() / mb);
        info.setFreePhysicalMemoryMB(osBean.getFreeMemorySize() / mb);

        File root = new File("/");
        info.setTotalDiskSpaceMB(root.getTotalSpace() / mb);
        info.setFreeDiskSpaceMB(root.getFreeSpace() / mb);

        info.setJvmUptimeSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        return ToolCallResult.of(info);
    }

}
