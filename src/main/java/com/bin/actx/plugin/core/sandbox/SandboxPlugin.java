package com.bin.actx.plugin.core.sandbox;

import com.bin.actx.context.RequestContext;
import com.bin.actx.plugin.BasePlugin;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.service.request.RequestMessagePayload;
import com.bin.actx.tool.kit.sandbox.SandboxToolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * 沙箱插件
 *
 * @author Bin
 * @since 2026/7/27 14:52
 */
public class SandboxPlugin extends BasePlugin implements RequestHook {

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        requestMessagePayload.addToolkit(SandboxToolkit.class);
        requestMessagePayload.addSystemPrompt(getSystemPrompt(requestContext));
    }

    /**
     * 获取系统提示
     *
     * @return 系统提示
     */
    private Collection<String> getSystemPrompt(RequestContext requestContext) {
        return new ArrayList<>(Arrays.asList(
                "---",
                "",
                "# 沙箱使用指南",
                "",
                "## 预装环境",
                "",
                "- `Python`: 版本3.10、3.11、3.12、3.13、3.14，路径 /opt/python/versions",
                "- `Java`: 版本8、11、17、21，路径 /usr/lib/jvm（包含 Maven 3.9.2）",
                "- `Node.js`: 版本v18、v20、v22，路径 /opt/node",
                "- `Go`: 版本1.23、1.24、1.25，路径 /opt/go",
                "",
                "## 目录映射",
                "",
                "| 沙箱内路径        | 宿主机路径           | 权限     |",
                "| :---------------- | :------------------- | :------- |",
                "| `/data`           | `/`（根目录）        | 只读     |",
                "| `/data/workspace` | `/workspace`         | 读写     |",
                "| 其他沙箱目录      | 无映射（随沙箱销毁） | 完全操作 |",
                "",
                "## 执行原则",
                "",
                "- 文件写入：常规文件写入必须优先使用文件工具来完成，如果无法完成再考虑使用沙箱。",
                "- 合并命令：尽量将多个命令用 && 或 ; 合并为一条，或写成脚本再执行，以减少交互次数，提升效率。",
                "- 输出文件：非临时性产出文件请放入 /data/workspace 并按项目或功能建立子目录（如 /data/workspace/my-project/）。",
                "",
                "## 沙箱类型",
                "",
                "1. 临时沙箱",
                "  - 由沙箱池管理，创建/销毁快，即用即毁，每次执行命令都是全新的环境。",
                "  - 禁止执行 pip install、npm install、apt-get 等任何安装命令，安装结果不会保留，完全无效。",
                "  - 仅适用于使用预装环境即可完成的任务，如编译测试、简单计算、运行已有脚本。",
                "  - 优先选择，除非任务需要安装额外依赖或需要持久化状态。",
                "",
                "2. 短期沙箱",
                "  - 有效期为 60 分钟，每次执行命令后自动刷新。",
                "  - 适用于需要安装依赖（pip/npm/apt-get 等）或需要多次操作、长时间运行的任务。",
                "  - 允许在有效期内开放指定端口，允许用户在线访问页面等。",
                "  - 任务完成后请手动关闭，以释放资源。",
                ""
        ));
    }

}
