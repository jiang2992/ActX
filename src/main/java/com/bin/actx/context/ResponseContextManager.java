package com.bin.actx.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.springframework.stereotype.Component;

/**
 * 工具上下文管理，因为SpringAi自带的ToolContext无法正常保存对象（会进行序列化）<br/> 目前主要用来存储响应上下文
 *
 * @author Bin
 * @since 2026/7/23 16:39
 */
@Component
public class ResponseContextManager {

    /**
     * key只会存活最高60分钟
     */
    private final Map<String, ResponseContext> map = new PassiveExpiringMap<>(60, TimeUnit.MINUTES, new ConcurrentHashMap<>());

    public void add(ResponseContext responseContext) {
        map.put(responseContext.getId(), responseContext);
    }

    public ResponseContext getByToolContext(Map<String, Object> toolContext) {
        String responseContextId = (String) toolContext.get("responseContextId");
        if (responseContextId == null) {
            return null;
        }
        return map.get(responseContextId);
    }

    public ResponseContext getBySessionId(String sessionId) {
        return map.values().stream()
                .filter(responseContext -> sessionId.equals(responseContext.getRequestContext().getSessionId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 移除响应上下文（尽可能调用该方法手动删除，否则需要等60分钟才会释放，浪费内存）
     *
     * @param responseContextId 响应上下文id
     */
    public void remove(String responseContextId) {
        map.remove(responseContextId);
    }

}
