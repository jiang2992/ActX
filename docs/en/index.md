---
layout: home

hero:
  name: "ActX"
  text: "Java General-Purpose Business Agent Application"
  tagline: "Lightweight · Highly Extensible · Non-Invasive <br/>— Help any Java enterprise project quickly integrate its own dedicated AI agent"
  actions:
    - theme: brand
      text: Documentation
      link: /en/info
    - theme: alt
      text: GitHub
      link: https://github.com/jiang2992/ActX

features:
  - icon: 🤖
    title: Multi-Agent Collaboration
    details: 9 built-in core agents, each with its own responsibility, supporting custom management. Follows the flexible hierarchy of "Scenario → Agent → Model".
  - icon: 🧠
    title: Business Cognition
    details: Autonomous business discovery through data sources, business documents, project materials, and more — proactively understanding and accumulating business knowledge into knowledge graphs.
  - icon: 🔍
    title: Knowledge Query
    details: Data querying based on knowledge graphs with three-layer statement security detection at the data gateway, supporting tenant control and permission control.
  - icon: 📋
    title: Complex Task Orchestration
    details: Complete task execution system — Evaluation → Planning → Dispatch → Execution → Review → Summary, with different subtasks handled by the best-matching agent.
  - icon: 🔌
    title: Progressive Tools & MCP
    details: Tools use two-phase loading, loading tool packages on demand; supports multi-mode MCP tool integration, connecting to external MCP tool servers.
  - icon: 🏗️
    title: Highly Extensible Architecture
    details: Built on Spring Boot 3.2 + Spring AI 1.1, supporting extension plugins, message channels, and sandbox isolation — seamlessly integrating with Feishu, DingTalk, WeChat, and more.
---

## Why ActX

ActX is a **Java general-purpose business agent application** built on **Spring Boot + Spring AI**. It adopts the design philosophy of "**Lightweight, Highly Extensible, and Non-Invasive**", helping any Java enterprise project quickly integrate its own dedicated AI agent.

Additionally, ActX supports proactive **enterprise business cognition** — autonomously understanding and accumulating business knowledge through business databases, documents, project materials, and other information, structuring it into knowledge graphs to empower agents with business query capabilities.

**Tech Stack:** Spring Boot 3.2 / Spring AI 1.1 / JDK 17+ / SQLite / Neo4j / OpenSandbox

---

## Core Capabilities

| Capability | Description |
|---|---|
| **Multi-Model** | Built on the Spring AI framework, supports all mainstream large language models |
| **Multi-Agent** | Multi-agent collaboration with specialized roles; 9 built-in core agents with custom management support |
| **Progressive Tools** | Tools use two-phase loading; tool packages are loaded on demand during task execution |
| **Conversation Memory** | Context memory based on conversations; automatic summary for long conversations |
| **MCP Management** | Supports multi-mode MCP tool integration; connects to external MCP tool servers |
| **Complex Tasks** | Complete task execution system supporting evaluation, planning, dispatch, execution, review, and summary; comprehensive task evaluation, node review, and retry mechanisms |
| **File Management** | Full file operation support for agents with comprehensive file permission control |
| **Sandbox Isolation** | Secure isolated code execution environment based on OpenSandbox; supports quick sandboxes and short-term sandboxes |
| **Business Cognition** | Supports business knowledge accumulation through data sources and business materials to build knowledge graphs |
| **Knowledge Query** | Data queries based on knowledge graphs with statement security review and permission control |
| **Extension Plugins** | Extends agent capabilities through standardized interfaces; different agents can be configured with different plugins |
| **Message Channel** | Message channel extension support, driven by message events; supports quick integration with Feishu, DingTalk, WeChat, or enterprise internal systems |

---

## Feature Showcase

### Multi-Agent Collaboration

Behind ActX, multiple agents with different responsibilities work collaboratively. Agents support flexible management, following the hierarchy: **Scenario → Agent → Model**.

### Business Cognition

ActX autonomously performs business discovery and builds a business cognition layer, proactively confirming speculation results with users to assist in accumulating business knowledge.

### Complex Tasks

When ActX identifies a task as complex, it delegates it to a dedicated task dispatch agent. Tasks are automatically decomposed into multiple subtasks, each handled by a specialized agent.

**Task Flow:** Evaluation → Planning → Dispatch → Execution → Review → Summary

---

## Project Structure

```
ActX
├── config/                  # Runtime configuration directory
│   ├── agent/               # Agent definitions (each subdirectory is one Agent)
│   │   ├── chat/            # Default chat Agent
│   │   ├── task_evaluate/   # Task evaluation Agent
│   │   ├── task_planning/   # Task planning Agent
│   │   ├── task_dispatcher/ # Task dispatch Agent
│   │   ├── coding_task/     # Coding task Agent
│   │   ├── general_task/    # General task Agent
│   │   ├── business_discover/ # Business discovery Agent
│   │   └── summary/         # Summary Agent
│   ├── actx.json            # Scenario routing configuration
│   ├── model.json           # Model configuration
│   ├── mcp.json             # MCP tool configuration
│   └── datasource.json      # Data source configuration
├── src/main/java/com/bin/actx/
│   ├── agent/               # Agent core framework
│   ├── channel/             # Message channels
│   ├── config/              # Configuration loading and management
│   ├── controller/          # Web interfaces
│   ├── plugin/              # Plugin system
│   ├── task/                # Task orchestration engine
│   ├── tool/                # Tool packages
│   ├── mcp/                 # MCP client
│   ├── sandbox/             # Sandbox management
│   ├── storage/             # Storage layer (SQLite / Neo4j)
│   ├── file/                # File management
│   ├── embedding/           # Vector embedding
│   └── sql/                 # SQL parsing and execution
├── file/                    # File storage directory
│   ├── attachment/          # Attachment storage
│   └── workspace/           # Workspace (sandbox outputs, etc.)
└── db/                      # SQLite database
```

---

## License

ActX is open-sourced under the [Apache License 2.0](https://github.com/jiang2992/ActX/blob/main/LICENSE).

Stars, Forks, Issues, and PRs are welcome!
