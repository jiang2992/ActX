package com.bin.actx.network.html.playwright;

import lombok.Data;

@Data
public class PlaywrightPage {

    private String url;

    private Boolean success;

    private Exception exception;

    private String content;

    public static PlaywrightPage success(String url, String content) {
        PlaywrightPage result = new PlaywrightPage();
        result.setUrl(url);
        result.setSuccess(true);
        result.setContent(content);
        return result;
    }

    public static PlaywrightPage fail(String url, Exception exception) {
        PlaywrightPage result = new PlaywrightPage();
        result.setUrl(url);
        result.setSuccess(false);
        result.setException(exception);
        return result;
    }

}
