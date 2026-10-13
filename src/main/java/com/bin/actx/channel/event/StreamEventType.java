package com.bin.actx.channel.event;

/**
 * 流式聊天事件类型
 *
 * @author Bin
 * @since 2026/5/12 11:42
 */
public enum StreamEventType {

    /**
     * 会话创建，携带sessionId
     */
    SESSION,

    /**
     * 提醒
     */
    TELL,

    /**
     * 思考中
     */
    THINKING,

    /**
     * 工具调用
     */
    TOOL_CALL,

    /**
     * 工具调用结果
     */
    TOOL_RESULT,

    /**
     * 响应正文
     */
    MESSAGE,

    /**
     * 流结束
     */
    DONE,

    /**
     * 停止
     */
    STOP,

    /**
     * 错误信息
     */
    ERROR,

    /**
     * 任务列表
     */
    TASK_ORDER,

    /**
     * 任务更新
     */
    TASK_UPDATE,

    /**
     * 子任务更新
     */
    SUB_TASK_UPDATE

}
