package com.bin.actx.tool;

import com.bin.actx.tool.model.ToolKitInfo;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 工具管理器，负责工具包的自动发现、注册和回调提供。
 *
 * @author Bin
 * @since 2026/5/12 15:19
 */
@Slf4j
@Component
public class ToolManager {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 工具包映射：工具包名称 → 工具包信息
     */
    private Map<String, ToolKitInfo> toolKitMapper;

    /**
     * 工具策略：工具名称 → 工具策略
     */
    private Map<String, ToolStrategy> toolStrategyMapper;

    public void register() {
        toolKitMapper = new ConcurrentHashMap<>();
        toolStrategyMapper = new ConcurrentHashMap<>();

        Set<String> toolKitNames = new HashSet<>();
        Map<String, Object> toolKitBeans = applicationContext.getBeansWithAnnotation(Toolkit.class);
        toolKitBeans.values().forEach(bean -> {
            Toolkit toolKit = bean.getClass().getAnnotation(Toolkit.class);
            ToolKitInfo toolKitInfo = new ToolKitInfo();
            toolKitInfo.setName(toolKit.name());
            toolKitInfo.setDescription(toolKit.description());
            toolKitInfo.setBean(bean);
            toolKitInfo.setCls(bean.getClass());
            toolKitInfo.setAutoDiscover(toolKit.autoDiscover());
            toolKitMapper.put(toolKitInfo.getName(), toolKitInfo);
            toolKitNames.add(toolKitInfo.getName());

            // 获取工具策略
            Method[] declaredMethods = bean.getClass().getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                Tool toolAnnotation = declaredMethod.getAnnotation(Tool.class);
                if (toolAnnotation == null) {
                    continue;
                }
                ToolStrategy strategyAnnotation = declaredMethod.getAnnotation(ToolStrategy.class);
                if (strategyAnnotation == null) {
                    continue;
                }
                toolStrategyMapper.put(toolAnnotation.name(), strategyAnnotation);
            }
        });

        log.info("Registered {} tool kits: {}", toolKitNames.size(),
                toolKitNames.size() > 10 ? toolKitNames.stream().limit(10).collect(Collectors.joining(", ")) + "..."
                        : String.join(", ", toolKitNames));
    }

    public Collection<ToolKitInfo> getToolkitList(boolean onlyAutoDiscover) {
        return toolKitMapper.values().stream()
                .filter(item -> !onlyAutoDiscover || item.isAutoDiscover())
                .collect(Collectors.toSet());
    }

    public Collection<ToolKitInfo> getToolkitList(Collection<Class<?>> cls) {
        return toolKitMapper.values().stream().filter(toolKitInfo -> cls.contains(toolKitInfo.getCls()))
                .collect(Collectors.toSet());
    }

    public Collection<ToolKitInfo> getToolkitListByNames(Collection<String> names) {
        return toolKitMapper.values().stream().filter(toolKitInfo -> names.contains(toolKitInfo.getName()))
                .collect(Collectors.toSet());
    }

    public List<ToolCallback> getToolCallbackList(boolean onlyAutoDiscover) {
        return Arrays.stream(MethodToolCallbackProvider.builder()
                .toolObjects(this.getToolkitList(onlyAutoDiscover).stream().map(ToolKitInfo::getBean).distinct().toArray())
                .build()
                .getToolCallbacks()).toList();
    }

    public List<ToolCallback> getToolCallbackList(Collection<Class<?>> cls) {
        return Arrays.stream(MethodToolCallbackProvider.builder()
                .toolObjects(this.getToolkitList(cls).stream().map(ToolKitInfo::getBean).distinct().toArray())
                .build()
                .getToolCallbacks()).toList();
    }

    public List<ToolCallback> getToolCallbackListByNames(Collection<String> names) {
        return Arrays.stream(MethodToolCallbackProvider.builder()
                .toolObjects(this.getToolkitListByNames(names).stream().map(ToolKitInfo::getBean).distinct().toArray())
                .build()
                .getToolCallbacks()).toList();
    }

    public String getToolOperationType(String toolName) {
        ToolStrategy toolStrategy = toolStrategyMapper.get(toolName);
        if (toolStrategy == null) {
            return ToolStrategy.OPERATION_TYPE_READ;
        }
        return toolStrategy.operationType();
    }

}
