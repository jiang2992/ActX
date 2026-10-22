# 配置沙箱

ActX 的**沙箱隔离**与**代码执行**能力依赖于 [OpenSandbox](https://github.com/alibaba/OpenSandbox) 平台。OpenSandbox 是阿里巴巴开源的通用 AI 应用沙箱平台，ActX 通过 Java SDK 与其对接，为 Agent 提供安全的命令执行、代码运行和文件操作环境。

> 如果你仅使用 ActX 的基础对话能力，不需要沙箱。沙箱仅在 Agent 需要执行代码、运行命令等场景时启用。

---

## 1. 环境要求

| 依赖 | 版本要求 | 说明 |
|---|---|---|
| Docker | 20.10+ | OpenSandbox 使用 Docker 作为容器运行时 |

---

## 2. 安装 OpenSandbox

OpenSandbox Server 以 Python 包的形式分发，推荐使用 `uv` 或 `pip` 安装。

### 2.1 安装 Server

```bash
pip install opensandbox-server
```

### 2.2 初始化配置

以 Docker 运行时为例，生成默认配置文件：

```bash
opensandbox-server init-config ~/.sandbox.toml --example docker
```

### 2.3 启动服务

```bash
opensandbox-server
# 或指定配置文件路径
opensandbox-server --config /path/to/sandbox.toml
```

启动后可通过健康检查确认服务是否正常：

```bash
curl http://127.0.0.1:18080/health
# → {"status": "healthy"}
```

> 更多安装方式和详细配置请参考 [OpenSandbox 官方文档](https://open-sandbox.ai)。

---

## 3. 配置 ActX 连接 OpenSandbox

项目已提供示例配置文件 `env/opensandbox/sandbox.toml`，你可以根据实际情况修改。

### 3.1 OpenSandbox 侧配置

以下是 `env/opensandbox/sandbox.toml` 的核心配置项：

```toml
[server]
host = "127.0.0.1"
port = 18080
max_sandbox_timeout_seconds = 86400

# API Key 认证
api_key = "actx_sandbox_key"

[log]
level = "INFO"

[runtime]
type = "docker"
execd_image = "opensandbox/execd:v1.0.20"

[storage]
# 允许挂载的宿主机路径前缀（建议限制，不要留空）
allowed_host_paths = ["/path/to/actx/file"]

[store]
type = "sqlite"
path = "~/.opensandbox/opensandbox.db"

[docker]
network_mode = "bridge"
drop_capabilities = ["AUDIT_WRITE", "MKNOD", "NET_ADMIN", "NET_RAW", "SYS_ADMIN", "SYS_MODULE", "SYS_PTRACE", "SYS_TIME", "SYS_TTY_CONFIG"]
no_new_privileges = true
pids_limit = 4096

[ingress]
mode = "direct"

[egress]
image = "opensandbox/egress:v1.1.3"
mode = "dns"
```

> - `allowed_host_paths` 建议限制为 ActX 的 `file/` 目录，避免沙箱访问宿主机其他路径。
> - `api_key` 需要与 ActX 侧配置的 `actx.sandbox.api-key` 保持一致。

### 3.2 ActX 侧配置

编辑 `src/main/resources/application.yml`，配置 OpenSandbox 的连接信息：

```yaml
actx:
  sandbox:
    enable: true
    domain: 'localhost:18080'
    api-key: "actx_sandbox_key"
    fast-sandbox-pool:
      image: 'opensandbox/code-interpreter:v1.1.0'
      max-idle: 3
      idle-timeout: 86400
      sandbox-timeout: 300
```

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `enable` | 是否启用沙箱，设为 `false` 可完全关闭 | `true` |
| `domain` | OpenSandbox Server 的地址（IP:端口） | `localhost:18080` |
| `api-key` | API 密钥，需与 OpenSandbox 侧 `api_key` 一致 | - |
| `fast-sandbox-pool.image` | 临时沙箱池使用的镜像 | `opensandbox/code-interpreter:v1.1.0` |
| `fast-sandbox-pool.max-idle` | 沙箱池中最大空闲沙箱数量 | `3` |
| `fast-sandbox-pool.idle-timeout` | 空闲沙箱超时时间（秒），超时后自动回收 | `86400`（24小时） |
| `fast-sandbox-pool.sandbox-timeout` | 临时沙箱单次使用超时时间（秒） | `300`（5分钟） |

> - `domain` 中的端口号必须与 `sandbox.toml` 中 `[server].port` 一致。
> - `api-key` 必须与 `sandbox.toml` 中 `[server].api_key` 一致。
> - 如果 OpenSandbox 部署在远程服务器上，将 `localhost` 替换为对应的主机地址即可。

---

## 4. ActX 沙箱机制

ActX 基于 OpenSandbox 实现了两种沙箱类型，分别适用于不同的使用场景。Agent 在执行任务时会根据任务需求自动选择合适的沙箱类型。

### 4.1 临时沙箱（连接池模式）

临时沙箱由**沙箱连接池**统一管理，核心特点是**即用即走、快速回收**。

**工作原理：**

- ActX 启动时自动创建沙箱连接池（`fast-sandbox-pool`），预先维护一批空闲沙箱。
- 当 Agent 需要执行命令时，从池中快速获取一个沙箱，执行完毕后立即销毁，不保留任何状态。
- 每次执行命令都是全新的干净环境，确保隔离性和一致性。
- 池中的空闲沙箱超过 `idle-timeout` 后会被自动回收，避免资源浪费。

**适用场景：**

- 使用预装环境即可完成的任务（编译测试、简单计算、运行脚本等）。
- 不需要安装额外依赖（`pip install`、`npm install` 等安装结果不会保留，完全无效）。
- 不需要持久化状态的一次性任务。

**预装环境：**

| 语言 | 版本 | 路径 |
|---|---|---|
| Python | 3.10、3.11、3.12、3.13、3.14 | `/opt/python/versions` |
| Java | 8、11、17、21（含 Maven 3.9.2） | `/usr/lib/jvm` |
| Node.js | v18、v20、v22 | `/opt/node` |
| Go | 1.23、1.24、1.25 | `/opt/go` |

**工具调用：** Agent 通过 `execute_on_temp_sandbox` 工具在临时沙箱中执行命令，命令执行超时时间固定为 120 秒。

### 4.2 短期沙箱

短期沙箱是独立创建的沙箱实例，具有**固定有效期**和**持久化状态**，适用于需要安装依赖或长时间运行的任务。

**工作原理：**

- Agent 通过 `create_short_sandbox` 工具创建一个有效期为 60 分钟的沙箱。
- 创建时可指定需要开放的端口，系统会返回端口映射后的访问地址。
- 每次执行命令后自动续期 60 分钟，保持沙箱存活。
- 任务完成后需通过 `kill_short_sandbox` 工具手动销毁，释放资源。

**适用场景：**

- 需要安装额外依赖（`pip install`、`npm install`、`apt-get` 等）的任务。
- 需要多次操作、保持状态的任务。
- 需要开放端口供用户在线访问页面的场景。
- 长时间运行的任务。

**工具调用流程：**

1. `create_short_sandbox` — 创建沙箱，获取 `sandboxId` 和端口映射信息。
2. `execute_on_short_sandbox` — 通过 `sandboxId` 在指定沙箱中执行命令。
3. `kill_short_sandbox` — 任务完成后销毁沙箱。

### 4.3 目录映射

沙箱与宿主机之间的目录映射关系如下：

| 沙箱内路径 | 宿主机路径 | 权限 |
|---|---|---|
| `/data` | ActX 项目根目录（`file/` 对应的父目录） | 只读 |
| `/data/workspace` | `file/workspace/` | 读写 |
| 其他沙箱目录 | 无映射（随沙箱销毁） | 完全操作 |

> 非临时性产出文件应放入 `/data/workspace` 并按项目或功能建立子目录（如 `/data/workspace/my-project/`），这样文件会持久化到宿主机，不会随沙箱销毁而丢失。

### 4.4 执行原则

Agent 在使用沙箱时遵循以下原则：

- **文件写入优先**：常规文件写入优先使用文件工具完成，无法完成时再考虑使用沙箱。
- **合并命令**：尽量将多个命令用 `&&` 或 `;` 合并为一条，或写成脚本再执行，减少交互次数。
- **优先临时沙箱**：除非任务需要安装依赖或持久化状态，否则优先使用临时沙箱。

---

## 5. 验证沙箱是否正常工作

ActX 启动时会自动检测 OpenSandbox 连接状态。如果连接失败，日志中会输出以下错误信息：

```
The connection to OpenSandbox failed. Please ensure that the OpenSandbox service and Docker service are running.
```

此时请检查：

1. Docker 服务是否正在运行。
2. OpenSandbox Server 是否正在运行（`opensandbox-server`）。
3. `application.yml` 中的 `domain` 和 `api-key` 是否与 `sandbox.toml` 中的配置一致。

连接成功后，日志中会输出：

```
Sandbox is enabled [domain: localhost:18080].
Fast sandbox pool created.
```

---

## 6. 关闭沙箱

如果不需要沙箱功能，编辑 `src/main/resources/application.yml`，将沙箱开关设为 `false`：

```yaml
actx:
  sandbox:
    enable: false
```

关闭后 ActX 启动时不会初始化 OpenSandbox 连接，无需安装 Docker 和 OpenSandbox。同时需要从 Agent 配置的 `plugins` 列表中移除 `SandboxPlugin`，详见[快速运行](/quick-start#_4-精简-agent-插件)。
