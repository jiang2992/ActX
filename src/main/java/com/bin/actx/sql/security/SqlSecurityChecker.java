package com.bin.actx.sql.security;

import com.bin.actx.sql.analyze.SqlUnionAnalyzer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;
import org.jiang.tools.collection.CollectionUtils;

/**
 * SQL安全检查器，基于 ShardingSphere SQL Parser 解析SQL并检查安全性
 *
 * @author Bin
 * @since 2026/8/25
 */
@Slf4j
public class SqlSecurityChecker {

    private final SqlUnionAnalyzer sqlUnionAnalyzer;

    public SqlSecurityChecker(SqlUnionAnalyzer sqlUnionAnalyzer) {
        this.sqlUnionAnalyzer = sqlUnionAnalyzer;
    }

    public SqlVerifyResult fullVerify() {
        DatabaseSecurityPolicy securityPolicy = sqlUnionAnalyzer.getDataSourceInstance().getDatabaseType().getSecurityPolicy();
        SqlVerifyResult sqlVerifyResult = new SqlVerifyResult();
        sqlVerifyResult.setSafe(true);

        StringBuilder message = new StringBuilder();
        if (!verifySqlType()) {
            sqlVerifyResult.setSafe(false);
            String allowedTypes = securityPolicy.getAllowedSqlTypes().stream().map(Class::getSimpleName).collect(
                    Collectors.joining(","));
            message.append("SQL type is not allowed, You can only use: ").append(allowedTypes).append("\n");
        } else {
            if (!verifyMaxRows()) {
                sqlVerifyResult.setSafe(false);
                message.append("You must specify the number of rows to be queried in the SELECT statement, and it must not exceed: ")
                        .append(securityPolicy.getMaxRows())
                        .append("\n");
            }
            if (!verifySingleStatement()) {
                sqlVerifyResult.setSafe(false);
                message.append("You can only use one SQL statement.\n");
            }
            if (!verifyForbiddenFunctions()) {
                sqlVerifyResult.setSafe(false);
                message.append("The following function is prohibited from being used: ")
                        .append(String.join(",", securityPolicy.getForbiddenFunctions())).append("\n");
            }
            if (!verifyForbiddenKeywords()) {
                sqlVerifyResult.setSafe(false);
                message.append("The following keyword is prohibited from being used: ")
                        .append(String.join(",", securityPolicy.getForbiddenKeywords())).append("\n");
            }
        }
        sqlVerifyResult.setMessage(!message.isEmpty() ? message.toString() : null);
        return sqlVerifyResult;
    }

    public boolean verifySqlType() {
        DatabaseSecurityPolicy securityPolicy = sqlUnionAnalyzer.getDataSourceInstance().getDatabaseType().getSecurityPolicy();
        if (securityPolicy == null) {
            log.warn("The SQL type check feature requires database types to support a security policy (DatabaseSecurityPolicy).");
            return true;
        }
        for (Class<? extends SQLStatement> allowedSqlType : securityPolicy.getAllowedSqlTypes()) {
            if (allowedSqlType.isInstance(sqlUnionAnalyzer.getSqlStatement())) {
                return true;
            }
        }
        return false;
    }

    public boolean verifyForbiddenFunctions() {
        DatabaseSecurityPolicy securityPolicy = sqlUnionAnalyzer.getDataSourceInstance().getDatabaseType().getSecurityPolicy();

        if (securityPolicy == null || CollectionUtils.isEmpty(securityPolicy.getForbiddenFunctions())) {
            log.warn("The SQL function check feature requires database types to support a security policy (DatabaseSecurityPolicy).");
            return true;
        }

        if (!sqlUnionAnalyzer.hasAnalyzer()) {
            log.warn("The SQL function check feature requires database types to support a specific analyzer.");
            return true;
        }

        Set<String> functions = sqlUnionAnalyzer.getUsedFunctions();

        for (String forbiddenFunction : securityPolicy.getForbiddenFunctions()) {
            if (forbiddenFunction == null || forbiddenFunction.isBlank()) {
                continue;
            }
            if (functions.contains(forbiddenFunction.trim().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        return true;
    }

    private boolean verifyForbiddenKeywords() {
        DatabaseSecurityPolicy securityPolicy = sqlUnionAnalyzer.getDataSourceInstance().getDatabaseType().getSecurityPolicy();

        if (securityPolicy == null || CollectionUtils.isEmpty(securityPolicy.getForbiddenKeywords())) {
            log.warn("The SQL keyword check feature requires database types to support a security policy (DatabaseSecurityPolicy).");
            return true;
        }

        if (!sqlUnionAnalyzer.hasAnalyzer()) {
            log.warn("The SQL keyword check feature requires database types to support a specific analyzer.");
            return true;
        }

        List<String> keywords = sqlUnionAnalyzer.getUsedKeywords();

        for (String forbiddenKeyword : securityPolicy.getForbiddenKeywords()) {
            if (forbiddenKeyword == null || forbiddenKeyword.isBlank()) {
                continue;
            }
            List<String> forbiddenTokens = Arrays.stream(forbiddenKeyword.trim().toLowerCase(Locale.ROOT).split("\\s+")).toList();
            if (containsTokens(keywords, forbiddenTokens)) {
                return false;
            }
        }

        return true;
    }

    public boolean verifySingleStatement() {
        ParseTree parent = sqlUnionAnalyzer.getParseAstNode().getRootNode().getParent();
        ParseTree parseTree = parent.getChild(parent.getChildCount() - 1);
        return parseTree instanceof TerminalNode terminalNode && terminalNode.getSymbol().getType() == CommonToken.EOF;
    }

    public boolean verifyMaxRows() {
        DatabaseSecurityPolicy securityPolicy = sqlUnionAnalyzer.getDataSourceInstance().getDatabaseType().getSecurityPolicy();

        if (securityPolicy == null || securityPolicy.getMaxRows() <= 0) {
            log.warn("The SQL limit check feature requires database types to support a security policy (DatabaseSecurityPolicy).");
            return true;
        }

        if (!sqlUnionAnalyzer.hasAnalyzer()) {
            log.warn("The SQL limit check feature requires database types to support a specific analyzer.");
            return true;
        }

        if (sqlUnionAnalyzer.getSqlStatement() instanceof SelectStatement) {
            Long limit = sqlUnionAnalyzer.getLimit();
            return limit != null && limit <= securityPolicy.getMaxRows();
        }

        return true;
    }

    public boolean containsTokens(List<String> tokens, List<String> target) {
        if (target.isEmpty() || target.size() > tokens.size()) {
            return false;
        }

        for (int i = 0; i <= tokens.size() - target.size(); i++) {
            boolean matched = true;
            for (int j = 0; j < target.size(); j++) {
                if (!tokens.get(i + j).equals(target.get(j))) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }

        return false;
    }

}
