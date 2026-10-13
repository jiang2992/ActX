package com.bin.actx.sql.rewriter.condition;

import java.util.List;
import lombok.Data;

/**
 * 表条件规则
 *
 * @author Bin
 * @since 2026/9/3 11:33
 */
@Data
public class TableConditionRule {

    /**
     * 表名，支持通配符（*、sys_*、*_user）
     */
    private String table;

    /**
     * 列
     */
    private String column;

    /**
     * 操作符
     */
    private Operator operator;

    /**
     * 值
     */
    private List<Object> values;

    /**
     * 操作符
     */
    public enum Operator {
        EQ,
        IN,
        NE,
        NOT_IN,
        IS_NULL,
        IS_NOT_NULL
    }

}
