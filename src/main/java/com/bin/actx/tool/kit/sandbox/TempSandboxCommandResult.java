package com.bin.actx.tool.kit.sandbox;

import lombok.Data;

@Data
public class TempSandboxCommandResult {

    private String output;

    private long executionTimeInMillis;

    private String error;

}
