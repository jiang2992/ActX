package com.bin.actx.message;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Data;

/**
 * 聊天会话，存储单个会话的完整消息历史
 *
 * @author Bin
 * @since 2026/5/13 13:58
 */
@Data
public class SessionMessageCollection implements Serializable {

    private final String sessionId;

    private final List<SessionMessage> messages;

    public SessionMessageCollection(String sessionId) {
        this.sessionId = sessionId;
        this.messages = new CopyOnWriteArrayList<>();
    }

    public void addMessage(SessionMessage message) {
        messages.add(message);
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public int getMessageCount() {
        return messages.size();
    }

}
