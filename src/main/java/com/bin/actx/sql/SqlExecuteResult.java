package com.bin.actx.sql;

import lombok.Data;

@Data
public class SqlExecuteResult {

    private Boolean parseResult;

    private Boolean verifyResult;

    private String executedSql;

    private String message;

    private String result;

    public static SqlExecuteResult executeSuccess(String executedSql, String result) {
        SqlExecuteResult sqlExecuteResult = new SqlExecuteResult();
        sqlExecuteResult.setParseResult(true);
        sqlExecuteResult.setVerifyResult(true);
        sqlExecuteResult.setExecutedSql(executedSql);
        sqlExecuteResult.setResult(result);
        return sqlExecuteResult;
    }

    public static SqlExecuteResult executeFail(String executedSql, String message) {
        SqlExecuteResult sqlExecuteResult = new SqlExecuteResult();
        sqlExecuteResult.setParseResult(true);
        sqlExecuteResult.setVerifyResult(true);
        sqlExecuteResult.setExecutedSql(executedSql);
        sqlExecuteResult.setMessage(message);
        return sqlExecuteResult;
    }

    public static SqlExecuteResult verifyFail(String message) {
        SqlExecuteResult sqlExecuteResult = new SqlExecuteResult();
        sqlExecuteResult.setParseResult(true);
        sqlExecuteResult.setVerifyResult(false);
        sqlExecuteResult.setMessage(message);
        return sqlExecuteResult;
    }

    public static SqlExecuteResult parseFail(String message) {
        SqlExecuteResult sqlExecuteResult = new SqlExecuteResult();
        sqlExecuteResult.setParseResult(false);
        sqlExecuteResult.setMessage(message);
        return sqlExecuteResult;
    }

}
