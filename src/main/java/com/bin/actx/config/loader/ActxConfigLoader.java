package com.bin.actx.config.loader;

import com.bin.actx.config.loader.payload.ActxConfigPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * ActX系统配置加载器，用于从 actx.json 文件中加载系统配置
 *
 * @author Bin
 * @since 2026/6/2 15:48
 */
@Component
public class ActxConfigLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从指定的 actx.json 文件读取配置并注册到系统中
     *
     * @param path 配置文件路径
     * @throws IOException 文件读取失败时抛出
     */
    public ActxConfigPayload load(String path) throws IOException {
        File configFile = new File(path);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        return objectMapper.readValue(configFile, ActxConfigPayload.class);
    }

}
