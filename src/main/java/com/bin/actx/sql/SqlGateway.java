package com.bin.actx.sql;

import com.bin.actx.context.ResponseContext;
import com.bin.actx.exception.SqlAnalyzeException;
import com.bin.actx.sql.analyze.SqlUnionAnalyzer;
import com.bin.actx.sql.datasource.DataSourceInstance;
import com.bin.actx.sql.datasource.DataSourceManager;
import com.bin.actx.sql.rewriter.SqlRewriteResult;
import com.bin.actx.sql.rewriter.SqlUnionRewriter;
import com.bin.actx.sql.rewriter.condition.DefaultTableConditionRuleProvider;
import com.bin.actx.sql.rewriter.condition.TableConditionRewriter;
import com.bin.actx.sql.rewriter.condition.TableConditionRuleProvider;
import com.bin.actx.sql.security.SqlSecurityChecker;
import com.bin.actx.sql.security.SqlVerifyResult;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SqlGateway {

    @Autowired
    private DataSourceManager dataSourceManager;

    public List<SqlExecuteResult> route(SqlRouteRequest request) {
        List<SqlExecuteResult> results = new ArrayList<>();
        DataSourceInstance dataSourceInstance = dataSourceManager.getInstance(request.getDataSourceId());

        Map<Integer, String> waitExecutes = new HashMap<>();
        for (String sql : request.getSqlList()) {

            // 解析
            SqlUnionAnalyzer sqlUnionAnalyzer;
            try {
                sqlUnionAnalyzer = new SqlUnionAnalyzer(sql, dataSourceInstance);
            } catch (SqlAnalyzeException e) {
                results.add(SqlExecuteResult.parseFail(e.getMessage()));
                continue;
            }

            // 校验SQL类型、多语句、行数限制、禁止的函数、关键字等
            SqlSecurityChecker sqlSecurityChecker = new SqlSecurityChecker(sqlUnionAnalyzer);
            SqlVerifyResult verifyResult = sqlSecurityChecker.fullVerify();
            if (!verifyResult.isSafe()) {
                results.add(SqlExecuteResult.verifyFail(verifyResult.getMessage()));
                continue;
            }

            // 重写SQL
            if (request.isDataPermissionEnable()) {
                try {
                    SqlUnionRewriter sqlUnionRewriter = new SqlUnionRewriter(sqlUnionAnalyzer);
                    TableConditionRewriter tableConditionRewriter = new TableConditionRewriter(sqlUnionRewriter,
                            getDataPermissionRuleProviders(request.getResponseContext()));
                    SqlRewriteResult rewriteResult = tableConditionRewriter.rewrite();
                    sql = rewriteResult.getSql();
                } catch (Exception e) {
                    results.add(SqlExecuteResult.parseFail("SQL rewrite failed: " + e.getMessage()));
                    continue;
                }
            }

            waitExecutes.put(results.size(), sql);
            results.add(null);
        }

        try (Connection connection = dataSourceInstance.getDataSource().getConnection()) {
            connection.setReadOnly(true);
            try (Statement statement = connection.createStatement()) {
                for (Entry<Integer, String> waitExecute : waitExecutes.entrySet()) {
                    Integer index = waitExecute.getKey();
                    String sql = waitExecute.getValue();
                    SqlExecuteResult executeResult = this.execute(dataSourceInstance, statement, sql);
                    results.set(index, executeResult);
                }
            }
        } catch (Exception e) {
            for (Integer index : waitExecutes.keySet()) {
                if (results.get(index) != null) {
                    continue;
                }
                results.set(index, SqlExecuteResult.executeFail(waitExecutes.get(index), e.getMessage()));
            }
        }

        return results;
    }

    private SqlExecuteResult execute(DataSourceInstance dataSourceInstance, Statement statement, String sql) {
        try {
            log.debug("Executing SQL on DataSource [{}]: {}", dataSourceInstance.getInfo().getName(), sql);
            boolean hasResultSet = statement.execute(sql);
            if (hasResultSet) {
                int maxRows = dataSourceInstance.getDatabaseType().getSecurityPolicy().getMaxRows();
                statement.setMaxRows(maxRows);
                try (ResultSet resultSet = statement.getResultSet()) {
                    return SqlExecuteResult.executeSuccess(sql, formatResultSet(resultSet));
                }
            } else {
                return SqlExecuteResult.executeSuccess(sql, "执行成功（影响行数：%d）".formatted(statement.getUpdateCount()));

            }
        } catch (Exception e) {
            return SqlExecuteResult.executeFail(sql, e.getMessage());
        }
    }

    private List<TableConditionRuleProvider> getDataPermissionRuleProviders(ResponseContext responseContext) {
        List<TableConditionRuleProvider> providerList = new ArrayList<>();
        providerList.add(new DefaultTableConditionRuleProvider(responseContext));
        return providerList;
    }

    /**
     * 将查询结果集格式化为文本表格
     *
     * @param resultSet 查询结果集
     * @return 格式化后的文本
     * @throws SQLException 读取结果集失败时抛出
     */
    private String formatResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                sb.append(" | ");
            }
            sb.append(metaData.getColumnLabel(i));
        }
        while (resultSet.next()) {
            sb.append('\n');
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    sb.append(" | ");
                }
                String value = resultSet.getString(i);
                sb.append(value == null ? "NULL" : value);
            }
        }
        return sb.toString();
    }

}
