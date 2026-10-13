package com.bin.actx.sql.datasource;

import lombok.Builder;
import lombok.Data;

/**
 * 数据源配置领域模型
 *
 * @author Bin
 * @since 2026/8/11 15:20
 */
@Data
@Builder
public class DataSourceConfig {

    /**
     * 数据源唯一标识（如 "db1"）
     */
    private String id;

    /**
     * 数据库类型：mysql / postgresql / oracle 等
     */
    private String type;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * JDBC 连接 URL
     */
    private String url;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * JDBC 驱动类名（可选，未指定时根据 type 自动推导）
     */
    private String driverClassName;

    /**
     * 连接池最大连接数
     */
    private Integer maxPoolSize;

    /**
     * 连接池最小空闲连接数
     */
    private Integer minIdle;

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectionTimeout;

    /**
     * 空闲超时时间（毫秒）
     */
    private Integer idleTimeout;

    /**
     * 连接最大存活时间（毫秒）
     */
    private Integer maxLifetime;

}
