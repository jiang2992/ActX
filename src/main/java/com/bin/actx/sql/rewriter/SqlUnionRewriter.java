package com.bin.actx.sql.rewriter;

import com.bin.actx.sql.analyze.SqlUnionAnalyzer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;

/**
 * SQL统一重写器
 * <p>
 * 提供与数据库方言无关的通用重写能力：基于解析结果中各片段的字符偏移， 在原始SQL上收集文本编辑操作，最后统一应用生成重写后的SQL。
 * </p>
 *
 * @author Bin
 * @since 2026/9/1 16:31
 */
public class SqlUnionRewriter {

    private static final String WHERE_KEYWORD = "WHERE";

    @Getter
    private final SqlUnionAnalyzer sqlUnionAnalyzer;

    private final List<SqlRewriteEdit> edits = new ArrayList<>();

    public SqlUnionRewriter(SqlUnionAnalyzer sqlUnionAnalyzer) {
        this.sqlUnionAnalyzer = sqlUnionAnalyzer;
    }

    /**
     * 在原始SQL的指定位置插入文本
     *
     * @param position 原始SQL中的字符偏移，插入文本位于该字符之前
     * @param text     待插入文本
     */
    public void insert(int position, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        edits.add(new SqlRewriteEdit(position, text));
    }

    /**
     * 是否存在编辑操作
     */
    public boolean hasEdits() {
        return !edits.isEmpty();
    }

    /**
     * 为SELECT语句的WHERE子句追加AND条件
     * <p>
     * 已存在WHERE子句时，将原条件整体括号包裹并在其前拼接新条件； 不存在时在FROM子句之后插入新的WHERE子句。
     * </p>
     *
     * @param selectStatement SELECT语句
     * @param condition       追加的条件表达式
     */
    public void addWhereCondition(SelectStatement selectStatement, String condition) {
        Optional<WhereSegment> where = selectStatement.getWhere();
        if (where.isPresent()) {
            WhereSegment whereSegment = where.get();
            insert(whereSegment.getStartIndex() + WHERE_KEYWORD.length(), " (" + condition + ") AND (");
            insert(whereSegment.getStopIndex() + 1, ")");
            return;
        }
        selectStatement.getFrom().ifPresent(from -> insert(from.getStopIndex() + 1, " WHERE (" + condition + ")"));
    }

    /**
     * 应用全部编辑操作，生成重写后的SQL
     */
    public String build() {
        StringBuilder sb = new StringBuilder(sqlUnionAnalyzer.getSql());
        edits.stream()
                .sorted(Comparator.comparingInt(SqlRewriteEdit::getPosition).reversed())
                .forEach(edit -> sb.insert(edit.getPosition(), edit.getText()));
        return sb.toString();
    }

    /**
     * 单次文本编辑操作
     */
    @Data
    @AllArgsConstructor
    private static class SqlRewriteEdit {

        private int position;

        private String text;

    }

}
