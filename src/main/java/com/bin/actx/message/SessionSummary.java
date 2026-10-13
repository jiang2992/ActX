package com.bin.actx.message;

import java.io.Serializable;
import lombok.Data;

/**
 * 总结记录
 *
 * @author Bin
 * @since 2026/5/13 16:44
 */
@Data
public class SessionSummary implements Serializable {

    private final String sessionId;

    private Record lastSummary;

    public SessionSummary(String sessionId) {
        this.sessionId = sessionId;
    }

    @Data
    public static class Record implements Serializable {

        /**
         * 开始下标(包含)
         */
        private final Integer startIndex;

        /**
         * 结束下标(不包含)
         */
        private final Integer endIndex;

        private final String content;

        public Record(Integer startIndex, Integer endIndex, String content) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.content = content;
        }

    }

}
