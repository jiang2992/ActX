package com.bin.actx.advisor;

import com.bin.actx.context.ResponseContext;
import com.bin.actx.exception.ToolCallingException;
import com.bin.actx.mcp.McpClientManager;
import com.bin.actx.model.ChatModelManager;
import com.bin.actx.model.factory.getter.ModelMessageGetter;
import com.bin.actx.plugin.PluginManager;
import com.bin.actx.plugin.hook.ToolHook;
import com.bin.actx.tool.ToolManager;
import com.bin.actx.tool.model.ToolCallResult;
import com.bin.actx.util.SpringUtils;
import jakarta.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.json.JsonUtils;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

/**
 * 工具调用顾问，主要用于捕获工具调用前后的事件，并允许修改工具调用后的请求信息。
 *
 * @author Bin
 * @since 2026/5/9 17:16
 */
@Slf4j
public class ToolAdvisor extends ToolCallAdvisor {

    private static final String MODEL_STREAM_ADVISOR_NAME = "stream";

    private final ResponseContext responseContext;
    private final List<ToolHook> toolHooks;
    private final ToolManager toolManager;
    private final McpClientManager mcpClientManager;
    private final ChatModelManager chatModelManager;

    public ToolAdvisor(ResponseContext responseContext) {
        super(SpringUtils.getBean(ToolCallingManager.class), 0);
        this.responseContext = responseContext;
        this.toolHooks = SpringUtils.getBean(PluginManager.class).getToolHooks(responseContext.getRequestContext().getAgentInstance());
        this.toolManager = SpringUtils.getBean(ToolManager.class);
        this.mcpClientManager = SpringUtils.getBean(McpClientManager.class);
        this.chatModelManager = SpringUtils.getBean(ChatModelManager.class);
    }

    @Override
    @Nonnull
    protected ChatClientRequest doInitializeLoop(@Nonnull ChatClientRequest request, @Nonnull CallAdvisorChain chain) {
        return super.doInitializeLoop(request, chain);
    }

    @Override
    @Nonnull
    protected ChatClientResponse doFinalizeLoop(@Nonnull ChatClientResponse response, @Nonnull CallAdvisorChain chain) {
        return super.doFinalizeLoop(response, chain);
    }

    @Override
    @Nonnull
    protected ChatClientRequest doBeforeCall(@Nonnull ChatClientRequest request, @Nonnull CallAdvisorChain chain) {
        // 工具调用后置处理
        Prompt prompt = request.prompt();
        for (ToolHook hook : toolHooks) {
            Prompt nextPrompt = hook.toolCallAfter(prompt, responseContext);
            if (nextPrompt != null) {
                prompt = nextPrompt;
            }
        }
        return ChatClientRequest.builder()
                .prompt(prompt)
                .context(request.context())
                .build();
    }

    @Override
    @Nonnull
    protected ChatClientResponse doAfterCall(@Nonnull ChatClientResponse chatClientResponse, @Nonnull CallAdvisorChain callAdvisorChain) {
        if (chatClientResponse.chatResponse() == null || !chatClientResponse.chatResponse().hasToolCalls()) {
            return chatClientResponse;
        }

        // 工具调用前置处理
        for (ToolHook hook : toolHooks) {
            hook.toolCallBefore(chatClientResponse, responseContext);
        }
        return super.doAfterCall(chatClientResponse, callAdvisorChain);
    }

    @Override
    @Nonnull
    public Flux<ChatClientResponse> adviseStream(@Nonnull ChatClientRequest request, @Nonnull StreamAdvisorChain chain) {
        ChatClientRequest streamRequest = request;
        if (request.prompt().getOptions() instanceof ToolCallingChatOptions toolOptions) {
            ToolCallingChatOptions optionsCopy = toolOptions.copy();
            optionsCopy.setInternalToolExecutionEnabled(false);
            streamRequest = ChatClientRequest.builder()
                    .prompt(new Prompt(request.prompt().getInstructions(), optionsCopy))
                    .context(request.context())
                    .build();
        }

        // 预取末尾的ChatModelStreamAdvisor，后续迭代直接调用它（它不依赖chain参数）
        StreamAdvisor modelStreamAdvisor = resolveModelStreamAdvisor(chain);

        return doStreamWithToolLoop(streamRequest, chain, modelStreamAdvisor, 0);
    }

    private StreamAdvisor resolveModelStreamAdvisor(StreamAdvisorChain chain) {
        return chain.getStreamAdvisors().stream()
                .filter(a -> a.getName().equals(MODEL_STREAM_ADVISOR_NAME))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ChatModelStreamAdvisor not found in advisor chain"));
    }

    /**
     * 处理工具调用的流式响应
     *
     * @param request            request
     * @param chain              chain
     * @param modelStreamAdvisor modelStreamAdvisor
     * @param iteration          iteration
     * @return flux
     */
    private Flux<ChatClientResponse> doStreamWithToolLoop(ChatClientRequest request, StreamAdvisorChain chain,
            StreamAdvisor modelStreamAdvisor, int iteration) {
        if (iteration >= 100) {
            return Flux.error(new ToolCallingException("Max tool call iterations exceeded"));
        }

        // 首次迭代使用chain（Deque只能消耗一次），后续直接调用ChatModelStreamAdvisor
        Flux<ChatClientResponse> responseFlux = (iteration == 0)
                ? chain.nextStream(request)
                : modelStreamAdvisor.adviseStream(request, chain);

        // 工具调用辅助，允许盲调
        List<ToolCallback> toolCallbackList = new ArrayList<>(toolManager.getToolCallbackList(false));
        List<ToolCallback> mcpToolCallbackList = mcpClientManager.getAllToolCallbacks();
        if (mcpToolCallbackList != null && !mcpToolCallbackList.isEmpty()) {
            toolCallbackList.addAll(mcpToolCallbackList);
        }
        ToolCallingChatOptions toolCallOptions = request.prompt().getOptions().copy();
        toolCallOptions.setToolCallbacks(toolCallbackList);

        StringBuilder answer = new StringBuilder();

        return responseFlux
                .takeWhile(response -> responseContext.getStopInfo() == null)
                .flatMap(lastResponse -> {
                    if (lastResponse.chatResponse() == null || lastResponse.chatResponse().getResult() == null) {
                        return Flux.just(lastResponse);
                    }
                    if (!lastResponse.chatResponse().hasToolCalls()) {
                        ModelMessageGetter modelMessageGetter = chatModelManager.getModelMessageGetter(
                                responseContext.getRequestContext().getAgentInstance().getConfig().getModelId());
                        String content = modelMessageGetter.getContent(lastResponse.chatResponse().getResult().getOutput());
                        if (StringUtils.notEmpty(content)) {
                            answer.append(content);
                        }
                        return Flux.just(lastResponse);
                    }

                    lastResponse = repairChatClientResponse(lastResponse);

                    // 工具调用前置处理
                    for (ToolHook hook : toolHooks) {
                        hook.toolCallBefore(lastResponse, responseContext);
                    }

                    // 执行工具调用
                    ToolExecutionResult toolExecutionResult;
                    try {
                        Prompt toolCallPrompt = new Prompt(request.prompt().getInstructions(), toolCallOptions);
                        toolExecutionResult = this.toolCallingManager
                                .executeToolCalls(toolCallPrompt, lastResponse.chatResponse());
                    } catch (Exception e) {
                        log.error("Failed to execute tool calls", e);
                        toolExecutionResult = this.buildToolExecutionErrorResult(request, lastResponse, e);
                    }

                    // 如果是直接返回
                    if (toolExecutionResult.returnDirect()) {
                        ChatClientResponse toolResponse = lastResponse.mutate()
                                .chatResponse(ChatResponse.builder()
                                        .from(lastResponse.chatResponse())
                                        .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                                        .build())
                                .build();
                        return Flux.just(toolResponse);
                    }

                    // 用更新后的会话历史构建下一轮请求
                    List<Message> updatedHistory = toolExecutionResult.conversationHistory();
                    if (!answer.isEmpty()) {
                        for (int i = updatedHistory.size() - 1; i >= 0; i--) {
                            if (updatedHistory.get(i) instanceof AssistantMessage am && !am.getToolCalls().isEmpty()) {
                                AssistantMessage merged = AssistantMessage.builder()
                                        .content(answer.toString())
                                        .toolCalls(am.getToolCalls())
                                        .properties(am.getMetadata())
                                        .build();
                                updatedHistory.set(i, merged);
                                break;
                            }
                        }
                        answer.setLength(0);
                    }
                    ToolCallingChatOptions optionsCopy = request.prompt().getOptions().copy();
                    optionsCopy.setInternalToolExecutionEnabled(false);
                    Prompt prompt = new Prompt(updatedHistory, optionsCopy);

                    // 工具调用后置处理
                    for (ToolHook hook : toolHooks) {
                        Prompt nextPrompt = hook.toolCallAfter(prompt, responseContext);
                        if (nextPrompt != null) {
                            prompt = nextPrompt;
                        }
                    }

                    ChatClientRequest nextRequest = ChatClientRequest.builder()
                            .prompt(prompt)
                            .context(request.context())
                            .build();

                    return doStreamWithToolLoop(nextRequest, chain, modelStreamAdvisor, iteration + 1);
                });
    }

    /**
     * 修复 ChatClientResponse，目前主要用来处理LLM传空tool.id或tool.name的情况
     *
     * @param lastResponse ChatClientResponse
     * @return ChatClientResponse
     */
    private ChatClientResponse repairChatClientResponse(ChatClientResponse lastResponse) {
        AssistantMessage assistantMessage = lastResponse.chatResponse().getResult().getOutput();
        List<ToolCall> newToolCalls = new ArrayList<>(assistantMessage.getToolCalls().size());
        boolean needReplaceToolCalls = false;
        for (ToolCall toolCall : assistantMessage.getToolCalls()) {
            if (StringUtils.isEmpty(toolCall.id()) || StringUtils.isEmpty(toolCall.name())) {
                toolCall = new ToolCall("unknown", toolCall.type(), "unknown", toolCall.arguments());
                newToolCalls.add(toolCall);
                needReplaceToolCalls = true;
            } else {
                newToolCalls.add(toolCall);
            }
        }

        if (needReplaceToolCalls) {
            try {
                Field toolCallsField = AssistantMessage.class.getDeclaredField("toolCalls");
                toolCallsField.setAccessible(true);
                toolCallsField.set(assistantMessage, newToolCalls);
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                log.warn("Failed to replace toolCalls via reflection", ex);
            }
        }

        return lastResponse;
    }

    /**
     * 当调用工具发生异常时，构造引导性消息给LLM
     *
     * @param request      ChatClientRequest
     * @param lastResponse ChatClientResponse
     * @param e            Exception
     * @return ToolExecutionResult
     */
    private ToolExecutionResult buildToolExecutionErrorResult(ChatClientRequest request, ChatClientResponse lastResponse, Exception e) {
        AssistantMessage assistantMessage = lastResponse.chatResponse().getResult().getOutput();

        String message = "Failed to execute tool calls: " + e.getMessage();
        if (e instanceof IllegalStateException) {
            message += " (The toolkit needs to be loaded first)";
        } else if (e instanceof NullPointerException) {
            message += " (It is necessary to strictly specify the values of tool.id and tool.name)";
        }

        ToolCallResult<Object> objectToolCallResult = ToolCallResult.of(null, message);
        String toolCallResult = JsonUtils.toJson(objectToolCallResult);
        List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
        for (ToolCall toolCall : assistantMessage.getToolCalls()) {
            toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), toolCallResult));
        }
        ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder().responses(toolResponses).build();
        List<Message> instructions = new ArrayList<>(request.prompt().getInstructions());
        instructions.add(assistantMessage);
        instructions.add(toolResponseMessage);
        return ToolExecutionResult.builder()
                .conversationHistory(instructions)
                .returnDirect(false)
                .build();
    }

}
