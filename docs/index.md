---
layout: home

hero:
  name: "ActX"
  text: "Java 通用业务智能体应用"
  tagline: "轻量级 · 高扩展 · 无侵入 <br/>— 帮助任何 Java 企业项目快速接入专属 AI 智能体"
  actions:
    - theme: brand
      text: 开发文档
      link: /info
    - theme: alt
      text: GitHub
      link: https://github.com/jiang2992/ActX

features:
  - icon: 🤖
    title: 多 Agent 协作
    details: 内置 9 个核心 Agent，各司其职，支持自定义管理。遵循「场景 → Agent → 模型」的灵活层级体系。
  - icon: 🧠
    title: 业务认知
    details: 自主进行业务发现，通过数据源、业务文档、项目资料等信息主动理解和沉淀业务知识，构建知识图谱。
  - icon: 🔍
    title: 知识查询
    details: 基于知识图谱进行数据查询，数据网关三层语句安全检测，支持租户控制与权限控制。
  - icon: 📋
    title: 复杂任务编排
    details: 完整的任务执行体系：评估 → 规划 → 调度 → 执行 → 评审 → 总结，不同子任务由最匹配的 Agent 负责。
  - icon: 🔌
    title: 渐进式工具 & MCP
    details: 工具采用二阶段加载，按需加载工具包；支持多模式 MCP 工具接入，连接外部 MCP 工具服务器。
  - icon: 🏗️
    title: 高扩展架构
    details: 基于 Spring Boot 3.2 + Spring AI 1.1，支持扩展插件、消息通道、沙箱隔离，可无缝集成飞书、钉钉、微信等系统。
---

## 为什么选择 ActX

ActX 是一个基于 **Spring Boot + Spring AI** 构建的 Java 通用业务智能体应用。它以「**轻量级、高扩展、无侵入**」为设计理念，帮助任何 Java 企业项目快速接入专属的 AI 智能体。

同时，ActX 支持企业**业务主动认知**——通过业务数据库、业务文档、项目资料等信息来自主理解和沉淀业务知识，结构化为知识图谱，为智能体赋予业务查询能力。

**技术栈：** Spring Boot 3.2 / Spring AI 1.1 / JDK 17+ / SQLite / Neo4j / OpenSandbox

---

## 核心能力一览

| 能力 | 说明 |
|---|---|
| **多模型** | 基于 Spring AI 框架，支持市面上所有主流大语言模型 |
| **多 Agent** | 多 Agent 协作，各司其职，内置 9 个核心 Agent，支持自定义管理 |
| **渐进式工具** | 工具采用二阶段加载，任务过程按需加载工具包 |
| **会话记忆** | 基于会话的上下文记忆，长会话自动总结摘要 |
| **MCP 管理** | 支持多模式 MCP 工具接入，连接外部 MCP 工具服务器 |
| **复杂任务** | 完整的任务执行体系，支持评估、规划、调度、执行、评审、总结，完善的任务评估、节点评审、重试机制 |
| **文件管理** | 为 Agent 提供完整的文件操作支持，完善的文件权限控制 |
| **沙箱隔离** | 基于 OpenSandbox 提供安全隔离的代码执行环境，支持快速沙箱和短期沙箱 |
| **业务认知** | 支持通过数据源、业务资料等信息进行业务知识沉淀，构建知识图谱 |
| **知识查询** | 基于知识图谱进行数据查询，支持语句安全审查、权限控制等 |
| **扩展插件** | 通过统一标准接口实现智能体的能力扩展，不同 Agent 可配置不同的插件 |
| **消息通道** | 消息通道扩展支持，由消息事件驱动，支持快速整合飞书、钉钉、微信或企业内部系统 |

---

## 功能展示

### 多 Agent 协作

ActX 背后是多个不同职责的 Agent 协同工作，Agent 支持自由管理，依次是 **场景 → Agent → 模型**。

### 业务认知

ActX 自主进行业务发现，构建业务认知层，主动向用户确认推测结果，协助完成业务知识的沉淀。

### 复杂任务

当 ActX 将任务标识为复杂任务时，会交给专门的任务调度 Agent 处理。任务自动拆分为多个子任务，不同子任务由专门的 Agent 负责。

**任务流程：** 评估 → 规划 → 调度 → 执行 → 评审 → 总结

---

## 项目结构

```
ActX
├── config/                  # 运行配置目录
│   ├── agent/               # Agent 定义（每个子目录为一个 Agent）
│   │   ├── chat/            # 默认对话 Agent
│   │   ├── task_evaluate/   # 任务评估 Agent
│   │   ├── task_planning/   # 任务规划 Agent
│   │   ├── task_dispatcher/ # 任务调度 Agent
│   │   ├── coding_task/     # 编码任务 Agent
│   │   ├── general_task/    # 通用任务 Agent
│   │   ├── business_discover/ # 业务发现 Agent
│   │   └── summary/         # 总结 Agent
│   ├── actx.json            # 场景路由配置
│   ├── model.json           # 模型配置
│   ├── mcp.json             # MCP 工具配置
│   └── datasource.json      # 数据源配置
├── src/main/java/com/bin/actx/
│   ├── agent/               # Agent 核心框架
│   ├── channel/             # 消息通道
│   ├── config/              # 配置加载与管理
│   ├── controller/          # Web 接口
│   ├── plugin/              # 插件体系
│   ├── task/                # 任务编排引擎
│   ├── tool/                # 工具包
│   ├── mcp/                 # MCP 客户端
│   ├── sandbox/             # 沙箱管理
│   ├── storage/             # 存储层（SQLite / Neo4j）
│   ├── file/                # 文件管理
│   ├── embedding/           # 向量嵌入
│   └── sql/                 # SQL 解析与执行
├── file/                    # 文件存储目录
│   ├── attachment/          # 附件存储
│   └── workspace/           # 工作空间（沙箱产出物等）
└── db/                      # SQLite 数据库
```

---

## 开源协议

ActX 采用 [Apache License 2.0](https://github.com/jiang2992/ActX/blob/main/LICENSE) 开源协议。

欢迎 Star、Fork、提交 Issue 和 PR！
