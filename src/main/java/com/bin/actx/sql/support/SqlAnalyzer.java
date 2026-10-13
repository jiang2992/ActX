package com.bin.actx.sql.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.shardingsphere.sql.parser.engine.core.ParseASTNode;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.pagination.NumberLiteralPaginationValueSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.pagination.PaginationValueSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.pagination.limit.LimitSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;

/**
 * SQL安全分析器
 * <p>
 * 继承并重写方法来处理不同数据库之间的SQL差异化部分
 * </p>
 *
 * @author Bin
 * @since 2026/8/26 16:53
 */
public abstract class SqlAnalyzer {

    public abstract Set<String> getUsedFunctions(ParseASTNode ast);

    public List<String> getUsedKeywords(ParseASTNode ast) {
        List<String> keywords = new ArrayList<>();
        getUsedKeywords(ast.getRootNode(), keywords);
        return keywords;
    }

    private void getUsedKeywords(ParseTree node, List<String> tokens) {
        if (node == null) {
            return;
        }

        if (node instanceof TerminalNode terminalNode) {
            String text = terminalNode.getText();
            if (text != null && !text.isBlank()) {
                tokens.add(text.toLowerCase(Locale.ROOT));
            }
            return;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            getUsedKeywords(node.getChild(i), tokens);
        }
    }

    public Long getLimit(SelectStatement selectStatement) {
        LimitSegment limitSegment = selectStatement.getLimit().orElse(null);
        if (limitSegment == null) {
            return null;
        }
        PaginationValueSegment paginationValueSegment = limitSegment.getRowCount().orElse(null);
        if (paginationValueSegment == null) {
            return null;
        }
        if (paginationValueSegment instanceof NumberLiteralPaginationValueSegment segment) {
            return segment.getValue();
        }
        return null;
    }

}
