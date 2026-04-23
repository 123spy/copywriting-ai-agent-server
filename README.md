# Copywriting AI Agent Server

`copywriting-ai-agent-server` 是这个项目的后端服务，基于 `Spring Boot 3 + Spring AI Alibaba + MyBatis-Plus`，负责用户登录、文案生成工作流编排、项目结果落库，以及项目维度的常规 CRUD 查询能力。

## 当前能力

- 用户注册、登录、登出、获取当前登录用户
- 通过 `AgentFlow` 执行文案生成主流程
- 将生成后的项目、文案结果、配图结果保存到 MySQL
- 提供内容项目的完整常规接口：
  - 创建生成任务
  - 查询单个项目
  - 查询项目详情（包含文案结果和图片结果）
  - 分页查询我的项目
  - 更新项目基础信息
  - 删除项目及其关联结果

## 技术栈

- `Spring Boot 3.5`
- `Spring AI Alibaba`
- `Spring AI PGVector`
- `MyBatis-Plus`
- `MySQL`
- `PostgreSQL / PGVector`
- `Knife4j / OpenAPI`

## 目录说明

```text
copywriting-ai-agent-server
├─ src/main/java/com/spy/copywritingaiagentserver
│  ├─ controller        # 接口层
│  ├─ service           # 业务层
│  ├─ model             # domain / dto / vo
│  ├─ ai                # 智能体、工具、RAG、工作流
│  └─ workflow          # StateGraph 编排节点
├─ src/main/resources
│  ├─ application.yml
│  ├─ application-local.yml
│  ├─ documents         # RAG 语料与平台规则
│  └─ mapper            # MyBatis XML
└─ sql/create_table.sql # MySQL 建表脚本
```

## 启动前准备

### 1. 环境要求

- `JDK 17`
- `Maven 3.9+`
- `MySQL 8+`
- `PostgreSQL 14+`，并启用 `pgvector`

### 2. 初始化数据库

先执行 MySQL 建表脚本：

```sql
source sql/create_table.sql;
```

### 3. 配置本地参数

项目默认启用 `local` profile：

```yaml
spring:
  profiles:
    active: local
```

请根据自己的环境检查并修改这些配置：

- `src/main/resources/application.yml`
  - MySQL 数据源
  - 服务端口
- `src/main/resources/application-local.yml`
  - DashScope API Key
  - PGVector 数据源
  - 搜索工具 API Key
  - COS 对象存储配置

建议把敏感配置替换成你自己的本地密钥，不要直接用于生产环境。

## 启动方式

在 `copywriting-ai-agent-server` 目录下执行：

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

默认访问地址：

- 服务根路径：`http://localhost:8135/api`
- Swagger / Knife4j：`http://localhost:8135/api/doc.html`
- OpenAPI：`http://localhost:8135/api/v3/api-docs`

## 核心接口

### 用户接口

基路径：`/api/user`

- `POST /register` 注册
- `POST /login` 登录
- `POST /logout` 登出
- `GET /get/login` 获取当前登录用户

当前项目使用 `Session` 维持登录态，所以联调时需要带上同一个会话。

### 内容项目接口

基路径：`/api/contentProject`

#### 1. 创建生成任务

`POST /create`

请求体：

```json
{
  "platform": "xiaohongshu",
  "topic": "低糖早餐代餐",
  "audience": "上班族女性",
  "tone": "真实、轻松、有种草感",
  "productInfo": "一款低糖高蛋白早餐奶昔，冲泡方便",
  "requirement": "帮我生成一篇适合小红书发布的种草内容，并给出配图提示词"
}
```

返回值为 `FinalPostResult`，同时会自动创建：

- `content_project`
- `content_result`
- `project_image`（如果生成了图片）

#### 2. 查询单个项目

`GET /get?id={projectId}`

返回 `ContentProject`。

#### 3. 查询项目详情

`GET /get/detail?id={projectId}`

返回结构：

```json
{
  "contentProject": {},
  "contentResultList": [],
  "projectImageList": []
}
```

适合前端详情页一次性拉取。

#### 4. 分页查询我的项目

`POST /list/page`

请求体示例：

```json
{
  "current": 1,
  "pageSize": 10,
  "platform": "xiaohongshu",
  "status": 2,
  "searchText": "早餐",
  "sortField": "id",
  "sortOrder": "descend"
}
```

支持字段：

- `id`
- `projectName`
- `platform`
- `topic`
- `audience`
- `tone`
- `normalizedStyle`
- `status`
- `searchText`
- `current`
- `pageSize`
- `sortField`
- `sortOrder`

说明：

- 只会返回当前登录用户自己的项目
- `pageSize` 当前限制为不超过 `100`

#### 5. 更新项目

`POST /update`

请求体示例：

```json
{
  "id": 1,
  "projectName": "早餐奶昔小红书项目",
  "platform": "xiaohongshu",
  "topic": "低糖早餐代餐",
  "audience": "通勤上班族",
  "tone": "真实分享",
  "normalizedStyle": "gentle-authentic",
  "productInfo": "升级后的产品说明",
  "requirement": "需要更强调便捷和低糖"
}
```

说明：

- 只允许更新自己的项目
- `status` 与 `userId` 不允许通过接口直接修改
- `RUNNING` 状态的项目不允许修改

#### 6. 删除项目

`POST /delete`

请求体：

```json
{
  "id": 1
}
```

说明：

- 只允许删除自己的项目
- 会一起删除关联的 `content_result` 和 `project_image`
- `RUNNING` 状态的项目不允许删除

## 数据表说明

### `content_project`

内容创作项目主表，保存用户输入和任务状态。

核心字段：

- `userId`
- `projectName`
- `platform`
- `topic`
- `audience`
- `tone`
- `normalizedStyle`
- `productInfo`
- `requirement`
- `status`

### `content_result`

保存生成出来的文案与评分结果。

核心字段：

- `projectId`
- `title`
- `openingHook`
- `body`
- `cta`
- `imagePrompt`
- `reviewPass`
- `titleScore`
- `bodyScore`
- `imagePromptScore`
- `overallScore`
- `reviewFeedback`

### `project_image`

保存项目关联的图片结果。

核心字段：

- `projectId`
- `resultId`
- `imageUrl`

## 开发建议

- 如果只联调 CRUD，可以先登录后直接调 `/contentProject/get`、`/get/detail`、`/list/page`
- 如果要跑完整生成链路，请确保 DashScope、PGVector、搜索接口、COS 都已配置可用
- 如果后续要扩展“重生成”“版本记录”“草稿保存”，可以继续沿用当前 `content_project -> content_result -> project_image` 的结构
