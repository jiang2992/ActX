package com.bin.actx.sql;

import com.bin.actx.exception.SqlSecurityException;
import com.bin.actx.sql.analyze.SqlUnionAnalyzer;
import com.bin.actx.sql.datasource.DataSourceInstance;
import com.bin.actx.sql.security.SqlSecurityChecker;
import com.bin.actx.sql.support.mysql.MySqlDatabaseType;
import com.bin.actx.sql.support.oracle.OracleDatabaseType;
import com.bin.actx.sql.support.sqlserver.SqlServerDatabaseType;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SqlTest {

    @Test
    public void testSqlVerify() {
        DataSourceInstance dataSourceInstance = new DataSourceInstance(null, new MySqlDatabaseType(), null);
        final List<String> sqlList = List.of(
                "SELECT * FROM user;SELECT * FROM user;",
                "SELECT SLEEP(10);",
                "SELECT sleep(10);",
                "SELECT LOAD_FILE('/etc/passwd');",
                "SELECT BENCHMARK(1000000, MD5('test'));",
                "SELECT SLEEP (10);",
                "DROP TABLE user;",
                "TRUNCATE TABLE user;",
                "DELETE FROM user;",
                "SELECT * FROM user\n"
                        + "UNION\n"
                        + "SELECT * FROM admin;",
                "SELECT 'DROP TABLE user';",
                "SELECT 'SLEEP(10)';",
                "SELECT 'UNION SELECT * FROM admin';",
                "SELECT `drop` FROM user;",
                "SELECT drop_time FROM user;",
                "SELECT deleted_at FROM user;",
                "SELECT sleep_count FROM user;",
                "-- DROP TABLE user\n"
                        + "SELECT * FROM user;",
                "SELECT *\n"
                        + "FROM user\n"
                        + "/* DELETE FROM user */\n"
                        + "WHERE id = 1;",
                "SELECT IF(id = 1, SLEEP(10), 0)\n"
                        + "FROM user;",
                "SELECT CONCAT('abc', SLEEP(10));",
                "SELECT sleep_count FROM user;",
                "SELECT sleep FROM user;",
                "SELECT SLEEP FROM user;",
                "SELECT * FROM user INTO '/tmp/a';SELECT * FROM user INTO OUTFILE '/tmp/a';"
        );

        for (String sql : sqlList) {
            try {
                SqlUnionAnalyzer sqlUnionAnalyzer = new SqlUnionAnalyzer(sql, dataSourceInstance);
                SqlSecurityChecker sqlSecurityChecker = new SqlSecurityChecker(sqlUnionAnalyzer);
                boolean b = sqlSecurityChecker.fullVerify().isSafe();
                System.out.printf("%s%n - %s%n - %s%n%n", sql, sqlUnionAnalyzer.getSqlStatement().getClass(), b);
            } catch (SqlSecurityException e) {
                System.out.printf("%s%n - error: %s%n - %s%n%n", sql, e.getMessage(), false);
            }
        }
    }

    @Test
    public void testSqlVerify2() {
        DataSourceInstance dataSourceInstance = new DataSourceInstance(null, new OracleDatabaseType(), null);
        final List<String> sqlList = List.of(
                // ===== 正常 SELECT =====
                "SELECT * FROM USER_INFO",
                "SELECT ID, NAME FROM USER_INFO WHERE ID = 1",
                "SELECT COUNT(*) FROM USER_INFO",
                "SELECT SYSDATE FROM DUAL",
                "SELECT CURRENT_DATE FROM DUAL",
                "SELECT NVL(NAME, 'UNKNOWN') FROM USER_INFO",
                "SELECT TO_CHAR(SYSDATE, 'YYYY-MM-DD') FROM DUAL",
                "SELECT UPPER(NAME) FROM USER_INFO",

                // ===== FOR UPDATE，应该拦截 =====
                "SELECT * FROM USER_INFO FOR UPDATE",
                "SELECT * FROM USER_INFO WHERE ID = 1 FOR UPDATE",

                // ===== 字符串/注释，不能误判 =====
                "SELECT 'FOR UPDATE' FROM DUAL",
                "SELECT 'DBMS_LOCK.SLEEP(10)' FROM DUAL",
                "SELECT * FROM USER_INFO /* FOR UPDATE */",
                "SELECT * FROM USER_INFO -- FOR UPDATE\n",

                // ===== 普通函数 =====
                "SELECT ABS(-1) FROM DUAL",
                "SELECT ROUND(10.123, 2) FROM DUAL",
                "SELECT SUBSTR(NAME, 1, 3) FROM USER_INFO",
                "SELECT REGEXP_LIKE(NAME, 'TEST') FROM USER_INFO",

                // ===== 子查询 =====
                "SELECT * FROM USER_INFO WHERE ID IN (SELECT ID FROM USER_ROLE)",

                // ===== JOIN =====
                "SELECT A.ID, B.NAME FROM USER_INFO A JOIN DEPT B ON A.DEPT_ID = B.ID",

                // ===== 多语句 =====
                "SELECT * FROM user;SELECT * FROM user;"
        );

        for (String sql : sqlList) {
            try {
                SqlUnionAnalyzer sqlUnionAnalyzer = new SqlUnionAnalyzer(sql, dataSourceInstance);
                SqlSecurityChecker sqlSecurityChecker = new SqlSecurityChecker(sqlUnionAnalyzer);
                boolean b = sqlSecurityChecker.fullVerify().isSafe();
                System.out.printf("%s%n - %s%n - %s%n%n", sql, sqlUnionAnalyzer.getSqlStatement().getClass(), b);
            } catch (SqlSecurityException e) {
                System.out.printf("%s%n - error: %s%n - %s%n%n", sql, e.getMessage(), false);
            }
        }
    }


    @Test
    public void testSqlVerify3() {
        DataSourceInstance dataSourceInstance = new DataSourceInstance(null, new SqlServerDatabaseType(), null);
        List<String> sqlList = List.of(
                // ===== 正常 SELECT，应该通过 =====
                "SELECT * FROM users",
                "SELECT id, name FROM users WHERE id = 1",
                "SELECT COUNT(*) FROM users",
                "SELECT TOP 100 * FROM users",
                "SELECT TOP 100 id, name FROM users ORDER BY id DESC",
                "SELECT GETDATE()",
                "SELECT ISNULL(name, 'UNKNOWN') FROM users",
                "SELECT LEN(name) FROM users",
                "SELECT SUBSTRING(name, 1, 3) FROM users",
                "SELECT UPPER(name) FROM users",
                "SELECT * FROM users WHERE name LIKE '%test%'",

                // ===== JOIN，应该通过 =====
                "SELECT a.id, b.name " +
                        "FROM users a " +
                        "JOIN department b ON a.dept_id = b.id",

                // ===== 子查询，应该通过 =====
                "SELECT * FROM users " +
                        "WHERE id IN (SELECT user_id FROM user_role)",

                // ===== UNION =====
                "SELECT id FROM users " +
                        "UNION " +
                        "SELECT id FROM admins",

                // ===== 字符串中包含敏感关键字，不应该误判 =====
                "SELECT 'EXEC xp_cmdshell' AS test",
                "SELECT 'WAITFOR DELAY' AS test",
                "SELECT 'OPENROWSET' AS test",
                "SELECT 'BULK INSERT' AS test",

                // ===== 注释中的敏感关键字，不应该误判 =====
                "-- EXEC xp_cmdshell\nSELECT * FROM users",
                "SELECT * FROM users /* WAITFOR DELAY */",

                // ===== 高危函数/语法，应该拦截 =====
                "SELECT xp_cmdshell('whoami')",
                "SELECT OPENROWSET('SQLNCLI', 'Server=127.0.0.1;Trusted_Connection=yes;', 'SELECT 1')",
                "SELECT OPENDATASOURCE('SQLNCLI', 'Data Source=127.0.0.1;Integrated Security=SSPI').master.sys.databases",
                "SELECT * FROM OPENQUERY(TEST_LINK, 'SELECT 1')",

                // ===== 高危关键字，应该拦截 =====
                "EXEC xp_cmdshell 'whoami'",
                "EXECUTE xp_cmdshell 'whoami'",
                "BULK INSERT users FROM 'C:\\test.txt'",

                // ===== 多语句，应该拦截 =====
                "SELECT * FROM users; SELECT * FROM admins;",
                "SELECT 1; EXEC xp_cmdshell 'whoami'",
                "SELECT 1; WAITFOR DELAY '00:00:10'",

                // ===== 边界测试 =====
                "SELECT exec_time FROM users",
                "SELECT execute_time FROM users",
                "SELECT waitfor_time FROM users",
                "SELECT openquery_name FROM users",
                "SELECT 'a;b;c' AS test"
        );

        for (String sql : sqlList) {
            try {
                SqlUnionAnalyzer sqlUnionAnalyzer = new SqlUnionAnalyzer(sql, dataSourceInstance);
                SqlSecurityChecker sqlSecurityChecker = new SqlSecurityChecker(sqlUnionAnalyzer);
                boolean b = sqlSecurityChecker.fullVerify().isSafe();
                System.out.printf("%s%n - %s%n - %s%n%n", sql, sqlUnionAnalyzer.getSqlStatement().getClass(), b);
            } catch (SqlSecurityException e) {
                System.out.printf("%s%n - error: %s%n - %s%n%n", sql, e.getMessage(), false);
            }
        }
    }

}
