package com.bin.actx.tool.kit.sandbox;

import com.alibaba.opensandbox.sandbox.Sandbox;
import com.alibaba.opensandbox.sandbox.domain.exceptions.SandboxException;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.Execution;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.ExecutionError;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.OutputMessage;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.RunCommandRequest;
import com.alibaba.opensandbox.sandbox.domain.models.sandboxes.SandboxEndpoint;
import com.bin.actx.config.properties.AppProperties;
import com.bin.actx.sandbox.SandboxManager;
import com.bin.actx.tool.Toolkit;
import com.bin.actx.tool.model.ToolCallResult;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jiang.tools.text.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 命令执行工具
 *
 * @author Bin
 * @since 2026/7/9 11:42
 */
@Toolkit(name = "沙箱", description = "允许在沙箱中执行Shell命令", autoDiscover = false)
public class SandboxToolkit {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private SandboxManager sandboxManager;

    @Tool(name = "execute_on_temp_sandbox", description = "在临时沙箱中执行命令，执行后销毁沙箱，命令执行超时时间固定120秒")
    public ToolCallResult<TempSandboxCommandResult> executeOnTempSandbox(
            @ToolParam(description = "执行的命令") String command
    ) {
        long startTime = System.currentTimeMillis();

        Execution execution = sandboxManager.acquireFastSandbox(sandbox -> {
            RunCommandRequest request = RunCommandRequest.builder()
                    .command(command)
                    .timeout(Duration.ofSeconds(120))
                    .build();
            return sandbox.commands().run(request);
        });

        long endTime = System.currentTimeMillis();

        // 合并输出结果
        List<OutputMessage> stderr = execution.getLogs().getStderr();
        List<OutputMessage> stdout = execution.getLogs().getStdout();
        List<OutputMessage> mergedOutput = Stream.concat(stdout.stream(), stderr.stream())
                .sorted(Comparator.comparing(OutputMessage::getTimestamp))
                .toList();
        String outString = mergedOutput.stream().map(OutputMessage::getText).collect(Collectors.joining("\n"));

        // 获取执行时长
        long executionTimeInMillis;
        if (execution.getComplete() != null) {
            executionTimeInMillis = execution.getComplete().getExecutionTimeInMillis();
        } else {
            executionTimeInMillis = endTime - startTime;
        }

        TempSandboxCommandResult result = new TempSandboxCommandResult();
        result.setOutput(outString);
        result.setExecutionTimeInMillis(executionTimeInMillis);

        // 处理错误信息
        if (execution.getError() != null) {
            ExecutionError error = execution.getError();
            String errorString = error.getName() + ": " + error.getValue();
            result.setError(errorString);
        }

        return ToolCallResult.of(result);
    }

    @Tool(name = "create_short_sandbox", description = "创建短期沙箱")
    public ToolCallResult<ShortSandboxCreateResult> executeOnShortSandbox(
            @ToolParam(description = "指定开放的端口（会返回映射后的端点）", required = false) List<Integer> ports
    ) {
        ShortSandboxCreateResult result = new ShortSandboxCreateResult();

        // 创建沙箱（60分钟过期）
        try (Sandbox sandbox = sandboxManager.createSandbox(appProperties.getFastSandboxPoolImage(), Duration.ofMinutes(60))) {
            result.setSandboxId(sandbox.getId());

            // 开放端口
            if (ports != null && !ports.isEmpty()) {
                Map<Integer, String> endpointMap = new HashMap<>();
                for (Integer port : ports) {
                    SandboxEndpoint endpoint = sandbox.getEndpoint(port);
                    endpointMap.put(port, endpoint.getEndpoint());
                }
                result.setEndpoints(endpointMap);
            }

            return ToolCallResult.of(result, "Create short sandbox success, execute the command using the sandbox id.");
        } catch (Exception e) {
            return ToolCallResult.of(null, "Create short sandbox fail: " + e.getMessage());
        }
    }

    @Tool(name = "execute_on_short_sandbox", description = "在指定的短期沙箱中执行命令，命令执行超时时间固定120秒")
    public ToolCallResult<ShortSandboxCommandResult> executeOnShortSandbox(
            @ToolParam(description = "沙箱id（通过创建沙箱获取）", required = false) String sandboxId,
            @ToolParam(description = "执行的命令") String command
    ) {
        if (StringUtils.isEmpty(sandboxId)) {
            return ToolCallResult.of(null, "Please specify the sandbox id.");
        }

        Execution execution;
        long startTime = System.currentTimeMillis();
        try (Sandbox sandbox = sandboxManager.getSandbox(sandboxId)) {
            if (sandbox == null) {
                return ToolCallResult.of(null, "Unable to connect to the sandbox (this sandbox may have expired)");
            }

            // 续期60分钟
            sandbox.renew(Duration.ofMinutes(60));

            RunCommandRequest request = RunCommandRequest.builder()
                    .command(command)
                    .timeout(Duration.ofSeconds(120))
                    .build();

            execution = sandbox.commands().run(request);
        } catch (SandboxException e) {
            return ToolCallResult.of(null, "Unable to connect to the sandbox (this sandbox may have expired): " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();

        // 合并输出结果
        List<OutputMessage> stderr = execution.getLogs().getStderr();
        List<OutputMessage> stdout = execution.getLogs().getStdout();
        List<OutputMessage> mergedOutput = Stream.concat(stdout.stream(), stderr.stream())
                .sorted(Comparator.comparing(OutputMessage::getTimestamp))
                .toList();
        String outString = mergedOutput.stream().map(OutputMessage::getText).collect(Collectors.joining("\n"));

        // 获取执行时长
        long executionTimeInMillis;
        if (execution.getComplete() != null) {
            executionTimeInMillis = execution.getComplete().getExecutionTimeInMillis();
        } else {
            executionTimeInMillis = endTime - startTime;
        }

        ShortSandboxCommandResult result = new ShortSandboxCommandResult();
        result.setOutput(outString);
        result.setExecutionTimeInMillis(executionTimeInMillis);

        // 处理错误信息
        if (execution.getError() != null) {
            ExecutionError error = execution.getError();
            String errorString = error.getName() + ": " + error.getValue();
            result.setError(errorString);
        }

        return ToolCallResult.of(result);
    }

    @Tool(name = "kill_short_sandbox", description = "销毁指定的短期沙箱（在确认非持续任务完成后，必须销毁沙箱）")
    public ToolCallResult<Boolean> killShortSandbox(@ToolParam(description = "沙箱id") String sandboxId) {
        if (StringUtils.isEmpty(sandboxId)) {
            return ToolCallResult.of(null, "Please specify the sandbox id.");
        }

        try (Sandbox sandbox = sandboxManager.getSandbox(sandboxId)) {
            if (sandbox == null) {
                return ToolCallResult.of(false, "The sandbox has expired or has been killed.");
            }
            sandbox.kill();
        } catch (SandboxException e) {
            return ToolCallResult.of(false, "The sandbox has expired or has been killed: " + e.getMessage());
        }
        return ToolCallResult.of(true, "The sandbox was successfully killed.");
    }

}
