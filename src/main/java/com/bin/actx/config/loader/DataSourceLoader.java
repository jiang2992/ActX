package com.bin.actx.config.loader;

import com.bin.actx.config.loader.payload.DataSourcePayload;
import com.bin.actx.config.loader.payload.DataSourcePayload.DataSourceItem;
import com.bin.actx.sql.datasource.DataSourceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 数据源配置加载器，用于从 datasource.json 文件中加载外部数据源配置
 *
 * @author Bin
 * @since 2026/8/11 15:20
 */
@Component
public class DataSourceLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从指定的 datasource.json 文件中加载数据源配置
     *
     * @param path 配置文件路径
     * @return 数据源配置列表
     * @throws IOException 文件读取失败时抛出
     */
    public List<DataSourceConfig> load(String path) throws IOException {
        File configFile = new File(path);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        DataSourcePayload payload = objectMapper.readValue(configFile, DataSourcePayload.class);

        Map<String, DataSourceItem> dataSources = payload.getDataSources();
        if (dataSources == null || dataSources.isEmpty()) {
            return new ArrayList<>();
        }

        List<DataSourceConfig> configs = new ArrayList<>();
        for (Map.Entry<String, DataSourceItem> entry : dataSources.entrySet()) {
            String dsId = entry.getKey();
            DataSourceItem item = entry.getValue();

            DataSourceConfig config = DataSourceConfig.builder()
                    .id(dsId)
                    .type(item.getType())
                    .name(item.getName())
                    .url(item.getUrl())
                    .username(item.getUsername())
                    .password(item.getPassword())
                    .driverClassName(item.getDriverClassName())
                    .maxPoolSize(item.getMaxPoolSize())
                    .minIdle(item.getMinIdle())
                    .connectionTimeout(item.getConnectionTimeout())
                    .idleTimeout(item.getIdleTimeout())
                    .maxLifetime(item.getMaxLifetime())
                    .build();

            configs.add(config);
        }

        return configs;
    }

}
