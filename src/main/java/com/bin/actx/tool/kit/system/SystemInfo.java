package com.bin.actx.tool.kit.system;

import lombok.Data;

/**
 * 系统环境及资源使用信息
 *
 * @author Bin
 * @since 2026/8/21
 */
@Data
public class SystemInfo {

    /**
     * 操作系统名称
     */
    private String osName;

    /**
     * 操作系统版本
     */
    private String osVersion;

    /**
     * 系统架构
     */
    private String osArch;

    /**
     * Java版本
     */
    private String javaVersion;

    /**
     * 可用处理器核心数
     */
    private int cpuCores;

    /**
     * JVM总内存（MB）
     */
    private long jvmTotalMemoryMB;

    /**
     * JVM空闲内存（MB）
     */
    private long jvmFreeMemoryMB;

    /**
     * JVM最大可用内存（MB）
     */
    private long jvmMaxMemoryMB;

    /**
     * 系统总物理内存（MB）
     */
    private long totalPhysicalMemoryMB;

    /**
     * 系统可用物理内存（MB）
     */
    private long freePhysicalMemoryMB;

    /**
     * 根磁盘总空间（MB）
     */
    private long totalDiskSpaceMB;

    /**
     * 根磁盘可用空间（MB）
     */
    private long freeDiskSpaceMB;

    /**
     * JVM运行时长（秒）
     */
    private long jvmUptimeSeconds;

}
