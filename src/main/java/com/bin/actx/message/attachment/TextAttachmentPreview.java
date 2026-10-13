package com.bin.actx.message.attachment;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.jiang.tools.json.JsonUtils;

@Data
public class TextAttachmentPreview implements AttachmentPreview {

    private String name;

    private String path;

    private String content;

    private Boolean isAll;

    @Override
    public String toJsonText() {
        Map<String, String> map = new HashMap<>();
        map.put("name", this.name);
        map.put("path", this.path);
        map.put("content", this.content);
        if (!isAll) {
            map.put("tips", "已展示前%s个字符，更多内容通过文件工具获取".formatted(this.content.length()));
        }
        return JsonUtils.toJson(map);
    }

}
