package com.bin.actx.sql.datasource;

import com.bin.actx.sql.security.DatabaseSecurityPolicy;
import com.bin.actx.sql.support.SqlAnalyzer;

/**
 * 数据库类型支持接口，定义不同数据库类型的特征行为。 扩展新的数据库类型时需实现此接口。
 *
 * @author Bin
 * @since 2026/8/11 15:20
 */
public interface DatabaseType {

    /**
     * 获取数据库类型名称
     *
     * @return 类型名称（如 "mysql", "postgresql"）
     */
    String getName();

    /**
     * 获取默认 JDBC 驱动类名
     *
     * @return 驱动类全限定名
     */
    String getDefaultDriverClassName();

    /**
     * 获取连接有效性验证 SQL
     *
     * @return 验证查询语句
     */
    String getValidationQuery();

    /**
     * 获取数据库安全策略
     *
     * @return 数据库安全策略
     */
    DatabaseSecurityPolicy getSecurityPolicy();

    /**
     * 获取数据库分析器
     *
     * @return 数据库分析器
     */
    SqlAnalyzer getAnalyzer();

}
