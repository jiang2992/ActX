package com.bin.actx.message.attachment;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.jiang.tools.json.JsonUtils;
import org.springframework.ai.content.Media;

@Data
public class MediaAttachmentPreview implements AttachmentPreview {

    private String name;

    private String path;

    private Media media;

    @Override
    public String toJsonText() {
        Map<String, String> map = new HashMap<>();
        map.put("name", this.name);
        map.put("path", this.path);
        map.put("tips", "在media属性中");
        return JsonUtils.toJson(map);
    }

}
