package com.bin.actx.sql.datasource;

import lombok.Builder;
import lombok.Data;

/**
 * 数据源信息模型，描述一个已注册数据源的关键信息（含解析后的驱动类名等）
 *
 * @author Bin
 * @since 2026/8/13
 */
@Data
@Builder
public class DataSourceInfo {

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
     * JDBC 驱动类名（优先配置指定值，否则为类型默认值）
     */
    private String driverClassName;

    /**
     * 数据库版本
     */
    private String version;

}
