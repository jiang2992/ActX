package com.bin.actx.message;

import java.io.Serializable;
import java.util.List;
import org.springframework.ai.chat.messages.Message;

/**
 * 会话消息
 *
 * @author Bin
 * @since 2026/5/29 13:40
 */
public interface SessionMessage extends Serializable {

    String getType();

    String getContent();

    void appendToMessages(List<Message> messages);

}
