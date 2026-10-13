package com.bin.actx.network.http;

import java.util.Map;
import lombok.Data;

/**
 * HTTP请求参数对象
 *
 * @author Bin
 * @since 2026/5/16
 */
@Data
public class HttpRequestParam {

    private String method;

    private String url;

    private Map<String, String> headers;

    private String body;

    private Integer timeout;

}
