package com.bin.actx.plugin;

import com.bin.actx.util.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 插件基类
 *
 * @author Bin
 * @since 2026/4/17 11:04
 */
public abstract class BasePlugin {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    {
        SpringUtils.autowireBean(this);
    }

    /**
     * 获取插件名称，默认从类名中去掉"Plugin"后缀得到
     */
    public String getName() {
        String simpleName = this.getClass().getSimpleName();
        if (!simpleName.endsWith("Plugin")) {
            throw new IllegalStateException("Need overwrite getName method.");
        }
        return simpleName.substring(0, simpleName.length() - "Plugin".length());
    }

    protected void infoLog(String message) {
        log.info("[Plugin: {}] {}", this.getName(), message);
    }

    protected void debugLog(String message) {
        log.debug("[Plugin: {}] {}", this.getName(), message);
    }

    protected void warnLog(String message) {
        log.warn("[Plugin: {}] {}", this.getName(), message);
    }

    protected void errorLog(String message) {
        log.error("[Plugin: {}] {}", this.getName(), message);
    }

}
