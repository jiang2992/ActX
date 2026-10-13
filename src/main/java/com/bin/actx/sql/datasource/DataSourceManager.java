package com.bin.actx.sql.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

/**
 * 数据源管理器，负责外部数据源的动态注册、管理与连接获取。 初始化时将所有配置的数据源注册到管理器并进行联通性测试。
 *
 * @author Bin
 * @since 2026/8/11 15:20
 */
@Slf4j
@Component
public class DataSourceManager {

    /**
     * 数据库类型支持映射，key 为类型标识（如 "mysql"），value 为对应的 DatabaseType 实现
     */
    private final Map<String, DatabaseType> databaseTypeMap = new HashMap<>();

    /**
     * 已注册的数据源连接池映射，key 为数据源标识
     */
    private final Map<String, DataSourceInstance> instanceMap = new LinkedHashMap<>();

    public DataSourceManager() {
        scanAndRegisterDatabaseTypes();
    }

    /**
     * 扫描 DatabaseType 的所有实现类，创建实例并注册
     */
    private void scanAndRegisterDatabaseTypes() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(DatabaseType.class));
        Set<BeanDefinition> candidates = provider.findCandidateComponents("com.bin.actx.sql.support");
        for (BeanDefinition candidate : candidates) {
            try {
                Class<?> clazz = Class.forName(candidate.getBeanClassName());
                DatabaseType databaseType = (DatabaseType) clazz.getDeclaredConstructor().newInstance();
                databaseTypeMap.put(databaseType.getName().toLowerCase(), databaseType);
                log.debug("Registered database type support: {}", databaseType.getName());
            } catch (Exception e) {
                log.warn("Failed to register database type: {}", candidate.getBeanClassName(), e);
            }
        }
    }

    /**
     * 注册所有数据源配置，初始化连接池并执行联通性测试
     *
     * @param configs 数据源配置列表
     */
    public void registerAll(List<DataSourceConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            log.debug("No data source configs to register.");
            return;
        }

        for (DataSourceConfig config : configs) {
            try {
                register(config);
            } catch (Exception e) {
                log.error("Failed to register data source: {} ({}), error: {}", config.getId(), config.getName(),
                        e.getMessage());
            }
        }
    }

    /**
     * 注册单个数据源配置，创建连接池并测试联通性
     *
     * @param config 数据源配置
     */
    private void register(DataSourceConfig config) {
        DataSourceInfo dataSourceInfo = toDataSourceInfo(config);

        log.info("Registering data source: {} ({}), type: {}", config.getId(), config.getName(), config.getType());

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ActxHikariPool-" + config.getId());

        // JDBC URL
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        String driverClassName = resolveDriverClassName(config);
        if (driverClassName != null) {
            hikariConfig.setDriverClassName(driverClassName);
        }

        // 连接池参数
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize() != null ? config.getMaxPoolSize() : 10);
        hikariConfig.setMinimumIdle(config.getMinIdle() != null ? config.getMinIdle() : 2);
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout() != null ? config.getConnectionTimeout() : 30000);
        hikariConfig.setIdleTimeout(config.getIdleTimeout() != null ? config.getIdleTimeout() : 600000);
        hikariConfig.setMaxLifetime(config.getMaxLifetime() != null ? config.getMaxLifetime() : 1800000);
        String validationQuery = resolveValidationQuery(config);
        if (validationQuery != null) {
            hikariConfig.setConnectionTestQuery(validationQuery);
        }

        // 创建连接池
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        // 联通性测试
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(5)) {
                log.warn("Data source connectivity test failed (isValid=false): {} ({})", config.getId(), config.getName());
                dataSource.close();
                return;
            }

            // 获取元数据
            DatabaseMetaData metaData = conn.getMetaData();
            dataSourceInfo.setVersion(metaData.getDatabaseProductName() + ":" + metaData.getDatabaseProductVersion());
        } catch (SQLException e) {
            log.error("Data source connectivity test failed: {} ({}), error: {}", config.getId(), config.getName(),
                    e.getMessage());
            dataSource.close();
            return;
        }
        log.info("Data source connectivity test passed: {} ({})", config.getId(), config.getName());

        instanceMap.put(config.getId(),
                new DataSourceInstance(dataSourceInfo, databaseTypeMap.get(config.getType().toLowerCase()), dataSource));

        log.info("Data source registered successfully: {} ({})", config.getId(), config.getName());
    }

    /**
     * 解析驱动类名：优先使用配置中指定的值，否则从 DatabaseType 推导
     *
     * @param config 数据源配置
     * @return 驱动类名，可能为 null
     */
    private String resolveDriverClassName(DataSourceConfig config) {
        if (config.getDriverClassName() != null && !config.getDriverClassName().isBlank()) {
            return config.getDriverClassName();
        }
        DatabaseType dbType = databaseTypeMap.get(config.getType().toLowerCase());
        if (dbType != null) {
            return dbType.getDefaultDriverClassName();
        }
        return null;
    }

    /**
     * 解析连接验证 SQL：从 DatabaseType 推导
     *
     * @param config 数据源配置
     * @return 验证 SQL，可能为 null
     */
    private String resolveValidationQuery(DataSourceConfig config) {
        DatabaseType dbType = databaseTypeMap.get(config.getType().toLowerCase());
        if (dbType != null) {
            return dbType.getValidationQuery();
        }
        return null;
    }

    /**
     * 将数据源配置转换为数据源信息（驱动类名解析为实际生效值）
     *
     * @param config 数据源配置
     * @return 数据源信息
     */
    private DataSourceInfo toDataSourceInfo(DataSourceConfig config) {
        return DataSourceInfo.builder()
                .id(config.getId())
                .type(config.getType())
                .name(config.getName())
                .url(config.getUrl())
                .username(config.getUsername())
                .driverClassName(resolveDriverClassName(config))
                .build();
    }

    /**
     * 根据数据源标识获取数据源信息（包含数据库类型、URL、驱动等）
     *
     * @param id 数据源标识
     * @return 数据源信息，不存在时返回 null
     */
    public DataSourceInfo getDataSourceInfo(String id) {
        DataSourceInstance instance = instanceMap.get(id);
        return instance != null ? instance.getInfo() : null;
    }

    /**
     * 获取全部数据源信息列表
     *
     * @return 数据源信息列表（按注册顺序）
     */
    public List<DataSourceInfo> getAllDataSourceInfos() {
        return instanceMap.values().stream().map(DataSourceInstance::getInfo).collect(Collectors.toList());
    }

    /**
     * 获取全部数据源标识列表
     *
     * @return 数据源标识列表（按注册顺序）
     */
    public List<String> getDataSourceIds() {
        return new ArrayList<>(instanceMap.keySet());
    }

    /**
     * 根据数据源标识获取数据源实例
     *
     * @param id 数据源标识
     * @return 数据源实例，不存在时返回 null
     */
    public DataSourceInstance getInstance(String id) {
        return instanceMap.get(id);
    }

    /**
     * 根据数据源标识获取数据库连接
     *
     * @param id 数据源标识（如 "db1"）
     * @return 数据库连接
     * @throws IllegalArgumentException 数据源未注册时抛出
     * @throws SQLException             获取连接失败时抛出
     */
    public Connection getConnection(String id) throws SQLException {
        DataSourceInstance instance = instanceMap.get(id);
        DataSource dataSource = instance != null ? instance.getDataSource() : null;
        if (dataSource == null) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        return dataSource.getConnection();
    }

    /**
     * 根据数据源标识获取底层 DataSource
     *
     * @param id 数据源标识
     * @return DataSource 实例
     * @throws IllegalArgumentException 数据源未注册时抛出
     */
    public DataSource getDataSource(String id) {
        DataSourceInstance instance = instanceMap.get(id);
        DataSource dataSource = instance != null ? instance.getDataSource() : null;
        if (dataSource == null) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        return dataSource;
    }

    /**
     * 检查指定数据源是否已注册
     *
     * @param id 数据源标识
     * @return true 如果已注册
     */
    public boolean contains(String id) {
        return instanceMap.containsKey(id);
    }

    /**
     * 关闭所有数据源连接池，在应用关闭时调用
     */
    @jakarta.annotation.PreDestroy
    public void destroy() {
        log.info("Closing all data sources...");
        for (Map.Entry<String, DataSourceInstance> entry : instanceMap.entrySet()) {
            String dsId = entry.getKey();
            DataSourceInstance instance = entry.getValue();
            DataSource dataSource = instance != null ? instance.getDataSource() : null;
            if (dataSource instanceof Closeable closeable) {
                try {
                    closeable.close();
                    log.debug("Closed data source: {}", dsId);
                } catch (IOException e) {
                    log.error("Failed to close data source: {}", dsId, e);
                }
            }
        }
        instanceMap.clear();
        log.info("All data sources closed.");
    }

}
