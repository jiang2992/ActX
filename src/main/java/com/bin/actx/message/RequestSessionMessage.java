package com.bin.actx.message;

import com.bin.actx.file.AttachmentInfo;
import com.bin.actx.message.attachment.AttachmentPreview;
import com.bin.actx.message.attachment.MediaAttachmentPreview;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jiang.tools.json.JsonUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;

/**
 * 请求消息
 *
 * @author Bin
 * @since 2026/5/29 13:40
 */
@Data
@AllArgsConstructor
public class RequestSessionMessage implements SessionMessage {

    private final String type = "user";

    private String content;

    private List<AttachmentInfo> attachments;

    public RequestSessionMessage(String content) {
        this.content = content;
    }

    @Override
    public void appendToMessages(List<Message> messages) {
        this.appendToMessages(messages, null);
    }

    /**
     * 将消息追加到消息列表中
     *
     * @param messages              消息列表
     * @param attachmentPreviewList 优先于attachments，二则不能同时存在
     */
    public void appendToMessages(List<Message> messages, List<AttachmentPreview> attachmentPreviewList) {
        UserMessage.Builder userMessage = new UserMessage.Builder();
        StringBuilder contentSb = new StringBuilder();
        if (this.attachments != null && !this.attachments.isEmpty()) {
        }

        if (attachmentPreviewList != null && !attachmentPreviewList.isEmpty()) {
            Set<String> collect = attachmentPreviewList.stream().map(AttachmentPreview::toJsonText).collect(Collectors.toSet());
            contentSb.append("[Uploaded attachments: %s]\n".formatted(
                    String.format("[%s]", String.join(",", collect))
            ));
            List<Media> mediaList = attachmentPreviewList.stream()
                    .filter(item -> item instanceof MediaAttachmentPreview)
                    .map(item -> ((MediaAttachmentPreview) item).getMedia())
                    .toList();
            userMessage.media(mediaList);
        } else if (this.attachments != null && !this.attachments.isEmpty()) {
            String attachmentsJson = JsonUtils.toJson(this.attachments.stream().map(item -> {
                Map<String, String> attachmentInfo = new LinkedHashMap<>();
                attachmentInfo.put("name", item.getFileName());
                attachmentInfo.put("path", item.getPath());
                return attachmentInfo;
            }).toList());
            contentSb.append("[Uploaded attachments (Use the 'file' toolkit to get the content): %s]\n".formatted(attachmentsJson));
        }

        contentSb.append(this.content);
        userMessage.text(contentSb.toString());

        messages.add(userMessage.build());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Builder {

        private String content;
        private List<AttachmentInfo> attachments;

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder attachments(List<AttachmentInfo> attachments) {
            this.attachments = attachments;
            return this;
        }

        public RequestSessionMessage build() {
            return new RequestSessionMessage(this.content, this.attachments);
        }
    }

}
