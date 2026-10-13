package com.bin.actx.task.result;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 任务调度结果
 *
 * @author Bin
 * @since 2026/6/23 11:34
 */
@Data
public class TaskDispatchResult implements Serializable {

    public static final String RESULT_RETRY = "retry";

    public static final String RESULT_CONTINUE = "continue";

    public static final String RESULT_TERMINATION = "termination";

    public static final String RESULT_LACK = "lack";

    private List<SubTaskDispatchResult> results;

    @Data
    public static class SubTaskDispatchResult implements Serializable {

        private String runtimeId;

        private String code;

        private String result;

        private String suggest;

    }

    public boolean isAllContinue() {
        if (results == null || results.isEmpty()) {
            return true;
        }
        return results.stream().allMatch(item -> item.getResult().equals(RESULT_CONTINUE));
    }

    public boolean isTermination() {
        if (results == null || results.isEmpty()) {
            return false;
        }
        return results.stream().anyMatch(item -> item.getResult().equals(RESULT_TERMINATION));
    }

    public String getTerminationSuggest() {
        if (results == null || results.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (SubTaskDispatchResult result : results) {
            if (result.getResult().equals(RESULT_TERMINATION)) {
                sb.append(result.getCode()).append(":").append(result.getSuggest()).append("\n");
            }
        }
        return sb.toString();
    }

}
