# 天天AI超级智能体应用平台

基于 Spring AI + Spring Boot 3 的 AI 智能体应用平台，包含知识库问答系统和 AI 超级智能体两大核心功能。

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | **21** | 必须 JDK 21，支持虚拟线程 |
| Maven | 3.8+ | 项目构建 |
| Node.js | 16+ | 前端构建 |
| npm | 7+ | 前端依赖管理 |

## 必须安装的第三方服务

| 服务 | 版本 | 用途 | 启动检查 |
|------|------|------|---------|
| **MySQL** | 8.0+ | 用户账号存储 | `mysql -u root -p` |
| **Redis** | 6.0+ | JWT 双令牌 + 对话记忆 | `redis-cli ping` → PONG |

### 数据库初始化

```sql
-- 创建数据库（字符集 utf8mb4）
CREATE DATABASE IF NOT EXISTS `agent` DEFAULT CHARSET utf8mb4;

-- 创建用户表
CREATE TABLE IF NOT EXISTS `agent`.`user` (
    `id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`     VARCHAR(100) NOT NULL                COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL                COMMENT '密码',
    `role`     TINYINT      NOT NULL DEFAULT 1       COMMENT '角色 0=管理员 1=用户',
    `remark`   VARCHAR(500) DEFAULT NULL             COMMENT '备注信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入默认管理员（密码：admin123）
INSERT INTO `agent`.`user` (`name`, `password`, `role`, `remark`)
VALUES ('admin', 'admin123', 0, '默认管理员账号');
```

## 可选服务

| 服务 | 用途 | 不启动的影响 |
|------|------|-------------|
| Ollama | 本地大模型部署 | 不影响，默认走云端 DashScope |
| PostgreSQL + PgVector | 生产级向量存储 | 不影响，默认用内存向量库 |
| SearchAPI | 联网搜索工具 | AI 超级智能体的搜索功能不可用 |

## API Key 申请

| 服务 | 用途 | 获取地址 |
|------|------|---------|
| **阿里云百炼 DashScope** | AI 大模型（必需） | https://bailian.console.aliyun.com/ |
| **SearchAPI** | 联网搜索（可选） | https://www.searchapi.io/ |

## 配置修改（拉取后必须填写）

打开 `src/main/resources/application.yml`，找到以下配置项替换：

```yaml
# 1. AI 大模型 API Key（必填）
spring:
  ai:
    dashscope:
      api-key: 替换为你的阿里云百炼 Key

# 2. MySQL 连接信息（按需修改）
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/agent?useUnicode=true&characterEncoding=utf-8
    username: root
    password: 替换为你的 MySQL 密码

# 3. Redis 连接信息（按需修改）
spring:
  data:
    redis:
      password: 替换为你的 Redis 密码

# 4. SearchAPI Key（可选，不用则 AI 无法联网搜索）
search-api:
  api-key: 替换为你的 SearchAPI Key
```

## 核心技术依赖

| 依赖 | 用途 |
|------|------|
| Spring Boot 3.4.4 | Web 框架 |
| Spring AI 1.0.0 | AI 开发框架 |
| spring-ai-alibaba / DashScope | 阿里云百炼大模型接入 |
| spring-boot-starter-data-redis | Redis 客户端 |
| mysql-connector-j | MySQL 驱动 |
| jjwt (0.12.6) | JWT 双令牌认证 |
| jieba-analysis | 中文分词（BM25 检索） |
| Hutool 5.8.37 | 工具库 |
| Knife4j | 接口文档 |
| iText 9.1.0 | PDF 生成 |
| jsoup | 网页抓取 |

## 启动命令

### 后端

```bash
# 编译打包
mvn clean package -DskipTests

# 本地启动（开发）
mvn spring-boot:run

# 生产部署
java -jar target/yu-ai-agent-0.0.1-SNAPSHOT.jar
```

### 前端

```bash
cd yu-ai-agent-frontend

# 安装依赖
npm install

# 开发启动
npm run dev

# 生产构建
npm run build
```

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:3000 |
| 后端 API | http://localhost:8123/api |
| Swagger 文档 | http://localhost:8123/api/swagger-ui.html |
| 健康检查 | POST http://localhost:8123/api/user/login |

## 登录账号

```
用户名：admin
密码：admin123
```

## 常见启动异常

| 异常 | 原因 | 解决 |
|------|------|------|
| `Port 8123 was already in use` | 端口被占用 | `kill` 旧进程或修改 `server.port` |
| `Unable to connect to Redis` | Redis 未启动 | 启动 Redis 服务 |
| `Unknown database 'agent'` | 数据库未创建 | 执行 `CREATE DATABASE agent` |
| `Access denied for user 'root'` | MySQL 密码错误 | 检查 `application.yml` 中的 datasource 密码 |
| `DashScope API key must be set` | AI Key 未配置 | 填写 `dashscope.api-key` |
| `Table 'agent.user' doesn't exist` | 用户表未创建 | 执行建表 SQL |
| `Package 'com.tiantian' not found` | JDK 版本不对 | 确认使用 JDK 21 |

## 项目结构

```
src/main/java/com/tiantian/yuaiagent/
├── agent/          ← ReAct 智能体（YuManus）
├── app/            ← 知识库问答应用（KnowledgeApp）
├── config/         ← 全局配置 + 拦截器
├── controller/     ← API 接口
├── dao/            ← 数据库访问
├── interceptor/    ← JWT 登录拦截器
├── model/          ← 数据实体
├── rag/            ← RAG 检索管线
│   ├── config/     ← 配置参数
│   ├── loader/     ← 文档加载 + 切块
│   ├── index/      ← BM25 + 向量索引
│   └── retriever/  ← 改写 → 检索 → 融合排序
├── service/        ← Redis 对话记忆
├── util/           ← JWT 工具
└── eval/           ← 效果测评框架
```

## RAG 管线流程

```
document/*.md → 切块(512/128) → JSON存储 + MD5增量
  → BM25(jieba分词) + 向量(Embedding) 双路索引
    → 用户提问 → LLM改写 → 混合检索
      → 按源文件分组融合 → topK → 上下文拼装
        → AI回答 → Redis记忆存储
```
