package com.bin.actx.util;

import com.bin.actx.channel.MessageChannel;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.function.BiConsumer;

/**
 * 消息通道代理工具类
 *
 * @author Bin
 * @since 2026/5/27 16:43
 */
public class MessageChannelProxy {

    @SuppressWarnings("unchecked")
    public static <T extends MessageChannel> T proxy(T messageChannel, BiConsumer<Method, Object[]> invokeMethod) {
        return (T) Proxy.newProxyInstance(messageChannel.getClass().getClassLoader(), messageChannel.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    try {
                        return method.invoke(messageChannel, args);
                    } finally {
                        if (Modifier.isPublic(method.getModifiers())) {
                            invokeMethod.accept(method, args);
                        }
                    }
                });
    }

}
