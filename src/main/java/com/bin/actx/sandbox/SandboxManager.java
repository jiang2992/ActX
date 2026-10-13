package com.bin.actx.sandbox;

import com.alibaba.opensandbox.sandbox.Sandbox;
import com.alibaba.opensandbox.sandbox.config.ConnectionConfig;
import com.alibaba.opensandbox.sandbox.domain.exceptions.SandboxException;
import com.alibaba.opensandbox.sandbox.domain.models.sandboxes.Host;
import com.alibaba.opensandbox.sandbox.domain.models.sandboxes.SandboxFilter;
import com.alibaba.opensandbox.sandbox.domain.models.sandboxes.Volume;
import com.alibaba.opensandbox.sandbox.domain.pool.AcquirePolicy;
import com.alibaba.opensandbox.sandbox.domain.pool.PoolCreationSpec;
import com.alibaba.opensandbox.sandbox.domain.pool.PoolCreationSpec.Builder;
import com.alibaba.opensandbox.sandbox.pool.SandboxPool;
import com.bin.actx.config.properties.AppProperties;
import com.bin.actx.exception.SandboxInitializeException;
import com.bin.actx.sandbox.store.SqlitePoolStateStore;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SandboxManager {

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private SqlitePoolStateStore sqlitePoolStateStore;

    private ConnectionConfig connectionConfig;

    /**
     * 快速沙箱池，即用即走
     */
    private SandboxPool fastSandboxPool;

    public Boolean isEnabled() {
        return connectionConfig != null;
    }

    public Boolean init() {
        boolean enabled = false;
        if (appProperties.getSandboxEnable() == null) {
            enabled = appProperties.getSandboxDomain() != null;
        } else if (appProperties.getSandboxEnable()) {
            enabled = true;
        }

        if (!enabled) {
            log.info("Sandbox is disabled.");
            return false;
        }

        if (appProperties.getSandboxDomain() == null || appProperties.getSandboxApiKey() == null) {
            throw new SandboxInitializeException("Sandbox domain or api key is null.");
        }

        connectionConfig = ConnectionConfig.builder()
                .domain(appProperties.getSandboxDomain())
                .apiKey(appProperties.getSandboxApiKey())
                .build();

        // 测试连接
        try (com.alibaba.opensandbox.sandbox.SandboxManager sandboxManager = com.alibaba.opensandbox.sandbox.SandboxManager.builder()
                .connectionConfig(connectionConfig).build()) {
            sandboxManager.listSandboxInfos(SandboxFilter.builder().pageSize(1).build());
        } catch (Exception e) {
            log.error("The connection to OpenSandbox failed. Please ensure that the OpenSandbox service and Docker service are running.",
                    e);
            return false;
        }

        log.info("Sandbox is enabled [domain: {}].", appProperties.getSandboxDomain());

        this.createFastSandboxPool();
        log.info("Fast sandbox pool created.");

        return true;
    }

    private void createFastSandboxPool() {
        PoolCreationSpec poolCreationSpec = new Builder()
                .image(appProperties.getFastSandboxPoolImage())
                .volumes(this.getWorkVolume())
                .build();
        fastSandboxPool = SandboxPool.builder()
                .ownerId("actx")
                .poolName("fast-sandbox-pool")
                .connectionConfig(connectionConfig)
                .maxIdle(appProperties.getFastSandboxPoolMaxIdle())
                .idleTimeout(Duration.ofSeconds(appProperties.getFastSandboxPoolIdleTimeout()))
                .creationSpec(poolCreationSpec)
                .stateStore(sqlitePoolStateStore)
                .build();
        fastSandboxPool.start();
    }

    /**
     * 获取快速沙箱，需要手动关闭沙箱
     *
     * @return 快速沙箱
     */
    public Sandbox acquireFastSandbox() {
        return fastSandboxPool.acquire(Duration.ofSeconds(appProperties.getFastSandboxPoolSandboxTimeout()),
                AcquirePolicy.DIRECT_CREATE);
    }

    /**
     * 获取快速沙箱并执行函数，沙箱在函数结束后会自动销毁
     *
     * @param func 处理函数
     * @param <T>  处理函数返回值类型
     * @return 处理函数返回值
     */
    public <T> T acquireFastSandbox(Function<Sandbox, T> func) {
        try (Sandbox sandbox = this.acquireFastSandbox()) {
            try {
                return func.apply(sandbox);
            } finally {
                sandbox.kill();
            }
        }
    }

    /**
     * 创建沙箱（需要手动严格管理生命周期）
     *
     * @param image   镜像
     * @param timeout 超时时间
     * @return 沙箱
     */
    public Sandbox createSandbox(String image, Duration timeout) {
        return Sandbox.builder()
                .connectionConfig(connectionConfig)
                .image(image)
                .timeout(timeout)
                .volumes(this.getWorkVolume())
                .build();
    }

    public Sandbox getSandbox(String id) {
        return Sandbox.connector()
                .connectionConfig(connectionConfig)
                .sandboxId(id)
                .connect();
    }

    public Sandbox getOrCreateSandbox(String id, String image, Duration timeout) {
        Sandbox sandbox;
        try {
            sandbox = this.getSandbox(id);
            if (!sandbox.isHealthy()) {
                log.info("Sandbox is unhealthy, kill the sandbox [image: {}].", image);
                try {
                    sandbox.kill();
                    sandbox.close();
                } catch (Exception e) {
                    log.warn("Kill and close sandbox error.", e);
                }
                sandbox = null;
            }
        } catch (SandboxException e) {
            log.warn("Connect sandbox fail [image: {}].", image);
            sandbox = null;
        } catch (Exception e) {
            log.error("Connect sandbox error [image: {}].", image, e);
            throw e;
        }

        // 创新创建沙箱
        if (sandbox == null) {
            log.info("Recreate the sandbox [image: {}].", image);
            sandbox = this.createSandbox(image, timeout);
        } else {
            sandbox.renew(timeout);
        }

        return sandbox;
    }

    /**
     * 获取工作目录卷,对目录进行权限控制
     *
     * @return 卷列表
     */
    private List<Volume> getWorkVolume() {
        String hostPath = Paths.get(appProperties.getFileStorageDir()).toAbsolutePath().normalize().toString();
        Volume data = Volume.builder()
                .name("data")
                .host(Host.of(hostPath))
                .mountPath("/data")
                .readOnly(true)
                .build();
        Volume workspace = Volume.builder()
                .name("workspace")
                .host(Host.of(hostPath + "/workspace"))
                .mountPath("/data/workspace")
                .readOnly(false)
                .build();
        return List.of(data, workspace);
    }

}
