package com.bin.actx.tool.model;

import lombok.Data;

/**
 * 工具包信息，记录懒加载工具包的名称、描述和Bean实例
 *
 * @author Bin
 * @since 2026/5/12 15:14
 */
@Data
public class ToolKitInfo {

    /**
     * 工具包名称
     */
    private String name;

    /**
     * 工具包描述，用于LLM选择工具包时的参考
     */
    private String description;

    /**
     * 工具包Bean实例
     */
    private Object bean;

    /**
     * 工具包Class
     */
    private Class<?> cls;

    /**
     * 是否自动发现
     */
    private boolean autoDiscover;

}
