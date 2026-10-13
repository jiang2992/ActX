package com.bin.actx.message;

import com.bin.actx.message.section.AnswerSection;
import com.bin.actx.message.section.ErrorAnswerSection;
import com.bin.actx.message.section.MessageAnswerSection;
import com.bin.actx.message.section.StopAnswerSection;
import com.bin.actx.message.section.TellAnswerSection;
import com.bin.actx.message.section.ThinkingAnswerSection;
import com.bin.actx.message.section.ToolAnswerSection;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jiang.tools.text.StringUtils;

/**
 * 回答会话消息构造器，它会在LLM响应开始之前创建，然后贯穿整个响应期间，通常用于承载LLM的所有响应片段
 *
 * @author Bin
 * @since 2026/7/30 15:53
 */
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSessionMessageBuilder {

    private LinkedList<AnswerSection> sections = new LinkedList<>();
    private Integer tokenTotal = 0;

    public AnswerSessionMessageBuilder tokenTotal(Integer tokenTotal) {
        this.tokenTotal = tokenTotal;
        return this;
    }

    public AnswerSessionMessageBuilder appendMessageSection(String content) {
        if (StringUtils.isEmpty(content)) {
            return this;
        }
        MessageAnswerSection messageAnswerSection = new MessageAnswerSection();
        messageAnswerSection.setContent(content);
        return this.appendSection(messageAnswerSection, last -> {
            last.append(content);
        });
    }

    public AnswerSessionMessageBuilder appendThinkingSection(String content) {
        if (StringUtils.isEmpty(content)) {
            return this;
        }
        ThinkingAnswerSection thinkingSection = new ThinkingAnswerSection();
        thinkingSection.setContent(content);
        return this.appendSection(thinkingSection, last -> {
            last.append(content);
        });
    }

    public AnswerSessionMessageBuilder appendToolSection(String id, String name, String arguments, String result) {
        ToolAnswerSection toolAnswerSection = new ToolAnswerSection();
        toolAnswerSection.setId(id);
        toolAnswerSection.setName(name);
        toolAnswerSection.setArguments(arguments);
        toolAnswerSection.setResult(result);
        return this.addSection(toolAnswerSection);
    }

    public AnswerSessionMessageBuilder appendErrorSection(String content) {
        ErrorAnswerSection errorAnswerSection = new ErrorAnswerSection();
        errorAnswerSection.setContent(content);
        return this.addSection(errorAnswerSection);
    }

    public AnswerSessionMessageBuilder appendStopSection(String content) {
        StopAnswerSection stopAnswerSection = new StopAnswerSection();
        stopAnswerSection.setContent(content);
        return this.addSection(stopAnswerSection);
    }

    public AnswerSessionMessageBuilder appendTellSection(String content) {
        TellAnswerSection tellAnswerSection = new TellAnswerSection();
        tellAnswerSection.setContent(content);
        return this.addSection(tellAnswerSection);
    }

    public synchronized AnswerSessionMessageBuilder addSection(AnswerSection section) {
        if (section == null) {
            return this;
        }
        sections.add(section);
        return this;
    }

    /**
     * 如果当前列表中最后一个判断是相同片段，则调用 consumer，否则直接添加
     *
     * @param section  section
     * @param consumer 相同片段时调用的参数
     * @param <T>      T
     * @return AnswerSessionMessageBuilder
     */
    public synchronized <T extends AnswerSection> AnswerSessionMessageBuilder appendSection(T section, Consumer<T> consumer) {
        if (section == null) {
            return this;
        }
        AnswerSection last = sections.isEmpty() ? null : sections.getLast();
        if (last != null && section.getClass().isAssignableFrom(last.getClass())) {
            consumer.accept((T) last);
            return this;
        }
        return this.addSection(section);
    }

    public synchronized AnswerSessionMessage build() {
        AnswerSessionMessage message = new AnswerSessionMessage();
        message.setSections(sections);
        message.setTokenTotal(tokenTotal);

        String content = sections.stream()
                .map(AnswerSection::toText)
                .collect(Collectors.joining("\n"));
        message.setContent(content);

        return message;
    }

}
