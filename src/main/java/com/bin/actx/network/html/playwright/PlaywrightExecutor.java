package com.bin.actx.network.html.playwright;

import com.bin.actx.exception.PlaywrightException;
import com.bin.actx.sandbox.PlaywrightSandbox;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaywrightExecutor {

    @Autowired
    private PlaywrightSandbox playwrightSandbox;

    /**
     * 线程池
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public List<PlaywrightPage> execute(List<String> urls) {
        Future<List<PlaywrightPage>> task = executor.submit(() -> {
            log.info("Playwright open urls: {}", String.join(", ", urls));

            List<PlaywrightPage> result = new ArrayList<>(urls.size());
            ExecutorService pageExecutor = Executors.newFixedThreadPool(3);

            try {
                playwrightSandbox.connectBrowser(browser -> {
                    // 创建任务
                    List<CompletableFuture<PlaywrightPage>> futures = new ArrayList<>(urls.size());
                    for (String url : urls) {
                        BrowserContext browserContext = browser.newContext();
                        CompletableFuture<PlaywrightPage> future = CompletableFuture.supplyAsync(() -> this.doExecute(browserContext, url),
                                pageExecutor);
                        futures.add(future);
                    }

                    // 并行执行任务
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                    // 处理任务结果
                    for (CompletableFuture<PlaywrightPage> future : futures) {
                        try {
                            PlaywrightPage pageResult = future.join();
                            result.add(pageResult);
                        } catch (Exception e) {
                            PlaywrightPage pageResult = PlaywrightPage.fail("unknown", e);
                            result.add(pageResult);
                        }
                    }

                    return null;
                });
            } catch (Exception e) {
                pageExecutor.shutdown();
            }

            return result;
        });

        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new PlaywrightException(e);
        }
    }

    private PlaywrightPage doExecute(BrowserContext browserContext, String url) {
        try {
            Page page = browserContext.newPage();
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            return PlaywrightPage.success(url, page.content());
        } catch (Exception e) {
            return PlaywrightPage.fail(url, e);
        }
    }

}
