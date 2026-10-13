package com.bin.actx.task;

import com.bin.actx.service.TaskService;
import com.bin.actx.task.constant.SubTaskConstant;
import com.bin.actx.task.context.TaskExecuteContext;
import com.bin.actx.task.model.SponsorTaskInfo;
import com.bin.actx.task.model.SubTaskOutputInfo;
import com.bin.actx.task.model.SubTaskRuntime;
import com.bin.actx.task.model.SubTaskRuntime.Branch;
import com.bin.actx.task.model.TaskPlan;
import com.bin.actx.task.model.TaskPlan.SubTask;
import com.bin.actx.task.result.SubTaskExecuteResult;
import com.bin.actx.util.SpringUtils;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jiang.tools.text.StringUtils;

/**
 * 子任务执行器<br/> 只负责执行，不负责更新任务状态
 *
 * @author Bin
 * @since 2026/6/1 21:18
 */
public class SubTaskExecutor implements Closeable {

    public static final String DISPATCHER_AGENT_FLAG = "dispatcher_agent";

    /**
     * 任务执行上下文
     */
    private final TaskExecuteContext taskExecuteContext;

    /**
     * 线程池
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * 任务服务
     */
    private final TaskService taskService;

    /**
     * 任务服务
     */
    private final TaskManager taskManager;

    /**
     * 任务计划缓存器
     */
    private final TaskPlanBuffer taskPlanBuffer;

    public SubTaskExecutor(TaskExecuteContext taskExecuteContext) {
        this.taskExecuteContext = taskExecuteContext;
        this.taskService = SpringUtils.getBean(TaskService.class);
        this.taskManager = SpringUtils.getBean(TaskManager.class);
        this.taskPlanBuffer = new TaskPlanBuffer(taskExecuteContext.getTaskOrder().getPlan());
    }

    /**
     * 在子任务列表中查找发起者任务，如果没有则返回空列表
     *
     * @param subTaskRuntimes 子任务列表
     * @return 找到的发起者任务
     */
    public List<SponsorTaskInfo> discoverDispatcherTask(Collection<SubTaskRuntime> subTaskRuntimes) {
        List<SponsorTaskInfo> sponsorTaskInfoList = new LinkedList<>();
        // 判断是否需要调度者介入
        for (SubTaskRuntime subTaskRuntime : subTaskRuntimes) {
            if (subTaskRuntime.getStatus().equals(SubTaskConstant.RUNTIME_COMPLETED)) {
                continue;
            }

            SubTask subTask = subTaskRuntime.getTaskInfo();
            if (DISPATCHER_AGENT_FLAG.equals(subTask.getAssignedAgent())) {
                Map<String, String> taskInput = this.getTaskInput(subTaskRuntime);
                SponsorTaskInfo sponsorTaskInfo = new SponsorTaskInfo();
                sponsorTaskInfo.setId(subTaskRuntime.getId());
                sponsorTaskInfo.setDescription(subTaskRuntime.getDescription());
                sponsorTaskInfo.setExpectedData(subTask.getExpectedOutput());
                sponsorTaskInfo.setDependentInfo(taskInput);
                sponsorTaskInfoList.add(sponsorTaskInfo);
            }

            for (Branch branch : subTaskRuntime.getBranches()) {
                if (branch.getStatus().equals(SubTaskConstant.RUNTIME_COMPLETED)) {
                    continue;
                }
                if (branch.getType().equals(SubTaskConstant.RUNTIME_TYPE_LACK)) {
                    SponsorTaskInfo sponsorTaskInfo = new SponsorTaskInfo();
                    sponsorTaskInfo.setId(branch.getId());
                    sponsorTaskInfo.setDescription(branch.getDescription());
                    sponsorTaskInfoList.add(sponsorTaskInfo);
                }
            }
        }
        return sponsorTaskInfoList;
    }

    /**
     * 创建子任务执行Future
     *
     * @param subTaskRuntime 子任务运行时
     * @return 子任务执行Future
     */
    public CompletableFuture<SubTaskExecuteResult> createTaskFuture(SubTaskRuntime subTaskRuntime) {
        taskExecuteContext.getTaskMessageChannel().onSubTaskCreated(taskExecuteContext, subTaskRuntime.getTaskInfo());
        return CompletableFuture.supplyAsync(() -> this.execute(subTaskRuntime), executor);
    }

    /**
     * 并行执行子任务
     *
     * @param subTasks 子任务
     * @return 子任务执行结果
     */
    public Map<TaskPlan.SubTask, SubTaskExecuteResult> parallelExecute(Collection<SubTaskRuntime> subTasks) {
        Map<TaskPlan.SubTask, SubTaskExecuteResult> resultMap = new LinkedHashMap<>();
        List<CompletableFuture<SubTaskExecuteResult>> futures = subTasks.stream().map(this::createTaskFuture).toList();

        // 并行执行任务
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 处理任务结果
        for (CompletableFuture<SubTaskExecuteResult> future : futures) {
            SubTaskExecuteResult subTaskExecuteResult = future.join();
            SubTask subTask = taskPlanBuffer.getSubTask(subTaskExecuteResult.getCode());
            resultMap.put(subTask, subTaskExecuteResult);
        }

        return resultMap;
    }

    /**
     * 执行子任务
     *
     * @param subTaskRuntime 子任务
     * @return 子任务执行结果
     */
    public SubTaskExecuteResult execute(SubTaskRuntime subTaskRuntime) {
        SubTaskExecuteResult executeResult;
        try {
            taskExecuteContext.getTaskMessageChannel().onSubTaskRunning(taskExecuteContext, subTaskRuntime.getTaskInfo());
            Map<String, String> taskInput = this.getTaskInput(subTaskRuntime);
            List<String> taskRemark = this.getTaskRemark(subTaskRuntime);
            String output = taskService.executeSubTask(taskExecuteContext, subTaskRuntime.getTaskInfo(), taskInput, taskRemark);
            executeResult = SubTaskExecuteResult.success(subTaskRuntime.getId(), subTaskRuntime.getCode(), output);
        } catch (Exception e) {
            executeResult = SubTaskExecuteResult.failure(subTaskRuntime.getId(), subTaskRuntime.getCode(), e);
        }
        return executeResult;
    }

    /**
     * 获取任务输入
     *
     * @param subTaskRuntime 子任务
     * @return 任务输入
     */
    private Map<String, String> getTaskInput(SubTaskRuntime subTaskRuntime) {
        SubTask subTask = subTaskRuntime.getTaskInfo();

        Map<String, String> inputData = new LinkedHashMap<>();
        Set<String> subTasks = new HashSet<>();
        for (String input : subTask.getInput()) {
            if ("original_request".equals(input)) {
                inputData.put("Original task request", taskExecuteContext.getTaskOrder().getPlan().getTaskSummary());
                continue;
            }
            if ("task_group_all".equals(input)) {
                List<String> completedSubTasks = taskManager.getCompletedSubTaskCodes(taskExecuteContext.getTaskOrder().getId());
                subTasks.addAll(completedSubTasks);
                continue;
            }
            if (input.startsWith("task_group_")) {
                TaskPlan.TaskGroup tg = taskPlanBuffer.getTaskGroup(input);
                tg.getSubTasks().stream().map(SubTask::getCode).forEach(subTasks::add);
                continue;
            }
            if (input.startsWith("sub_task_")) {
                subTasks.add(input);
            }
        }

        if (subTasks.isEmpty()) {
            return inputData;
        }

        Map<String, SubTaskOutputInfo> subTaskOutputs = taskManager.getSubTaskOutputs(taskExecuteContext.getTaskOrder().getId(), subTasks);
        for (Entry<String, SubTaskOutputInfo> entry : subTaskOutputs.entrySet()) {
            TaskPlan.SubTask st = taskPlanBuffer.getSubTask(entry.getKey());
            TaskPlan.TaskGroup tg = taskPlanBuffer.getTaskGroupBySubTaskCode(st.getCode());
            String inputValue = entry.getValue().getOutput();
            if (StringUtils.notEmpty(entry.getValue().getAdditional())) {
                inputValue += "(任务结果备注：" + entry.getValue().getAdditional() + ")";
            }
            inputData.put(tg.getTitle() + " - " + st.getDescription(), inputValue);
        }

        return inputData;
    }

    /**
     * 获取任务输入
     *
     * @param subTaskRuntime 子任务
     * @return 任务输入
     */
    private List<String> getTaskRemark(SubTaskRuntime subTaskRuntime) {
        List<String> taskRemark = new ArrayList<>();
        if (subTaskRuntime.getBranches() != null && !subTaskRuntime.getBranches().isEmpty()) {
            List<String> retrySuggests = new ArrayList<>();
            for (Branch branch : subTaskRuntime.getBranches()) {
                if (branch.getType().equals(SubTaskConstant.RUNTIME_TYPE_LACK)) {
                    taskRemark.add("- 调度者对该任务执行的补充 - %s: %s".formatted(branch.getDescription(), branch.getOutput()));
                }
                if (branch.getType().equals(SubTaskConstant.RUNTIME_TYPE_HISTORY)) {
                    retrySuggests.add(
                            "  - 第%s次执行后，调度者给出的重试建议: %s".formatted(retrySuggests.size() + 1, branch.getAdditional()));
                }
            }
            if (!retrySuggests.isEmpty()) {
                taskRemark.add("- 这是该任务的第" + (retrySuggests.size() + 1) + "次重试:");
                taskRemark.addAll(retrySuggests);
            }
        }

        return taskRemark;
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

}
