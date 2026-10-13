package com.bin.actx.config.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * App配置
 *
 * @author Bin
 * @since 2025/2/12 15:50
 */
@Getter
@Component
public class AppProperties {

    /**
     * 是否启用沙箱
     */
    @Value("${actx.sandbox.enable}")
    private Boolean sandboxEnable;

    /**
     * 沙箱域名
     */
    @Value("${actx.sandbox.domain}")
    private String sandboxDomain;

    /**
     * 沙箱API密钥
     */
    @Value("${actx.sandbox.api-key}")
    private String sandboxApiKey;

    /**
     * 快速沙箱池镜像
     */
    @Value("${actx.sandbox.fast-sandbox-pool.image:ubuntu}")
    private String fastSandboxPoolImage;

    /**
     * 快速沙箱池最大空闲数
     */
    @Value("${actx.sandbox.fast-sandbox-pool.max-idle:3}")
    private Integer fastSandboxPoolMaxIdle;

    /**
     * 快速沙箱池空闲超时时间（秒）
     */
    @Value("${actx.sandbox.fast-sandbox-pool.idle-timeout:86400}")
    private Integer fastSandboxPoolIdleTimeout;

    /**
     * 快速沙箱池沙箱超时时间（秒）
     */
    @Value("${actx.sandbox.fast-sandbox-pool.sandbox-timeout:300}")
    private Integer fastSandboxPoolSandboxTimeout;

    /**
     * 是否启用playwright（需要开启沙箱）
     */
    @Value("${actx.network.playwright.enable:false}")
    private Boolean playwrightEnable;

    /**
     * 配置存储目录
     */
    @Value("${actx.config.path:./config/}")
    private String configPath;

    /**
     * 数据库存储目录
     */
    @Value("${actx.storage.local.dir:./db/}")
    private String storageLocalDir;

    /**
     * 图数据库URL
     */
    @Value("${actx.storage.graph.url}")
    private String storageGraphUrl;

    /**
     * 图数据库用户
     */
    @Value("${actx.storage.graph.user}")
    private String storageGraphUser;

    /**
     * 图数据库密码
     */
    @Value("${actx.storage.graph.password}")
    private String storageGraphPassword;

    /**
     * 文件存储目录
     */
    @Value("${actx.storage.file.dir:./file/}")
    private String fileStorageDir;


    @Value("${actx.log.path:./log/}")
    private String logPath;


    @Value("${actx.log.full-request-logging:false}")
    private Boolean logFullRequestLogging;

}
