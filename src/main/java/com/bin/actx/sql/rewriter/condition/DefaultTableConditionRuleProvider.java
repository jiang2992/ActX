package com.bin.actx.sql.rewriter.condition;

import com.bin.actx.config.constant.SceneConstant;
import com.bin.actx.context.ResponseContext;
import java.util.ArrayList;
import java.util.List;

public class DefaultTableConditionRuleProvider implements TableConditionRuleProvider {

    private final ResponseContext responseContext;

    public DefaultTableConditionRuleProvider(ResponseContext responseContext) {
        this.responseContext = responseContext;
    }

    @Override
    public List<TableConditionRule> get() {
        // 如果是业务发现，则不需要添加任何条件
        String scene = responseContext.getRequestContext().getScene();
        if (SceneConstant.BUSINESS_DISCOVER.equals(scene)) {
            return new ArrayList<>();
        }

        TableConditionRule delFlagRule = new TableConditionRule();
        delFlagRule.setTable("*");
        delFlagRule.setColumn("del_flag");
        delFlagRule.setOperator(TableConditionRule.Operator.EQ);
        delFlagRule.setValues(List.of("0"));

        return List.of(delFlagRule);
    }

}
