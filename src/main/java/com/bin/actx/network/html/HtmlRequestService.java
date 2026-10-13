package com.bin.actx.network.html;

import com.bin.actx.analyze.HtmlPageExtractor;
import com.bin.actx.config.properties.AppProperties;
import com.bin.actx.network.html.playwright.PlaywrightExecutor;
import com.bin.actx.network.html.playwright.PlaywrightPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HtmlRequestService {

    private static final int MIN_CONTENT_LENGTH = 500;

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private PlaywrightExecutor playwrightExecutor;
    @Autowired
    private HtmlPageExtractor htmlPageExtractor;

    public List<HtmlPageData> getPlainTextPage(List<String> urls) {
        List<HtmlPageData> htmlPageData = new ArrayList<>(urls.size());
        List<String> playwrightUrls = new ArrayList<>();

        // 默认使用直接请求的方式来获取页面内容，如果判断获取的页面为动态页面或抛出异常，则使用 Playwright 获取
        for (String url : urls) {
            try {
                Response response = requestHtml(url);
                if (this.isDynamicPage(response.body())) {
                    playwrightUrls.add(url);
                } else {
                    htmlPageData.add(htmlPageExtractor.extractWithTika(url, response.body()));
                }
            } catch (Exception e) {
                playwrightUrls.add(url);
            }
        }

        // 如果没有动态页面，则直接返回
        if (playwrightUrls.isEmpty()) {
            return htmlPageData;
        }

        // 使用 Playwright 获取动态页面内容
        List<PlaywrightPage> pages = playwrightExecutor.execute(playwrightUrls);
        for (PlaywrightPage page : pages) {
            if (!page.getSuccess()) {
                htmlPageData.add(HtmlPageData.fail(page.getUrl(), "Error executing Playwright task: " + page.getException().getMessage()));
                continue;
            }
            try {
                htmlPageData.add(htmlPageExtractor.extractWithTika(page.getUrl(), page.getContent()));
            } catch (Exception e) {
                log.error("Error extracting HTML page", e);
                htmlPageData.add(HtmlPageData.fail(page.getUrl(), "Error extracting HTML page: " + e.getMessage()));
            }
        }

        return htmlPageData;
    }

    /**
     * 判断是否为动态页面
     *
     * @param html HTML
     * @return 是否为动态页面
     */
    private boolean isDynamicPage(String html) {
        if (html == null || html.length() < MIN_CONTENT_LENGTH) {
            return true;
        }
        // 检测常见动态框架特征
        String lower = html.toLowerCase();
        return lower.contains("<script") && (lower.contains("react") || lower.contains("vue") || lower.contains("angular")
                || lower.contains("__next"));
    }

    private Response requestHtml(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .ignoreContentType(true)
                .execute();
    }

}
