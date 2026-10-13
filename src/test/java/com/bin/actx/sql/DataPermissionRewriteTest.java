package com.bin.actx.sql;

import com.bin.actx.sql.analyze.SqlUnionAnalyzer;
import com.bin.actx.sql.datasource.DataSourceInstance;
import com.bin.actx.sql.rewriter.SqlRewriteResult;
import com.bin.actx.sql.rewriter.SqlUnionRewriter;
import com.bin.actx.sql.rewriter.condition.TableConditionRewriter;
import com.bin.actx.sql.rewriter.condition.TableConditionRule;
import com.bin.actx.sql.rewriter.condition.TableConditionRuleProvider;
import com.bin.actx.sql.support.mysql.MySqlDatabaseType;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DataPermissionRewriteTest {

    private final DataSourceInstance dataSourceInstance = new DataSourceInstance(null, new MySqlDatabaseType(), null);

    private SqlRewriteResult rewrite(String sql, List<TableConditionRule> rules) {
        SqlUnionAnalyzer analyzer = new SqlUnionAnalyzer(sql, dataSourceInstance);
        SqlUnionRewriter rewriter = new SqlUnionRewriter(analyzer);
        TableConditionRuleProvider provider = () -> rules;
        SqlRewriteResult result = new TableConditionRewriter(rewriter, List.of(provider)).rewrite();
        // 重写后的SQL必须仍可被正常解析
        if (result.isRewritten()) {
            new SqlUnionAnalyzer(result.getSql(), dataSourceInstance);
        }
        return result;
    }

    private TableConditionRule rule(String table, String column, TableConditionRule.Operator operator, Object... values) {
        TableConditionRule rule = new TableConditionRule();
        rule.setTable(table);
        rule.setColumn(column);
        rule.setOperator(operator);
        rule.setValues(List.of(values));
        return rule;
    }

    @Test
    public void testNoWhere() {
        SqlRewriteResult result = rewrite("SELECT * FROM user",
                List.of(rule("user", "dept_id", TableConditionRule.Operator.IN, 1, 2, 3)));
        System.out.println(result.getSql());
    }

    @Test
    public void testWithWhere() {
        SqlRewriteResult result = rewrite("SELECT * FROM user WHERE age > 18 AND name = 'a'",
                List.of(rule("user", "dept_id", TableConditionRule.Operator.IN, 1, 2)));
        System.out.println(result.getSql());
    }

    @Test
    public void testJoin() {
        SqlRewriteResult result = rewrite(
                "SELECT u.id, d.name FROM user u JOIN dept d ON u.dept_id = d.id WHERE u.age > 18",
                List.of(rule("user", "dept_id", TableConditionRule.Operator.EQ, 100)));
        System.out.println(result.getSql());
    }

    @Test
    public void testUnion() {
        SqlRewriteResult result = rewrite("SELECT id FROM user UNION SELECT id FROM admin",
                List.of(rule("user", "dept_id", TableConditionRule.Operator.IN, 1, 2)));
        System.out.println(result.getSql());
    }

    @Test
    public void testSubqueryInWhere() {
        SqlRewriteResult result = rewrite(
                "SELECT * FROM user WHERE id IN (SELECT user_id FROM user_role)",
                List.of(rule("user_role", "role_id", TableConditionRule.Operator.EQ, 1)));
        System.out.println(result.getSql());
    }

    @Test
    public void testIsNull() {
        SqlRewriteResult result = rewrite("SELECT * FROM user",
                List.of(rule("user", "deleted_at", TableConditionRule.Operator.IS_NULL)));
        System.out.println(result.getSql());
    }

    @Test
    public void testNoMatch() {
        SqlRewriteResult result = rewrite("SELECT * FROM product",
                List.of(rule("user", "dept_id", TableConditionRule.Operator.IN, 1)));
        System.out.println("rewritten=" + result.isRewritten() + " sql=" + result.getSql());
    }

    @Test
    public void testSubqueryInFrom() {
        SqlRewriteResult result = rewrite("SELECT * FROM (SELECT * FROM user) t WHERE t.age > 18",
                List.of(rule("user", "dept_id", TableConditionRule.Operator.EQ, 10)));
        System.out.println(result.getSql());
    }

    @Test
    public void testGroupByLimit() {
        SqlRewriteResult result = rewrite("SELECT dept_id, COUNT(*) FROM user GROUP BY dept_id ORDER BY dept_id LIMIT 10",
                List.of(rule("user", "region", TableConditionRule.Operator.EQ, "east")));
        System.out.println(result.getSql());
    }

    @Test
    public void testStringEscape() {
        SqlRewriteResult result = rewrite("SELECT * FROM user",
                List.of(rule("user", "name", TableConditionRule.Operator.NE, "O'Brien")));
        System.out.println(result.getSql());
    }

    @Test
    public void testMultiRules() {
        SqlRewriteResult result = rewrite("SELECT u.id FROM user u JOIN dept d ON u.dept_id = d.id",
                List.of(
                        rule("user", "status", TableConditionRule.Operator.IS_NOT_NULL),
                        rule("dept", "region", TableConditionRule.Operator.NOT_IN, "secret", "top")));
        System.out.println(result.getSql());
    }

    @Test
    public void testWildcardAll() {
        SqlRewriteResult result = rewrite("SELECT u.id FROM user u JOIN dept d ON u.dept_id = d.id",
                List.of(rule("*", "tenant_id", TableConditionRule.Operator.EQ, 1)));
        System.out.println(result.getSql());
    }

    @Test
    public void testWildcardPrefix() {
        SqlRewriteResult result = rewrite("SELECT * FROM sys_user JOIN sys_role ON sys_user.role_id = sys_role.id",
                List.of(rule("sys_*", "tenant_id", TableConditionRule.Operator.EQ, 1)));
        System.out.println(result.getSql());
    }

}
