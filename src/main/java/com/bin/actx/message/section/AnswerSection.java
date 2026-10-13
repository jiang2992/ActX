package com.bin.actx.message.section;

import java.io.Serializable;

public interface AnswerSection extends Serializable {

    /**
     * 从文本获取数据
     *
     * @param content 文本内容
     */
    void fromText(String content);

    /**
     * 将数据转为文本
     *
     */
    String toText();

    /**
     * 获取类型
     *
     * @return 类型
     */
    String getType();

}
