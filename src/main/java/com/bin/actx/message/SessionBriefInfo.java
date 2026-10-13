package com.bin.actx.message;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话简要信息
 *
 * @author Bin
 * @since 2026/5/15 16:45
 */
@Data
@NoArgsConstructor
public class SessionBriefInfo implements Serializable {

    private String userId;

    private String sessionId;

    private String name;

    public SessionBriefInfo(String userId, String sessionId, String name) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.name = name;
    }

}
