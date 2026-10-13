package com.bin.actx.sql.rewriter.condition;

import com.bin.actx.sql.analyze.SqlUnionAnalyzer;
import com.bin.actx.sql.rewriter.SqlRewriteResult;
import com.bin.actx.sql.rewriter.SqlUnionRewriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.combine.CombineSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.BinaryOperationExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.ExistsSubqueryExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.InExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.ListExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.NotExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.QuantifySubqueryExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.UnaryOperationExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.subquery.SubqueryExpressionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.subquery.SubquerySegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.JoinTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SubqueryTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.TableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;

/**
 * 表条件重写器
 * <p>
 * 根据表条件规则，将条件注入到SELECT语句的WHERE子句中。 支持JOIN、子查询、UNION等结构，会递归处理SQL中引用到受控表的每一层查询。
 * </p>
 *
 * @author Bin
 * @since 2026/9/1 16:35
 */
@Slf4j
public class TableConditionRewriter {

    private final SqlUnionRewriter sqlUnionRewriter;
    private final List<TableConditionRuleProvider> providerList;

    public TableConditionRewriter(SqlUnionRewriter sqlUnionRewriter, List<TableConditionRuleProvider> providerList) {
        this.sqlUnionRewriter = sqlUnionRewriter;
        this.providerList = providerList;
    }

    /**
     * 执行重写
     *
     * @return 重写结果，未命中规则时返回原始SQL
     */
    public SqlRewriteResult rewrite() {
        SqlUnionAnalyzer analyzer = sqlUnionRewriter.getSqlUnionAnalyzer();
        String originalSql = analyzer.getSql();

        List<TableConditionRule> ruleList = collectRules();
        if (ruleList.isEmpty()) {
            return new SqlRewriteResult(originalSql, false);
        }

        SQLStatement sqlStatement = analyzer.getSqlStatement();
        if (sqlStatement instanceof SelectStatement selectStatement) {
            handleSelect(selectStatement, ruleList);
        }

        if (!sqlUnionRewriter.hasEdits()) {
            return new SqlRewriteResult(originalSql, false);
        }
        return new SqlRewriteResult(sqlUnionRewriter.build(), true);
    }

    /**
     * 汇总全部条件规则并过滤无效规则
     *
     * @return 条件规则列表
     */
    private List<TableConditionRule> collectRules() {
        List<TableConditionRule> ruleList = new ArrayList<>();
        for (TableConditionRuleProvider provider : providerList) {
            List<TableConditionRule> rules = provider.get();
            if (rules == null) {
                continue;
            }
            rules.stream()
                    .filter(Objects::nonNull)
                    .filter(rule -> rule.getTable() != null && !rule.getTable().isBlank())
                    .filter(rule -> rule.getColumn() != null && !rule.getColumn().isBlank())
                    .filter(rule -> rule.getOperator() != null)
                    .forEach(ruleList::add);
        }
        return ruleList;
    }

    /**
     * 递归处理SELECT语句：对当前层引用的受控表注入条件，并深入处理UNION、CTE及子查询
     *
     * @param selectStatement selectStatement
     * @param ruleList        条件规则列表
     */
    private void handleSelect(SelectStatement selectStatement, List<TableConditionRule> ruleList) {
        Optional<CombineSegment> combine = selectStatement.getCombine();
        if (combine.isPresent()) {
            // UNION等组合查询：分别处理每个子查询（外层FROM与左子查询共享，不能重复处理）
            handleSelect(combine.get().getLeft().getSelect(), ruleList);
            handleSelect(combine.get().getRight().getSelect(), ruleList);
            return;
        }

        // CTE子查询
        selectStatement.getWith().ifPresent(with -> with.getCommonTableExpressions().forEach(
                cte -> handleSelect(cte.getSubquery().getSelect(), ruleList)));

        // 收集当前层物理表并为受控表生成权限条件
        List<SimpleTableSegment> tableList = new ArrayList<>();
        selectStatement.getFrom().ifPresent(from -> collectTables(from, tableList, ruleList));
        String condition = buildCondition(tableList, ruleList);
        if (condition != null) {
            sqlUnionRewriter.addWhereCondition(selectStatement, condition);
        }

        // WHERE条件中可能包含引用受控表的子查询
        selectStatement.getWhere().ifPresent(where -> walkExpression(where.getExpr(), ruleList));
    }

    /**
     * 递归展开FROM子句：收集当前层物理表，遇到的子查询则深入处理
     *
     * @param tableSegment tableSegment
     * @param tableList    tableList
     * @param ruleList     条件规则列表
     */
    private void collectTables(TableSegment tableSegment, List<SimpleTableSegment> tableList, List<TableConditionRule> ruleList) {
        if (tableSegment instanceof SimpleTableSegment simpleTable) {
            tableList.add(simpleTable);
        } else if (tableSegment instanceof JoinTableSegment joinTable) {
            collectTables(joinTable.getLeft(), tableList, ruleList);
            collectTables(joinTable.getRight(), tableList, ruleList);
        } else if (tableSegment instanceof SubqueryTableSegment subqueryTable) {
            handleSelect(subqueryTable.getSubquery().getSelect(), ruleList);
        }
    }

    /**
     * 递归遍历表达式，处理其中包含的子查询
     *
     * @param expression expression
     * @param ruleList   条件规则列表
     */
    private void walkExpression(ExpressionSegment expression, List<TableConditionRule> ruleList) {
        if (expression instanceof SubqueryExpressionSegment subqueryExpression) {
            handleSelect(subqueryExpression.getSubquery().getSelect(), ruleList);
        } else if (expression instanceof SubquerySegment subquery) {
            handleSelect(subquery.getSelect(), ruleList);
        } else if (expression instanceof BinaryOperationExpression binaryOperation) {
            walkExpression(binaryOperation.getLeft(), ruleList);
            walkExpression(binaryOperation.getRight(), ruleList);
        } else if (expression instanceof InExpression inExpression) {
            walkExpression(inExpression.getLeft(), ruleList);
            walkExpression(inExpression.getRight(), ruleList);
        } else if (expression instanceof NotExpression notExpression) {
            walkExpression(notExpression.getExpression(), ruleList);
        } else if (expression instanceof UnaryOperationExpression unaryOperation) {
            walkExpression(unaryOperation.getExpression(), ruleList);
        } else if (expression instanceof ExistsSubqueryExpression existsSubquery) {
            walkExpression(existsSubquery.getSubquery(), ruleList);
        } else if (expression instanceof QuantifySubqueryExpression quantifySubquery) {
            walkExpression(quantifySubquery.getSubquery(), ruleList);
        } else if (expression instanceof ListExpression listExpression) {
            listExpression.getItems().forEach(item -> walkExpression(item, ruleList));
        }
    }

    /**
     * 为当前层受控表生成权限条件，多条件间以AND连接；无受控表时返回null
     */
    private String buildCondition(List<SimpleTableSegment> tableList, List<TableConditionRule> ruleList) {
        List<String> conditions = new ArrayList<>();
        for (SimpleTableSegment table : tableList) {
            String tableName = table.getTableName().getIdentifier().getValue();
            String qualifier = table.getAliasName().orElse(tableName);
            for (TableConditionRule rule : ruleList) {
                if (!matchTable(tableName, rule.getTable())) {
                    continue;
                }
                String condition = buildSingleCondition(qualifier, rule);
                if (condition != null) {
                    conditions.add(condition);
                }
            }
        }
        if (conditions.isEmpty()) {
            return null;
        }
        return String.join(" AND ", conditions);
    }

    /**
     * 判断表名是否命中规则的table，支持*通配符（*匹配任意表，也支持prefix_*、*_suffix形式），匹配忽略大小写
     */
    private boolean matchTable(String tableName, String ruleTable) {
        if (!ruleTable.contains("*")) {
            return tableName.equalsIgnoreCase(ruleTable);
        }
        StringBuilder regex = new StringBuilder();
        for (String part : ruleTable.split("\\*", -1)) {
            if (!part.isEmpty()) {
                regex.append(Pattern.quote(part));
            }
            regex.append(".*");
        }
        return tableName.matches("(?i)" + regex);
    }

    /**
     * 根据单条规则生成条件表达式，规则无效时返回null
     */
    private String buildSingleCondition(String qualifier, TableConditionRule rule) {
        String column = qualifier + "." + rule.getColumn();
        return switch (rule.getOperator()) {
            case EQ -> buildCompareCondition(column, "=", rule);
            case NE -> buildCompareCondition(column, "<>", rule);
            case IN -> buildInCondition(column, "IN", rule);
            case NOT_IN -> buildInCondition(column, "NOT IN", rule);
            case IS_NULL -> column + " IS NULL";
            case IS_NOT_NULL -> column + " IS NOT NULL";
        };
    }

    private String buildCompareCondition(String column, String operator, TableConditionRule rule) {
        if (rule.getValues() == null || rule.getValues().isEmpty()) {
            log.warn("Data permission rule has no values: table={}, column={}", rule.getTable(), rule.getColumn());
            return null;
        }
        return "(" + column + " " + operator + " " + formatValue(rule.getValues().get(0)) + ")";
    }

    private String buildInCondition(String column, String operator, TableConditionRule rule) {
        if (rule.getValues() == null || rule.getValues().isEmpty()) {
            log.warn("Data permission rule has no values: table={}, column={}", rule.getTable(), rule.getColumn());
            return null;
        }
        String values = rule.getValues().stream()
                .map(this::formatValue)
                .reduce((a, b) -> a + ", " + b)
                .orElseThrow();
        return "(" + column + " " + operator + " (" + values + "))";
    }

    /**
     * 将规则值格式化为SQL字面量，字符串类型做单引号转义防止SQL注入
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }

}
