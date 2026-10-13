package com.bin.actx.task.model;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;

/**
 * 发起人任务信息，通常在需要任务发起者补充内容时创建
 *
 * @author Bin
 * @since 2026/6/1 16:54
 */
@Data
public class SponsorTaskInfo implements Serializable {

    private String id;

    private String description;

    private String expectedData;

    private Map<String, String> dependentInfo;

}
