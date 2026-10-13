package com.bin.actx.sandbox;

import com.alibaba.opensandbox.sandbox.Sandbox;
import com.alibaba.opensandbox.sandbox.domain.models.sandboxes.SandboxEndpoint;
import com.bin.actx.exception.SandboxInitializeException;
import com.bin.actx.network.http.HttpRequestParam;
import com.bin.actx.network.http.HttpRequestService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Playwright.CreateOptions;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.decorator.BlockingTimerDecorator;
import org.jiang.tools.decorator.BlockingTimerDecorator.RunStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaywrightSandbox {

    private static final Duration SANDBOX_TIMEOUT = Duration.ofHours(2);
    private static final String SANDBOX_IMAGE = "mcr.microsoft.com/playwright:v1.61.0-noble";

    @Autowired
    private SandboxManager sandboxManager;

    @Autowired
    private HttpRequestService httpRequestService;

    private SandboxServiceInfo sandboxServiceInfo;

    public synchronized void initService() {
        synchronized (this) {
            Sandbox sandbox = null;
            try {
                if (sandboxServiceInfo == null) {
                    log.info("Create playwright sandbox...");
                    sandbox = sandboxManager.createSandbox(SANDBOX_IMAGE, SANDBOX_TIMEOUT);
                    SandboxEndpoint endpoint = sandbox.getEndpoint(3000);
                    sandboxServiceInfo = new SandboxServiceInfo();
                    sandboxServiceInfo.setSandboxId(sandbox.getId());
                    sandboxServiceInfo.setServiceEndpoint(endpoint.getEndpoint());
                }
                if (!this.checkService()) {
                    sandbox = sandboxManager.getOrCreateSandbox(sandboxServiceInfo.getSandboxId(), SANDBOX_IMAGE,
                            SANDBOX_TIMEOUT);
                    SandboxEndpoint endpoint = sandbox.getEndpoint(3000);
                    sandboxServiceInfo = new SandboxServiceInfo();
                    sandboxServiceInfo.setSandboxId(sandbox.getId());
                    sandboxServiceInfo.setServiceEndpoint(endpoint.getEndpoint());
                    this.startService(sandbox);
                }
            } catch (Exception e) {
                log.error("Create sandbox error", e);
            } finally {
                if (sandbox != null) {
                    sandbox.close();
                }
            }
        }
    }

    public boolean checkService() {
        log.debug("Check playwright sandbox service...");
        HttpRequestParam httpRequestParam = new HttpRequestParam();
        httpRequestParam.setMethod("GET");
        httpRequestParam.setUrl("http://%s".formatted(sandboxServiceInfo.getServiceEndpoint()));
        httpRequestParam.setTimeout(3000);
        Boolean result = BlockingTimerDecorator.run(1000L, 10, () -> {
            try {
                HttpResponse<String> httpResponse = httpRequestService.sendHttpRequest(httpRequestParam);
                String body = httpResponse.body();
                if ("Running".equals(body)) {
                    return RunStep.terminated(true);
                }
            } catch (Exception e) {
                log.error("Http request error", e);
            }
            return RunStep.continued();
        });
        if (result == null || !result) {
            log.warn("Playwright sandbox check error");
            return false;
        }
        log.debug("Playwright sandbox service ready");
        return true;
    }

    public void startService(Sandbox sandbox) {
        log.debug("Start playwright sandbox service...");
        sandbox.commands()
                .run("mkdir -p /data && nohup npx -y playwright@1.61.0 run-server --port 3000 --host 0.0.0.0 >> \"/data/server_$(date +%Y-%m-%d).log\" 2>&1 &");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new SandboxInitializeException(e);
        }
        if (!this.checkService()) {
            throw new SandboxInitializeException("Playwright sandbox start error");
        }
    }

    public <T> void connectBrowser(Function<Browser, T> func) {
        this.initService();
        log.info("Playwright sandbox endpoint: {}", this.sandboxServiceInfo.getServiceEndpoint());
        try (
                Playwright playwright = Playwright.create(new CreateOptions().setEnv(Map.of(
                        "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"
                )));
                Browser browser = playwright.chromium().connect("ws://" + this.sandboxServiceInfo.getServiceEndpoint());
        ) {
            func.apply(browser);
        } catch (Exception e) {
            log.error("Playwright sandbox error", e);
        }
    }

}
