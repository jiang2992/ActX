package com.bin.actx.sql.support.sqlserver;

import com.bin.actx.sql.datasource.DatabaseType;
import com.bin.actx.sql.security.DatabaseSecurityPolicy;
import com.bin.actx.sql.support.SqlAnalyzer;
import java.util.Set;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;

public class SqlServerDatabaseType implements DatabaseType {

    @Override
    public String getName() {
        return "sqlserver";
    }

    @Override
    public String getDefaultDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getValidationQuery() {
        return "SELECT 1";
    }

    @Override
    public DatabaseSecurityPolicy getSecurityPolicy() {
        DatabaseSecurityPolicy policy = new DatabaseSecurityPolicy();

        policy.setMaxRows(1000);
        policy.setMaxSqlLength(10_000);
        policy.setMaxExecutionTimeSeconds(10);
        policy.setMaxResultSize(1_000_000);

        policy.setAllowedSqlTypes(Set.of(
                SelectStatement.class
        ));

        policy.setForbiddenFunctions(Set.of(
                "XP_CMDSHELL",
                "OPENROWSET",
                "OPENDATASOURCE",
                "OPENQUERY"
        ));

        policy.setForbiddenKeywords(Set.of(
                "WAITFOR",
                "OPENROWSET",
                "OPENDATASOURCE",
                "OPENQUERY",
                "BULK INSERT",
                "EXEC",
                "EXECUTE"
        ));

        return policy;
    }

    @Override
    public SqlAnalyzer getAnalyzer() {
        return new SqlServerAnalyzer();
    }

}
