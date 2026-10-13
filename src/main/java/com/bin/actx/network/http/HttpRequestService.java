package com.bin.actx.network.http;

import com.bin.actx.file.FileInfo;
import com.bin.actx.file.FileManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestService {

    @Autowired
    private FileManager fileManager;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public HttpResponse<String> sendHttpRequest(HttpRequestParam param) throws IOException, InterruptedException {
        HttpRequest httpRequest = this.buildHttpRequest(param);
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    public FileInfo downloadHttpResource(HttpRequestParam param, String savePath) throws IOException, InterruptedException {
        HttpRequest httpRequest = this.buildHttpRequest(param);
        HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        return fileManager.writeFile(savePath, "rename", response.body());
    }

    private HttpRequest buildHttpRequest(HttpRequestParam param) {
        String method = param.getMethod();
        String url = param.getUrl();
        Map<String, String> headers = param.getHeaders();
        String body = param.getBody();
        Integer timeout = param.getTimeout();

        // Create request builder
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(url));

        // Set timeout if provided
        int actualTimeout = timeout != null ? timeout : 30;
        requestBuilder.timeout(Duration.ofSeconds(actualTimeout));

        // Add headers if provided
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }
        }

        // Set request method and body
        String upperMethod = method.toUpperCase();
        switch (upperMethod) {
            case "GET":
                requestBuilder.GET();
                break;
            case "POST":
                if (body != null && !body.isEmpty()) {
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                } else {
                    requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                }
                break;
            case "PUT":
                if (body != null && !body.isEmpty()) {
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
                } else {
                    requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
                }
                break;
            case "DELETE":
                requestBuilder.DELETE();
                break;
            default:
                // For other methods, use the generic approach
                if (body != null && !body.isEmpty()) {
                    requestBuilder.method(upperMethod, HttpRequest.BodyPublishers.ofString(body));
                } else {
                    requestBuilder.method(upperMethod, HttpRequest.BodyPublishers.noBody());
                }
                break;
        }

        return requestBuilder.build();
    }

}
