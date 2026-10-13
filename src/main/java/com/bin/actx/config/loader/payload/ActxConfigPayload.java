package com.bin.actx.config.loader.payload;

import java.util.Map;
import lombok.Data;

/**
 * ActX系统配置载体类
 *
 * @author Bin
 * @since 2026/6/2 15:48
 */
@Data
public class ActxConfigPayload {

    /**
     * 场景配置映射：场景名称 -> 场景配置
     */
    private Map<String, SceneConfig> scenes;

    /**
     * 场景配置内部类
     */
    @Data
    public static class SceneConfig {

        private String agent;

    }

}
