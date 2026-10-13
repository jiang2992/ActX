package com.bin.actx.config;

import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具异常处理配置
 *
 * @author Bin
 * @since 2026/5/12 11:31
 */
@Configuration
public class ToolExceptionConfig {

    @Bean
    public ToolExecutionExceptionProcessor customExceptionProcessor() {
        return (ToolExecutionException exception) -> {
            return String.format("Calling tool \"%s\" fail: %s",
                    exception.getToolDefinition().name(), exception.getMessage());
        };
    }

}
