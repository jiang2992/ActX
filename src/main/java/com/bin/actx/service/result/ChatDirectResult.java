package com.bin.actx.service.result;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 聊天结果
 *
 * @author Bin
 * @since 2026/5/12 11:31
 */
@Data
@AllArgsConstructor
public class ChatDirectResult {

    private String sessionId;

    private String data;

}
