package com.bin.actx.sql.support.oracle;

import com.bin.actx.sql.datasource.DatabaseType;
import com.bin.actx.sql.security.DatabaseSecurityPolicy;
import com.bin.actx.sql.support.SqlAnalyzer;
import java.util.Set;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;

public class OracleDatabaseType implements DatabaseType {

    @Override
    public String getName() {
        return "oracle";
    }

    @Override
    public String getDefaultDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public String getValidationQuery() {
        return "SELECT 1 FROM DUAL";
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
                "UTL_FILE",
                "UTL_HTTP",
                "UTL_TCP",
                "UTL_SMTP",
                "UTL_INADDR",
                "DBMS_LOCK",
                "DBMS_SCHEDULER",
                "DBMS_JOB",
                "DBMS_XMLGEN"
        ));

        policy.setForbiddenKeywords(Set.of(
                "FOR UPDATE",
                "CREATE DATABASE LINK",
                "ALTER DATABASE",
                "DROP DATABASE",
                "GRANT",
                "REVOKE",
                "EXECUTE IMMEDIATE"
        ));

        return policy;
    }

    @Override
    public SqlAnalyzer getAnalyzer() {
        return new OracleAnalyzer();
    }

}
