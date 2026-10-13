package com.bin.actx.sql.security;

import lombok.Data;

@Data
public class SqlVerifyResult {

    private boolean isSafe;

    private String message;

}
