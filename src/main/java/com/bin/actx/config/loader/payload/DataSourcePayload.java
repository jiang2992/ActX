package com.bin.actx.config.loader.payload;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源配置载体类，对应 datasource.json 结构
 *
 * @author Bin
 * @since 2026/8/11 15:20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSourcePayload {

    /**
     * 数据源映射，key 为数据源唯一标识（如 "db1"）
     */
    private Map<String, DataSourceItem> dataSources;

    /**
     * 单个数据源配置项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceItem {

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
         * 连接池最大连接数，默认 10
         */
        private Integer maxPoolSize = 10;

        /**
         * 连接池最小空闲连接数，默认 2
         */
        private Integer minIdle = 2;

        /**
         * 连接超时时间（毫秒），默认 30000
         */
        private Integer connectionTimeout = 30000;

        /**
         * 空闲超时时间（毫秒），默认 600000
         */
        private Integer idleTimeout = 600000;

        /**
         * 连接最大存活时间（毫秒），默认 1800000
         */
        private Integer maxLifetime = 1800000;

    }

}
