# 日志配置

ActX 提供了**模型请求日志**功能，用于记录每次模型 API 调用的完整 HTTP 请求与响应细节，方便调试和排查问题。

---

## 配置

在 `src/main/resources/application.yml` 中配置：

```yaml
actx:
  log:
    path: ./log/
    full-request-logging: false
```

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `path` | 模型请求日志文件的存储目录 | `./log/` |
| `full-request-logging` | 是否开启完整请求日志，设为 `true` 开启 | `false` |

---

## 日志内容

开启后，每个模型 API 请求会生成一个独立的 `.log` 文件，保存在 `path` 指定的目录下，包含以下信息：

- 请求方法和 URL
- 请求 Headers（`Authorization`、`api-key` 等敏感 Header 会自动脱敏）
- 请求 Body
- 响应状态码和耗时
- SSE 流式数据块（流式调用时）

日志文件命名格式为 `yyyyMMddHHmmss_随机序号.log`。

---

## 使用建议

- **默认关闭**：开启后会记录完整的请求和响应内容，产生大量日志文件，建议仅在调试时开启。
- **及时清理**：开启期间产生的日志文件需要手动清理，避免磁盘空间占满。
