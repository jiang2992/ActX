package com.bin.actx.service.request;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户聊天请求
 *
 * @author Bin
 * @since 2026/5/12 15:11
 */
@Data
@NoArgsConstructor
public class UserChatRequest {

    private String userId;

    private String sessionId;

    private String message;

    private List<String> attachments;

    public UserChatRequest(String message) {
        this.message = message;
    }

}
