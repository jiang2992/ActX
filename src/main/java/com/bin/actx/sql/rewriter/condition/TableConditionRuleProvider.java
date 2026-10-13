package com.bin.actx.sql.rewriter.condition;

import java.util.List;

/**
 * 表条件规则提供器
 *
 * @author Bin
 * @since 2026/10/8 09:45
 */
public interface TableConditionRuleProvider {

    List<TableConditionRule> get();

}
