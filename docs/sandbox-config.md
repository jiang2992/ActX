# 配置沙箱

本文主要说明 OpenSandbox 的安装与 ActX 侧的沙箱配置。关于沙箱的运行机制（沙箱类型、目录映射、执行原则等），请参考[沙箱机制](/sandbox-manage)。

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

## 4. 验证沙箱是否正常工作

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

## 5. 关闭沙箱

如果不需要沙箱功能，编辑 `src/main/resources/application.yml`，将沙箱开关设为 `false`：

```yaml
actx:
  sandbox:
    enable: false
```

关闭后 ActX 启动时不会初始化 OpenSandbox 连接，无需安装 Docker 和 OpenSandbox。同时需要从 Agent 配置的 `plugins` 列表中移除 `SandboxPlugin`，详见[快速运行](/quick-start#_4-精简-agent-插件)。
