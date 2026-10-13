package com.bin.actx.sql.support.mysql;

import com.bin.actx.sql.support.SqlAnalyzer;
import java.util.HashSet;
import java.util.Set;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;
import org.apache.shardingsphere.sql.parser.engine.core.ParseASTNode;

public class MySqlAnalyzer extends SqlAnalyzer {

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
        if (node instanceof MySQLStatementParser.FunctionNameContext ||
                node instanceof MySQLStatementParser.RegularFunctionNameContext ||
                node instanceof MySQLStatementParser.JsonFunctionNameContext ||
                node instanceof MySQLStatementParser.AggregationFunctionNameContext
        ) {
            functions.add(node.getText().toLowerCase());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectFunctions(node.getChild(i), functions);
        }
    }

}
