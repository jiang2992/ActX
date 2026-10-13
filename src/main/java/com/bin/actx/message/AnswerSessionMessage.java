package com.bin.actx.message;

import com.bin.actx.message.section.AnswerSection;
import com.bin.actx.message.section.MessageAnswerSection;
import com.bin.actx.message.section.ToolAnswerSection;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;

/**
 * 应答消息
 *
 * @author Bin
 * @since 2026/5/29 13:40
 */
@Data
public class AnswerSessionMessage implements SessionMessage {

    private final String type = "answer";

    private String content;

    private List<AnswerSection> sections;

    private Integer tokenTotal;

    @Override
    public void appendToMessages(List<Message> messages) {
        if (sections == null || sections.isEmpty()) {
            return;
        }

        StringBuilder content = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();
        List<ToolResponse> toolResponses = new ArrayList<>();

        for (int i = 0; i < sections.size(); i++) {
            boolean isEnd = i == sections.size() - 1;
            AnswerSection section = sections.get(i);

            if (section instanceof MessageAnswerSection) {
                content.append(section.toText());
                if (!isEnd) {
                    continue;
                }
            } else if (section instanceof ToolAnswerSection toolAnswerSection) {
                toolCalls.add(new ToolCall(
                        toolAnswerSection.getId(),
                        "function",
                        toolAnswerSection.getName(),
                        toolAnswerSection.getArguments()
                ));
                toolResponses.add(new ToolResponse(
                        toolAnswerSection.getId(),
                        toolAnswerSection.getName(),
                        toolAnswerSection.getResult()
                ));
                if (!isEnd) {
                    continue;
                }
            }

            if (!content.isEmpty() || !toolCalls.isEmpty()) {
                messages.add(AssistantMessage.builder()
                        .content(content.toString())
                        .toolCalls(toolCalls)
                        .build());
                content = new StringBuilder();
                toolCalls = new ArrayList<>();
                if (!toolResponses.isEmpty()) {
                    messages.add(ToolResponseMessage.builder()
                            .responses(toolResponses)
                            .build());
                    toolResponses = new ArrayList<>();
                }
            }
        }

    }

}
