package com.bin.actx.context;

import com.bin.actx.channel.ChatMessageChannel;
import com.bin.actx.channel.MessageChannel;
import com.bin.actx.message.AnswerSessionMessage;
import com.bin.actx.message.AnswerSessionMessageBuilder;
import com.bin.actx.message.RequestSessionMessage;
import com.bin.actx.plugin.PluginManager;
import com.bin.actx.plugin.hook.ResponseHook;
import com.bin.actx.util.MessageChannelProxy;
import com.bin.actx.util.SpringUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.text.StringUtils;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.ai.chat.metadata.Usage;

@Slf4j
public class ResponseContext {

    /**
     * 缓存插件消息通道的方法名称
     */
    private static final Map<Class<?>, Collection<String>> PLUGIN_MESSAGE_CHANNEL_METHOD_NAMES = new ConcurrentHashMap<>();

    @Getter
    public final String id;

    @Getter
    private final ChatMessageChannel messageChannel;

    @Getter
    private final RequestContext requestContext;

    @Getter
    private ResponseStopInfo stopInfo;

    private final List<ResponseHook> responseHooks;

    private List<MessageChannel> pluginMessageChannels;

    private AnswerSessionMessageBuilder answerSessionMessageBuilder;

    @Getter
    @Setter
    private Throwable throwable;

    @Getter
    private Usage usage;

    public ResponseContext(RequestContext requestContext, ChatMessageChannel messageChannel) {
        if (messageChannel == null) {
            messageChannel = new ChatMessageChannel() {
            };
        }

        PluginManager pluginManager = SpringUtils.getBean(PluginManager.class);
        this.responseHooks = pluginManager.getResponseHooks(requestContext.getAgentInstance());
        this.id = IdUtils.uuid();
        this.requestContext = requestContext;
        this.messageChannel = MessageChannelProxy.proxy(messageChannel, this::invoke);
    }

    public ResponseContext init(RequestSessionMessage requestSessionMessage) {
        this.pluginMessageChannels = new ArrayList<>();
        this.answerSessionMessageBuilder = new AnswerSessionMessageBuilder();

        for (ResponseHook responseHook : responseHooks) {
            MessageChannel pluginMessageChannel = responseHook.responseProc(this, requestSessionMessage, answerSessionMessageBuilder);
            if (pluginMessageChannel == null) {
                continue;
            }
            pluginMessageChannels.add(pluginMessageChannel);

            Class<? extends MessageChannel> aClass = pluginMessageChannel.getClass();
            if (aClass.isAnonymousClass()) {
                continue;
            }
            if (PLUGIN_MESSAGE_CHANNEL_METHOD_NAMES.containsKey(aClass)) {
                continue;
            }
            PLUGIN_MESSAGE_CHANNEL_METHOD_NAMES.put(aClass, Arrays.stream(aClass.getMethods()).map(Method::getName).toList());
        }

        return this;
    }

    public AnswerSessionMessage complete() {
        return answerSessionMessageBuilder.build();
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
        answerSessionMessageBuilder.tokenTotal(usage.getTotalTokens());
    }

    public Map<String, Object> buildToolContext() {
        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("responseContextId", id);
        if (StringUtils.notEmpty(requestContext.getSessionId())) {
            toolContext.put("sessionId", requestContext.getSessionId());
        }
        return toolContext;
    }

    /**
     * 标记响应为停止（不会立马停止，需要到达下一个检查点）
     *
     * @param content 停止的原因
     */
    public void stop(String content) {
        this.stopInfo = new ResponseStopInfo(content);
        // TODO
    }

    private void invoke(Method method, Object[] args) {
        if (pluginMessageChannels == null) {
            return;
        }
        for (MessageChannel pluginMessageChannel : pluginMessageChannels) {
            try {
                if (PLUGIN_MESSAGE_CHANNEL_METHOD_NAMES.containsKey(pluginMessageChannel.getClass())) {
                    if (!PLUGIN_MESSAGE_CHANNEL_METHOD_NAMES.get(pluginMessageChannel.getClass()).contains(method.getName())) {
                        continue;
                    }
                }
                method.invoke(pluginMessageChannel, args);
            } catch (Exception e) {
                log.debug("Plugin message channel invoke failed: {}", e.getMessage());
            }
        }
    }

}
