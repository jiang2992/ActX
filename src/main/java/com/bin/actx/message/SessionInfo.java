package com.bin.actx.message;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话信息
 *
 * @author Bin
 * @since 2026/5/15 16:45
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo implements Serializable {

    private String userId;

    private String sessionId;

    private String name;

    private Integer lastTokenTotal = 0;

    private Integer lastMessageSort = 0;

    private LocalDateTime lastMessageTime;

    public SessionInfo(String userId, String sessionId, String name) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.name = name;
    }

}
