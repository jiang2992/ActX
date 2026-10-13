package com.bin.actx.model;

import com.bin.actx.config.properties.AppProperties;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jiang.tools.text.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 模型 HTTP 调用日志配置。
 * <p>
 * 为 Spring AI 模型 API 提供带日志的 RestClient.Builder 和 WebClient.Builder。
 * <p>
 * 仅在 actx.log.full-request-logging = true 时生效： 每个请求写入一个独立的日志文件，保存在 actx.log.path 目录下。
 * <p>
 * WebClient 用于流式模型调用，记录： - 请求方法 - URL - Headers - Request Body - Response Status - SSE 原始数据块
 *
 * @author Bin
 * @since 2026/8/17
 */
@Slf4j
@Component
public class ModelHttpLogger {

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private AppProperties appProperties;

    /**
     * 同步模型调用。
     */
    public RestClient.Builder getModelRestClientBuilder() {
        return RestClient.builder().requestInterceptor(this::logRestClientRequest);
    }

    /**
     * 流式模型调用。
     */
    public WebClient.Builder getModelWebClientBuilder() {
        return WebClient.builder().filter(logWebClient());
    }

    /**
     * 是否开启完整请求日志。
     */
    private boolean loggingEnabled() {
        return Boolean.TRUE.equals(appProperties.getLogFullRequestLogging());
    }

    /**
     * RestClient 请求日志。
     */
    private ClientHttpResponse logRestClientRequest(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (!loggingEnabled()) {
            return execution.execute(request, body);
        }

        RequestLogFile logFile;

        try {
            logFile = new RequestLogFile();
        } catch (IOException e) {
            log.warn("创建模型请求日志文件失败: {}", e.getMessage());
            return execution.execute(request, body);
        }

        long start = System.currentTimeMillis();
        try {
            writeRequest(logFile, "[Model RestClient]", request.getMethod(), request.getURI(), request.getHeaders());
            writeRequestBody(logFile, body);
            ClientHttpResponse response = execution.execute(request, body);
            logFile.writeLine(String.format("[Response Status] %s (%dms)", response.getStatusCode(), System.currentTimeMillis() - start));
            return response;
        } finally {
            logFile.close();
        }
    }

    /**
     * WebClient 日志过滤器。
     */
    private ExchangeFilterFunction logWebClient() {
        return (request, next) -> {
            if (!loggingEnabled()) {
                return next.exchange(request);
            }

            RequestLogFile logFile;
            try {
                logFile = new RequestLogFile();
            } catch (IOException e) {
                log.warn("创建模型请求日志文件失败: {}", e.getMessage());
                return next.exchange(request);
            }

            long start = System.currentTimeMillis();
            try {
                writeRequest(logFile, "[Model WebClient]", request.method(), request.url(), request.headers());
                ClientRequest loggingRequest = wrapRequestBody(request, logFile);
                return next.exchange(loggingRequest).map(response -> logResponse(response, logFile, start)).doOnError(e -> logFile.close())
                        .doOnCancel(logFile::close);
            } catch (RuntimeException e) {
                logFile.close();
                throw e;
            }
        };
    }

    /**
     * 写入请求基本信息。
     */
    private void writeRequest(RequestLogFile logFile, String clientName, HttpMethod method, URI uri, HttpHeaders headers) {
        logFile.writeLine(String.format("%s %s %s", clientName, method, uri));
        headers.forEach((name, values) -> logFile.writeLine(String.format("[%s] %s", name, maskSensitive(name, values))));
    }

    /**
     * 包装 WebClient 请求 Body。
     * <p>
     * 在 Body 真正写入 HTTP 请求时捕获内容。
     */
    private ClientRequest wrapRequestBody(ClientRequest request, RequestLogFile logFile) {
        BodyInserter<?, ? super ClientHttpRequest> originalBody = request.body();
        if (originalBody == null) {
            return request;
        }
        BodyInserter<Object, ClientHttpRequest> loggingBody = (outputMessage, context) -> {
            BodyLoggingHttpRequest loggingRequest = new BodyLoggingHttpRequest(outputMessage);
            return originalBody.insert(loggingRequest, context)
                    .doOnSuccess(v -> logFile.writeLine("[Request Body] " + loggingRequest.bodyLog()));
        };
        return ClientRequest.from(request).body(loggingBody).build();
    }

    /**
     * 写入响应状态，并包装 Response Body。
     */
    private ClientResponse logResponse(ClientResponse response, RequestLogFile logFile, long start) {
        logFile.writeLine(String.format("[Response Status] %s (%dms)", response.statusCode(), System.currentTimeMillis() - start));
        return response.mutate().body(body -> logResponseBody(body, logFile)).build();
    }

    /**
     * 在原始 Response Body 上增加 SSE 日志。
     * <p>
     * Response Body 真正结束时关闭日志文件。
     */
    private Flux<DataBuffer> logResponseBody(Flux<DataBuffer> body, RequestLogFile logFile) {
        return body.doOnNext(buffer -> {
            String content = readBuffer(buffer);
            if (!content.isEmpty()) {
                logFile.writeSse(content);
            }
        }).doFinally(signal -> logFile.close());
    }

    /**
     * 读取 DataBuffer 内容，同时恢复 reader position。
     */
    private String readBuffer(DataBuffer buffer) {
        int readerPosition = buffer.readPosition();
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        buffer.readPosition(readerPosition);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 写入请求 Body。
     */
    private void writeRequestBody(RequestLogFile logFile, byte[] body) {
        if (body != null && body.length > 0) {
            logFile.writeLine("[Request Body] " + new String(body, StandardCharsets.UTF_8));
        }
    }

    /**
     * 敏感 Header 脱敏。
     */
    private String maskSensitive(String headerName, List<String> values) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName) || "api-key".equalsIgnoreCase(headerName)) {

            return values.stream().map(this::maskSensitiveValue).collect(Collectors.joining(", "));
        }

        return String.join(", ", values);
    }

    /**
     * 敏感 Header 值脱敏。
     */
    private String maskSensitiveValue(String value) {
        return value.length() > 6 ? value.substring(0, 6) + " ***" : "***";
    }

    /**
     * 单个模型请求对应的日志文件。
     */
    private class RequestLogFile implements AutoCloseable {

        private final BufferedWriter writer;

        private final String fileName;

        /**
         * 保证 close 只真正执行一次。
         */
        private final AtomicBoolean closed = new AtomicBoolean(false);

        RequestLogFile() throws IOException {
            Path logDir = Paths.get(appProperties.getLogPath());
            Files.createDirectories(logDir);
            fileName = LocalDateTime.now().format(FILE_NAME_FORMATTER) + "_" + String.format("%08d",
                    ThreadLocalRandom.current().nextInt(100_000_000)) + ".log";
            writer = Files.newBufferedWriter(logDir.resolve(fileName), StandardCharsets.UTF_8);
        }

        /**
         * 写入普通日志行。
         */
        synchronized void writeLine(String line) {
            if (closed.get()) {
                return;
            }
            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                log.warn("Failed to write line to log file: {}, {}", fileName, e.getMessage());
            }
        }

        /**
         * 写入 SSE 数据。
         * <p>
         * DataBuffer 本身可能包含多行：
         * <p>
         * data: xxx data: yyy
         * <p>
         * 所以每个物理行都增加 [SSE] 前缀。
         */
        synchronized void writeSse(String content) {
            if (closed.get()) {
                return;
            }
            try {
                String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
                String[] lines = normalized.split("\n", -1);
                for (String line : lines) {
                    if (StringUtils.isEmpty(line)) {
                        continue;
                    }
                    writer.write("\n[SSE] ");
                    writer.write(line);
                    writer.newLine();
                }
                writer.flush();
            } catch (IOException e) {
                log.warn("Failed to write SSE to log file: {}, {}", fileName, e.getMessage());
            }
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            try {
                writer.close();
                log.debug("Full request log has been write to file: {}", fileName);
            } catch (IOException e) {
                log.warn("Failed to close log file: {}, {}", fileName, e.getMessage());
            }
        }
    }

    /**
     * 请求 Body 日志包装器。
     */
    static class BodyLoggingHttpRequest implements ClientHttpRequest {

        private final ClientHttpRequest delegate;

        private final StringBuilder body = new StringBuilder();

        BodyLoggingHttpRequest(ClientHttpRequest delegate) {
            this.delegate = delegate;
        }

        String bodyLog() {
            return body.toString();
        }

        @NotNull
        @Override
        public HttpMethod getMethod() {
            return delegate.getMethod();
        }

        @NotNull
        @Override
        public URI getURI() {
            return delegate.getURI();
        }

        @NotNull
        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @NotNull
        @Override
        public MultiValueMap<String, HttpCookie> getCookies() {
            return delegate.getCookies();
        }

        @NotNull
        @Override
        public <T> T getNativeRequest() {
            return delegate.getNativeRequest();
        }

        @NotNull
        @Override
        public DataBufferFactory bufferFactory() {
            return delegate.bufferFactory();
        }

        @NotNull
        @Override
        public Mono<Void> writeWith(@NotNull Publisher<? extends DataBuffer> body) {
            return delegate.writeWith(Flux.from(body).doOnNext(this::appendBuffer));
        }

        @NotNull
        @Override
        public Mono<Void> writeAndFlushWith(@NotNull Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return delegate.writeAndFlushWith(Flux.from(body).map(inner -> Flux.from(inner).doOnNext(this::appendBuffer)));
        }

        private void appendBuffer(DataBuffer buffer) {
            int readerPosition = buffer.readPosition();
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            buffer.readPosition(readerPosition);
            body.append(new String(bytes, StandardCharsets.UTF_8));
        }

        @NotNull
        @Override
        public Mono<Void> setComplete() {
            return delegate.setComplete();
        }

        @Override
        public boolean isCommitted() {
            return delegate.isCommitted();
        }

        @Override
        public void beforeCommit(@NotNull Supplier<? extends Mono<Void>> action) {
            delegate.beforeCommit(action);
        }
    }
}