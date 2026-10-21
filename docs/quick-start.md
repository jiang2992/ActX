# 快速运行

本文介绍如何以最简配置快速启动 ActX，仅保留核心对话能力，**不包含沙箱隔离和业务认知功能**。适合先体验基础能力，后续再按需扩展。

## 环境要求

| 依赖 | 版本要求 | 说明 |
|---|---|---|
| JDK | 17+ | 支持 JDK 17 / 21 / 25 |
| Maven | 3.6+ | 项目构建工具 |

> Neo4j、Docker、OpenSandbox 等在本快速启动中**均不需要**。

---

## 1. 克隆项目

```bash
git clone https://github.com/jiang2992/ActX.git
cd ActX
```

---

## 2. 配置大模型

编辑 `config_local/model.json`，填入你的大模型 API Key：

```json
{
  "chatModels": [
    {
      "id": "main-pro",
      "supplierName": "OpenAi",
      "modelName": "gpt-4o",
      "baseUrl": "https://api.openai.com/v1",
      "apiKey": "your-api-key",
      "temperature": 0.5,
      "maxTokens": 1000000,
      "enableThink": true
    },
    {
      "id": "main-pro-json",
      "supplierName": "OpenAi",
      "modelName": "gpt-4o",
      "baseUrl": "https://api.openai.com/v1",
      "apiKey": "your-api-key",
      "temperature": 0.5,
      "maxTokens": 1000000,
      "enableThink": false,
      "responseFormat": "json"
    },
    {
      "id": "main-pro-unthink",
      "supplierName": "OpenAi",
      "modelName": "gpt-4o",
      "baseUrl": "https://api.openai.com/v1",
      "apiKey": "your-api-key",
      "temperature": 0.5,
      "maxTokens": 1000000,
      "enableThink": false
    }
  ],
  "embeddingModels": []
}
```

> - 目前支持 OpenAi、ZhipuAi、Deepseek 三种模型供应商，如需其他供应商需添加 Spring AI 对应实现并实现 `ModelFactory` 接口。
> - 如果使用 TokenPlan 等套餐，建议直接使用 OpenAi 接口。
> - 建议保留 `main-pro`、`main-pro-json`、`main-pro-unthink` 三种配置。如果条件有限，可只保留 `main-pro`，但需同步修改各 Agent 配置中的模型引用。
> - `embeddingModels` 留空即可，向量嵌入模型仅在启用业务认知时需要。

---

## 3. 关闭沙箱

编辑 `src/main/resources/application.yml`，将沙箱开关设为 `false`：

```yaml
actx:
  sandbox:
    enable: false    # 关闭沙箱
```

> 关闭后 ActX 启动时不会初始化 OpenSandbox 连接，无需安装 Docker 和 OpenSandbox。

---

## 4. 精简 Agent 插件

编辑 `config_local/agent/chat/agent.json`，移除沙箱和业务认知相关的插件：

```json
{
  "id": "chat-agent",
  "name": "Default Chat Agent",
  "description": "The system's global default chat agent",
  "model": "main-pro",
  "systemPromptFile": "./config/agent/chat/prompt.md",
  "privatize": true,
  "toolkits": "*",
  "toolMode": "dynamic",
  "plugins": [
    {
      "class": "com.bin.actx.plugin.core.memory.SessionMemoryPlugin"
    },
    {
      "class": "com.bin.actx.plugin.core.file.FilePlugin"
    },
    {
      "class": "com.bin.actx.plugin.core.summary.SummaryPlugin"
    },
    {
      "class": "com.bin.actx.plugin.core.task.TaskPlugin"
    },
    {
      "class": "com.bin.actx.plugin.core.mcp.McpPlugin"
    }
  ]
}
```

**移除的插件：**

| 插件 | 说明 |
|---|---|
| `SandboxPlugin` | 沙箱代码执行插件，关闭沙箱后需移除 |
| `BusinessReferPlugin` | 业务认知引用插件，未启用 Neo4j 时需移除 |

---

## 5. 启动应用

```bash
mvn spring-boot:run
```

启动成功后访问 `http://localhost:8080` 即可开始使用。

---

## 当前可用能力

完成以上配置后，ActX 具备以下能力：

| 能力 | 状态 |
|---|---|
| 多模型对话 | ✅ 可用 |
| 多 Agent 协作 | ✅ 可用 |
| 复杂任务编排 | ✅ 可用 |
| 会话记忆 | ✅ 可用 |
| 文件管理 | ✅ 可用 |
| MCP 工具 | ✅ 可用（需在 `config_local/mcp.json` 中配置） |
| 沙箱隔离 | ❌ 未启用 |
| 业务认知 | ❌ 未启用 |
| 知识查询 | ❌ 未启用 |

