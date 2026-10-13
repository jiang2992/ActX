package com.bin.actx.analyze;

import com.bin.actx.network.html.HtmlPageData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component
public class HtmlPageExtractor {

    public HtmlPageData extractWithTika(String url, String html) throws IOException, TikaException, SAXException {
        HtmlPageData data = new HtmlPageData();
        data.setUrl(url);

        // 创建 Tika 解析器
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();

        // 设置内容长度限制（例如最多 5000000 字符，防止超大页面）
        BodyContentHandler handler = new BodyContentHandler(5000000);

        try (ByteArrayInputStream stream = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8))) {
            parser.parse(stream, handler, metadata, new org.apache.tika.parser.ParseContext());
        }

        // 获取正文纯文本
        String content = handler.toString();
        content = cleanText(content);
        data.setContent(content);

        // 提取常用元数据
        Map<String, String> metaMap = new HashMap<>();
        String author = metadata.get("Author");
        if (author != null) {
            metaMap.put("author", author);
        }
        String date = metadata.get("date");
        if (date != null) {
            metaMap.put("date", date);
        }
        String description = metadata.get("description");
        if (description != null) {
            metaMap.put("description", description);
        }
        data.setMetadata(metaMap);

        // 使用 Jsoup 解析，提取标题
        Document jsoupDoc = Jsoup.parse(content);
        String title = extractTitle(jsoupDoc);
        data.setTitle(title);

        return data;
    }

    private String extractTitle(Document doc) {
        // 优先使用 og:title
        Element ogTitle = doc.select("meta[property=og:title]").first();
        if (ogTitle != null) {
            return ogTitle.attr("content");
        }
        // 其次使用 title 标签
        if (!doc.title().isEmpty()) {
            return doc.title();
        }
        // 最后尝试 h1
        Element h1 = doc.select("h1").first();
        return h1 != null ? h1.text() : "无标题";
    }

    private String cleanText(String text) {
        if (text == null) {
            return null;
        }
        return text
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n[ \\t]+", "\n")
                .replaceAll(" \\n", "\n")
                .replaceAll("\\n ", "\n")
                .replaceAll("\\n{2,}", "\n\n")
                .trim();
    }

}
