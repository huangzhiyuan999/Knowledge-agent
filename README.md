<div align="center">

# 🧠 Knowledge-Agent
### 个人知识库 RAG 问答系统

**基于 Java 21 & Spring Boot 3 的全链路 AI 应用**

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-red.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)]()

</div>

---

## 🌟 项目简介

个人知识库 RAG 问答系统，内置 BM25 + 向量双路检索、LLM 查询改写、得分融合排序、文档热加载、Redis 对话记忆、JWT 双令牌认证。支持将 Markdown 文档自动切块索引，通过自然语言检索问答。

---

## 🚀 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | **21** | 必须 JDK 21，支持虚拟线程 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 16+ | 前端构建 |
| npm | 7+ | 前端依赖管理 |

---

## 📦 必须安装的第三方服务

| 服务 | 版本 | 用途 | 验证命令 |
|------|------|------|---------|
| **MySQL** | 8.0+ | 用户账号存储 | `mysql -u root -p` |
| **Redis** | 6.0+ | JWT 令牌 + 对话记忆 | `redis-cli ping` |

---

## 🗄️ 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS `agent` DEFAULT CHARACTER SET utf8mb4;
USE `agent`;

CREATE TABLE IF NOT EXISTS `user` (
    `id`       BIGINT       NOT NULL AUTO_INCREMENT,
    `name`     VARCHAR(100) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role`     TINYINT      NOT NULL DEFAULT 1,
    `remark`   VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `user` (`name`, `password`, `role`, `remark`)
VALUES ('admin', 'admin123', 0, '默认管理员账号');
```

也可直接运行 `sql/init.sql`。

---

## 🔑 API Key 申请

| 服务 | 用途 | 获取地址 |
|------|------|---------|
| 阿里云百炼 DashScope | AI 大模型（必需） | https://bailian.console.aliyun.com |

> SearchAPI（联网搜索）为可选功能，不配置不影响问答。

---

## ⚙️ 配置修改（拉取后必须填写）

打开 `src/main/resources/application.yml`，替换以下字段：

```yaml
# 1. AI 大模型 Key（必填）
spring:
  ai:
    dashscope:
      api-key: 你的阿里云百炼 Key

# 2. MySQL 密码（按实际填写）
spring:
  datasource:
    password: 你的 MySQL 密码

# 3. Redis 密码（按实际填写）
spring:
  data:
    redis:
      password: 你的 Redis 密码
```

可选配置：
```yaml
# RAG 检索参数（可在配置文件中调整）
rag:
  chunk:
    size: 512          # 切块大小
    overlap: 128       # 切块重叠
  retrieval:
    rerank-top-k: 3   # 最终保留条数
```

---

## 🚀 启动命令

### 后端

```bash
# 编译
mvn clean package -DskipTests

# 开发启动
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

---

## 🌐 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:3000 |
| 后端 API | http://localhost:8123/api |
| Swagger 文档 | http://localhost:8123/api/swagger-ui.html |

默认登录账号：`admin` / `admin123`

---

## 🧠 RAG 检索管线

```
document/*.md → 切块(512/128) → JSON 存储 + MD5 增量
  → BM25(jieba分词) + 向量(Embedding) 双路索引
    → 用户提问 → LLM 改写 → 双路检索
      → 按源文件分组融合取 topK
        → 上下文拼装 → AI 回答 → Redis 记忆存储
```

---

## 🏗️ 项目结构

```
src/main/java/com/tiantian/yuaiagent/
├── agent/          ReAct 智能体（YuManus）
├── app/            知识库问答（KnowledgeApp）
├── controller/     API 接口
├── dao/            数据库访问
├── interceptor/    JWT 登录拦截器
├── rag/
│   ├── config/     配置参数
│   ├── loader/     文档加载 + 切块
│   ├── index/      BM25 + 向量索引
│   └── retriever/  改写 → 检索 → 融合排序
├── service/        Redis 对话记忆
└── eval/           效果测评框架
```

---

## 🔧 常见问题

| 异常 | 解决 |
|------|------|
| `Port 8123 was already in use` | 关闭旧进程或修改端口 |
| `Unable to connect to Redis` | 启动 Redis 服务 |
| `Unknown database 'agent'` | 执行 CREATE DATABASE |
| `DashScope API key must be set` | 填写 dashscope.api-key |
| `Table 'agent.user' doesn't exist` | 执行建表 SQL |

---

## 📄 License

Apache License 2.0
