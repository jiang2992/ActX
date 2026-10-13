package com.bin.actx.sql.analyze;

import com.bin.actx.exception.SqlAnalyzeException;
import com.bin.actx.sql.datasource.DataSourceInstance;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.apache.shardingsphere.sql.parser.engine.api.CacheOption;
import org.apache.shardingsphere.sql.parser.engine.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.engine.api.SQLStatementVisitorEngine;
import org.apache.shardingsphere.sql.parser.engine.core.ParseASTNode;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;

/**
 * SQL统一解析器，负责解析通用部分及特定部分的转发
 *
 * @author Bin
 * @since 2026/9/1 16:27
 */
public class SqlUnionAnalyzer {

    private static final CacheOption CACHE_OPTION = new CacheOption(128, 1024L);

    @Getter
    private final String sql;

    @Getter
    private final DataSourceInstance dataSourceInstance;

    @Getter
    private final ParseASTNode parseAstNode;

    @Getter
    private final SQLStatement sqlStatement;

    public SqlUnionAnalyzer(String sql, DataSourceInstance dataSourceInstance) {
        this.sql = sql;
        this.dataSourceInstance = dataSourceInstance;
        this.parseAstNode = this.parse();
        this.sqlStatement = this.visit(parseAstNode);
    }

    private ParseASTNode parse() {
        try {
            SQLParserEngine parserEngine = new SQLParserEngine(dataSourceInstance.getDatabaseType().getName(), CACHE_OPTION);
            return parserEngine.parse(sql, false);
        } catch (Exception e) {
            throw new SqlAnalyzeException("SQL parsing failed: " + sql, e);
        }
    }

    private SQLStatement visit(ParseASTNode parseAstNode) {
        try {
            SQLStatementVisitorEngine visitorEngine = new SQLStatementVisitorEngine(dataSourceInstance.getDatabaseType().getName());
            return visitorEngine.visit(parseAstNode);
        } catch (Exception e) {
            throw new SqlAnalyzeException("SQL parsing failed: " + sql, e);
        }
    }

    public boolean hasAnalyzer() {
        return dataSourceInstance.getDatabaseType().getAnalyzer() != null;
    }

    public List<String> getUsedKeywords() {
        if (!hasAnalyzer()) {
            throw new SqlAnalyzeException("No analyzer for database type: " + dataSourceInstance.getDatabaseType().getName());
        }
        return dataSourceInstance.getDatabaseType().getAnalyzer().getUsedKeywords(parseAstNode);
    }

    public Set<String> getUsedFunctions() {
        if (!hasAnalyzer()) {
            throw new SqlAnalyzeException("No analyzer for database type: " + dataSourceInstance.getDatabaseType().getName());
        }
        return dataSourceInstance.getDatabaseType().getAnalyzer().getUsedFunctions(parseAstNode);
    }

    public Long getLimit() {
        if (!hasAnalyzer()) {
            throw new SqlAnalyzeException("No analyzer for database type: " + dataSourceInstance.getDatabaseType().getName());
        }
        if (sqlStatement instanceof SelectStatement selectStatement) {
            return dataSourceInstance.getDatabaseType().getAnalyzer().getLimit(selectStatement);
        }
        throw new SqlAnalyzeException("GetLimit function must be used with a SelectStatement");
    }

}
