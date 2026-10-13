package com.bin.actx.sql.rewriter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlRewriteResult {

    private String sql;

    private boolean rewritten;

}
