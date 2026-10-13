package com.bin.actx.sql.security;

import java.util.Set;
import lombok.Data;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;

@Data
public class DatabaseSecurityPolicy {

    /**
     * 最大返回行数
     */
    private int maxRows;

    /**
     * 最大 SQL 长度
     */
    private int maxSqlLength;

    /**
     * 最大执行时间（秒）
     */
    private int maxExecutionTimeSeconds;

    /**
     * 最大结果集大小（b）
     */
    private long maxResultSize;

    /**
     * 允许的SQL类型
     */
    private Set<Class<? extends SQLStatement>> allowedSqlTypes;

    /**
     * 禁止使用的函数
     */
    private Set<String> forbiddenFunctions;

    /**
     * 禁止使用的关键字
     */
    private Set<String> forbiddenKeywords;

}
