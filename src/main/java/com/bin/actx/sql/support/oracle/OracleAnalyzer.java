package com.bin.actx.sql.support.oracle;

import com.bin.actx.sql.support.SqlAnalyzer;
import java.util.HashSet;
import java.util.Set;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.shardingsphere.sql.parser.autogen.OracleStatementParser;
import org.apache.shardingsphere.sql.parser.engine.core.ParseASTNode;

public class OracleAnalyzer extends SqlAnalyzer {

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

        if (node instanceof OracleStatementParser.FunctionNameContext ||
                node instanceof OracleStatementParser.RegularFunctionNameContext ||
                node instanceof OracleStatementParser.AggregationFunctionNameContext ||
                node instanceof OracleStatementParser.FunctionNameDefContext ||
                node instanceof OracleStatementParser.AnalyticFunctionNameContext ||
                node instanceof OracleStatementParser.FeatureFunctionNameContext ||
                node instanceof OracleStatementParser.HierFunctionNameContext ||
                node instanceof OracleStatementParser.LeadLagFunctionNameContext ||
                node instanceof OracleStatementParser.RankFunctionNameContext
        ) {
            functions.add(node.getText().toLowerCase());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectFunctions(node.getChild(i), functions);
        }
    }

}
