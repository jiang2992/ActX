package com.bin.actx.config;

import com.bin.actx.config.properties.AppProperties;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * NEO4J配置
 *
 * @author Bin
 * @since 2026/8/12 10:21
 */
@Slf4j
@Configuration
public class Neo4jConfig {

    @Autowired
    private AppProperties appProperties;

    @Bean
    public Driver neo4jDriver() {
        // 配置连接池
        Config config = Config.builder()
                .withMaxConnectionPoolSize(10)
                .withConnectionAcquisitionTimeout(15, TimeUnit.SECONDS)
                .withMaxConnectionLifetime(60, TimeUnit.MINUTES)
                .build();
        Driver driver = GraphDatabase.driver(appProperties.getStorageGraphUrl(),
                AuthTokens.basic(appProperties.getStorageGraphUser(), appProperties.getStorageGraphPassword()),
                config
        );
    
        // 读取并执行业务模型初始化脚本（约束与索引）
        initializeBusinessModel(driver);
    
        log.info("Connect to the graph database: {}", appProperties.getStorageGraphUrl());
        return driver;
    }
    
    /**
     * 读取 classpath 下的 BusinessModel.cypher 并逐条执行，
     * 用于初始化业务模型的约束与索引（语句均为 IF NOT EXISTS，可重复执行）。
     */
    private void initializeBusinessModel(Driver driver) {
        try {
            ClassPathResource resource = new ClassPathResource("data/BusinessModel.cypher");
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            List<String> statements = Arrays.stream(content.split(";"))
                    .map(s -> s.replaceAll("(?m)^\\s*//.*$", ""))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            try (Session session = driver.session()) {
                for (String statement : statements) {
                    session.run(statement);
                }
            }
            log.info("Business model schema initialized with {} statements", statements.size());
        } catch (Exception e) {
            log.error("Failed to initialize business model schema", e);
        }
    }

}
