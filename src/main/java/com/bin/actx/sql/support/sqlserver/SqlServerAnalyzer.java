package com.bin.actx.sql.support.sqlserver;

import com.bin.actx.sql.support.SqlAnalyzer;
import java.util.HashSet;
import java.util.Set;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.shardingsphere.sql.parser.autogen.SQLServerStatementParser;
import org.apache.shardingsphere.sql.parser.engine.core.ParseASTNode;

public class SqlServerAnalyzer extends SqlAnalyzer {

    @Override
    public Set<String> getUsedFunctions(ParseASTNode ast) {
        Set<String> functions = new HashSet<>();
        collectFunctions(ast.getRootNode(), functions);
        return functions;
    }

    private void collectFunctions(ParseTree node, Set<String> functions) {
        if (node == null) {
            return;
        }

        if (node instanceof SQLServerStatementParser.FunctionNameContext ||
                node instanceof SQLServerStatementParser.RegularFunctionNameContext ||
                node instanceof SQLServerStatementParser.AggregationFunctionNameContext ||
                node instanceof SQLServerStatementParser.GraphAggFunctionNameContext
        ) {
            functions.add(node.getText().toLowerCase());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectFunctions(node.getChild(i), functions);
        }
    }

}
