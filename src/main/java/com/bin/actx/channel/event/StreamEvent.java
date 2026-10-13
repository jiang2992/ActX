package com.bin.actx.channel.event;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 流式聊天事件，携带事件类型和数据
 *
 * @author Bin
 * @since 2026/5/12 11:44
 */
@Getter
@AllArgsConstructor
public class StreamEvent {

    private final StreamEventType type;

    private final String data;

    /**
     * 工具调用事件载荷
     */
    @Data
    @AllArgsConstructor
    public static class ToolCallPayload {

        private String id;

        private String name;

        private String arguments;

    }

    /**
     * 工具调用结果事件载荷
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResultPayload {

        private String id;

        private String name;

        private String arguments;

        private String result;

    }

    /**
     * 任务列表信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolTaskListPayload {

        private String id;

        private String name;

        /**
         * 架构：
         */
        private String architecture;

        private List<ToolTaskItemPayload> items;

    }

    /**
     * 任务项信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolTaskItemPayload {

        private String id;

        private String name;

        private String agent;

        /**
         * 状态：1.代办 2.执行 3.完成 4.停止
         */
        private String status;

    }

}
