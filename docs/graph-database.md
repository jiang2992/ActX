# 图数据库

ActX 的**业务认知**与**知识查询**能力依赖于图数据库来存储和管理业务知识图谱。ActX 使用 **Neo4j 社区版**作为图数据库引擎，通过 Docker 容器快速部署。

> 如果你仅使用 ActX 的基础对话能力，不需要图数据库。图数据库仅在启用「业务认知」和「知识查询」功能时需要。

---

## 1. 环境要求

| 依赖 | 版本要求 | 说明 |
|---|---|---|
| Docker | 20.10+ | 用于运行 Neo4j 容器 |
| Docker Compose | v2+ | 用于编排 Neo4j 服务 |

---

## 2. 启动 Neo4j

项目已提供 Docker Compose 配置文件，位于 `env/neo4j/docker-compose.yml`：

```yaml
services:
  neo4j:
    image: neo4j:latest
    container_name: neo4j
    ports:
      - "7474:7474"   # HTTP 端口（Web 管理界面）
      - "7687:7687"   # Bolt 端口（数据库连接）
    volumes:
      - ./data:/data
      - ./logs:/logs
      - ./conf:/conf
      - ./plugins:/plugins
    environment:
      - NEO4J_AUTH=neo4j/actx_neo4j_password
      - NEO4J_PLUGINS='["genai"]'
```

在项目根目录下执行以下命令即可启动：

```bash
docker compose -f env/neo4j/docker-compose.yml up -d
```

启动后可以通过以下命令查看运行状态：

```bash
docker compose -f env/neo4j/docker-compose.yml ps
```

> 首次启动时 Neo4j 会拉取镜像并初始化数据目录，可能需要几分钟时间。

---

## 3. 访问 Neo4j 管理界面

Neo4j 启动成功后，打开浏览器访问：

```
http://localhost:7474
```

即可进入 Neo4j Browser 在线管理界面。使用以下凭据登录：

| 项目 | 值 |
|---|---|
| 用户名 | `neo4j` |
| 密码 | `actx_neo4j_password` |

登录后可以看到数据库的可视化图谱视图，左侧展示节点类型、关系类型和属性键，右侧为 Cypher 查询编辑器。你可以在查询框中执行 Cypher 语句来浏览和操作图谱数据。

![Neo4j 管理界面](/assets/功能展示-neo4j.png)

> 上图展示了 ActX 业务认知构建的知识图谱示例，包含业务概念、业务域、业务规则等多种节点类型及其关系。

---

## 4. 配置 ActX 连接 Neo4j

编辑 `src/main/resources/application.yml`，在 `actx.storage.graph` 下配置 Neo4j 的连接信息：

```yaml
actx:
  storage:
    graph:
      url: bolt://localhost:7687
      user: neo4j
      password: actx_neo4j_password
```

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `url` | Neo4j Bolt 协议连接地址 | `bolt://localhost:7687` |
| `user` | Neo4j 用户名 | `neo4j` |
| `password` | Neo4j 密码 | `actx_neo4j_password` |

> - `url` 中的端口号必须与 `docker-compose.yml` 中映射的 Bolt 端口（默认 `7687`）一致。
> - `user` 和 `password` 必须与 `docker-compose.yml` 中 `NEO4J_AUTH` 环境变量设置的凭据一致。
> - 如果 Neo4j 部署在远程服务器上，将 `localhost` 替换为对应的主机地址即可。

---

## 5. 停止与清理

停止 Neo4j 容器：

```bash
docker compose -f env/neo4j/docker-compose.yml stop
```

如果需要彻底清理数据（**注意：会删除所有图谱数据**）：

```bash
docker compose -f env/neo4j/docker-compose.yml down -v
```
