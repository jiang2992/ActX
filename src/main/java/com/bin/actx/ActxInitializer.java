package com.bin.actx;

import com.bin.actx.config.LocalConfigManager;
import com.bin.actx.config.properties.AppProperties;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.model.EmbeddingModelManager;
import com.bin.actx.sandbox.PlaywrightSandbox;
import com.bin.actx.sandbox.SandboxManager;
import com.bin.actx.tool.ToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 初始化器
 *
 * @author Bin
 * @since 2026/5/12 15:20
 */
@Slf4j
@Component
public class ActxInitializer {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ToolManager toolManager;

    @Autowired
    private ChatModelManager chatModelManager;

    @Autowired
    private EmbeddingModelManager embeddingModelManager;

    @Autowired
    private LocalConfigManager localConfigManager;

    @Autowired
    private SandboxManager sandboxManager;

    @Autowired
    private PlaywrightSandbox playwrightSandbox;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        log.info("Initializing ACTX...");

        try {
            // 注册工具
            toolManager.register();

            // 初始化模型
            chatModelManager.register();
            embeddingModelManager.register();

            // 加载本地配置
            localConfigManager.loadAndRegister();

            // 初始化沙箱
            Boolean sandboxInit = sandboxManager.init();
            if (sandboxInit && appProperties.getPlaywrightEnable()) {
                playwrightSandbox.initService();
            }

            log.info("Initialized ACTX.");
        } catch (Exception e) {
            log.error("Initialization failed.", e);
            SpringApplication.exit(applicationContext, () -> 1);
            System.exit(1);
        }
    }

}
