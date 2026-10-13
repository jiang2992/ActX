package com.bin.actx.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件安全工具类
 *
 * @author Bin
 * @since 2026/7/10 16:16
 */
public class FileSecurityUtils {

    /**
     * 判断 child 是否在 parent 目录之内（包含自身）
     *
     * @param parent 根目录
     * @param child  待检查的路径
     * @return true 表示安全且在内部
     */
    public static boolean isSubPath(Path parent, Path child) {
        // 获取绝对路径并去除 . 和 ..
        Path parentPath = parent.toAbsolutePath().normalize();
        Path childPath = child.toAbsolutePath().normalize();

        // 如果文件真实存在，且可能是符号链接，必须解析真实物理路径，防止越权访问
        try {
            if (Files.exists(parentPath)) {
                parentPath = parentPath.toRealPath();
            }
            if (Files.exists(childPath)) {
                childPath = childPath.toRealPath();
            }
        } catch (IOException e) {
            // 路径无权限或损坏，视为不安全
            return false;
        }

        // 逐级比较
        return childPath.startsWith(parentPath);
    }

}
