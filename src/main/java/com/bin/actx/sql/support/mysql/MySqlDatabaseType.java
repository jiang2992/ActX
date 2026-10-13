package com.bin.actx.sql.support.mysql;

import com.bin.actx.sql.datasource.DatabaseType;
import com.bin.actx.sql.security.DatabaseSecurityPolicy;
import com.bin.actx.sql.support.SqlAnalyzer;
import java.util.Set;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.statement.mysql.dal.show.column.MySQLShowColumnsStatement;
import org.apache.shardingsphere.sql.parser.statement.mysql.dal.show.table.MySQLShowTablesStatement;

/**
 * MySQL 数据库类型支持实现
 *
 * @author Bin
 * @since 2026/8/11 15:20
 */
public class MySqlDatabaseType implements DatabaseType {

    @Override
    public String getName() {
        return "mysql";
    }

    @Override
    public String getDefaultDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
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
                SelectStatement.class,
                MySQLShowTablesStatement.class,
                MySQLShowColumnsStatement.class
        ));
        policy.setForbiddenFunctions(Set.of(
                "SLEEP",
                "BENCHMARK",
                "LOAD_FILE",
                "GET_LOCK",
                "RELEASE_LOCK"
        ));
        policy.setForbiddenKeywords(Set.of(
                "INTO OUTFILE",
                "INTO DUMPFILE",
                "LOAD DATA"
        ));
        return policy;
    }

    @Override
    public SqlAnalyzer getAnalyzer() {
        return new MySqlAnalyzer();
    }

}
