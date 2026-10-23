# 文件管理

ActX 提供了一套完整的文件管理机制，包括文件存储目录划分、权限控制、Agent 文件工具集以及 HTTP 接口，用于支撑对话过程中的文件上传、读取、写入和管理需求。

---

## 存储目录结构

文件存储根目录通过 `actx.storage.file.dir` 配置，默认为 `./file/`。根目录下包含两个核心子目录：

```
file/
├── attachment/    # 附件目录
└── workspace/     # 工作空间目录
```

### attachment（附件目录）

**作用：** 存储用户通过对话上传的附件文件。

**特点：**
- 文件上传后会生成 UUID 作为文件名，保留原始扩展名，避免命名冲突
- 附件信息会持久化到数据库（通过 `AttachmentDao`），支持按 ID 查询和管理
- 支持文本文件和媒体文件两种预览模式：文本文件会读取内容展示，媒体文件（图片等）会作为 Media 对象传递

**权限：**
- 仅支持**读取**操作，Agent 不能向 attachment 目录写入文件
- 附件的上传和删除由系统通过 HTTP 接口管理，不开放给 Agent 直接操作

### workspace（工作空间目录）

**作用：** Agent 进行文件读写操作的工作区域，所有文件生成、编辑、整理均在此目录下进行。

**特点：**
- Agent 创建的文件、代码执行结果、任务产出物等都存储在这里
- 支持完整的目录结构，可创建子文件夹组织文件
- 文件操作支持冲突处理策略（重命名、覆盖、停止）

**权限：**
- 支持**完整的读写操作**，包括创建、写入、修改、删除、移动、复制
- 所有写入类工具（`write_file`、`patch_file`、`append_file`、`delete_file`、`move_file`、`copy_file`、`replace_in_file`）都限制在 workspace 目录内
- 读取类工具可以访问整个文件存储目录（包括 attachment）
- 通过路径安全校验（`FileSecurityUtils.isSubPath`）防止路径穿越攻击

---

## 文件工具集

ActX 通过 `FileManageToolkit` 为 Agent 提供了一整套文件操作工具，可在 Agent 配置中按需启用。

### 读取类工具

| 工具名 | 说明 | 关键参数 |
|---|---|---|
| `read_file` | 读取文件文本内容（最多返回 100000 字符） | `path` 文件路径、`limit` 读取行数（最高 1000）、`startLine` 开始行 |
| `read_visual_file` | 读取视觉文件（图片），读取后附带在请求的 media 中 | `paths` 文件路径列表 |
| `find_directory` | 列出或按文件名正则查找目录下的项目（最多 1000 个） | `path` 文件夹路径、`regex` 正则表达式、`depth` 递归深度（最大 3） |
| `find_in_file` | 在文本文件中按正则表达式搜索匹配的行 | `path` 文件路径、`regex` 正则表达式、`maxResults` 最大结果数（最高 1000） |
| `get_file_info` | 获取文件元数据（大小、扩展名、修改时间等） | `path` 文件路径 |

### 写入类工具

| 工具名 | 说明 | 关键参数 |
|---|---|---|
| `write_file` | 写入文本到文件 | `path` 文件路径（必须基于 `/workspace`）、`strategy` 冲突策略（rename/cover/stop）、`content` 内容（严格 10000 字符以内） |
| `patch_file` | 修补文件中的文本内容（按行号定位） | `path` 文件路径、`startLine` 开始行、`coverLines` 覆盖行数、`content` 内容 |
| `append_file` | 追加文本内容到文件尾部 | `path` 文件路径、`content` 内容 |
| `replace_in_file` | 替换文件中的字符串（精确匹配，非正则） | `path` 文件路径、`target` 目标字符串、`replacement` 替换字符串 |
| `delete_file` | 删除文件或文件夹 | `path` 文件路径、`recursive` 是否递归删除 |
| `move_file` | 移动文件或文件夹 | `sourcePath` 源路径、`targetPath` 目标路径（必须基于 `/workspace`） |
| `copy_file` | 复制文件或文件夹 | `sourcePath` 源路径、`targetPath` 目标路径（必须基于 `/workspace`） |

### 冲突处理策略

写入文件时支持三种冲突处理策略（当目标文件已存在且不为空时）：

| 策略 | 说明 |
|---|---|
| `stop` | 默认策略，停止写入并抛出异常 |
| `cover` | 强行覆盖原文件内容 |
| `rename` | 自动重命名文件（添加后缀避免同名） |

---

## HTTP 接口

ActX 通过 `FileController` 提供文件相关的 HTTP 接口，用于前端或外部系统的文件上传和下载。

### 上传文件

```
POST /file
Content-Type: multipart/form-data
```

**请求参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| `file` | MultipartFile | 上传的文件 |

**响应示例：**

```json
{
  "code": 200,
  "data": {
    "id": "276170a2fbe648a6ae08d70fc521ecfa",
    "fileName": "example.png",
    "path": "attachment/276170a2fbe648a6ae08d70fc521ecfa.png",
    "size": 102400,
    "extension": "png",
    "isTextFile": false
  }
}
```

### 下载文件

```
GET /file/{filePath}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| `filePath` | String | 文件的相对路径（如 `attachment/xxx.png` 或 `workspace/xxx.txt`） |

**响应：** 文件流，`Content-Disposition` 为 `attachment`，文件名使用 UTF-8 编码。

**示例：**

```
GET /file/attachment/276170a2fbe648a6ae08d70fc521ecfa.png
GET /file/workspace/mac_frame.py
```

---

## 安全机制

文件管理系统内置了多层安全防护：

1. **路径穿越防护**：所有文件操作都会通过 `FileSecurityUtils.isSubPath` 校验，确保路径在允许的范围内，防止 `../` 等攻击手段
2. **符号链接检测**：对真实存在的文件会解析物理路径，防止通过符号链接越权访问
3. **文件大小限制**：读取文件时会校验文件大小（上限 10MB），防止内存溢出
4. **写入内容限制**：Agent 工具写入内容有严格的字符数限制（默认 10000 字符）
5. **权限隔离**：写入操作严格限制在 workspace 目录内，attachment 目录仅支持读取
