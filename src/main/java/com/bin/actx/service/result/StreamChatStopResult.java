package com.bin.actx.service.result;

import lombok.Data;

/**
 * 停止流式聊天结果
 *
 * @author Bin
 * @since 2026/8/4 16:46
 */
@Data
public class StreamChatStopResult {

    private String sessionId;

    private Boolean result;

    private String message;

}
