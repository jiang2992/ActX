package com.bin.actx.tool.kit.sandbox;

import java.util.Map;
import lombok.Data;

@Data
public class ShortSandboxCreateResult {

    private String sandboxId;

    private Map<Integer, String> endpoints;

}
