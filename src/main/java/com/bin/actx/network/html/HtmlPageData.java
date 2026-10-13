package com.bin.actx.network.html;

import java.util.Map;
import lombok.Data;

@Data
public class HtmlPageData {

    private String url;

    private String title;

    private String content;

    private Map<String, String> metadata;

    private String exception;

    public static HtmlPageData fail(String url, String exception) {
        HtmlPageData htmlPageData = new HtmlPageData();
        htmlPageData.setUrl(url);
        htmlPageData.setException(exception);
        return htmlPageData;
    }

}
