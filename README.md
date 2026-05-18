<div align="center">

<!-- 封面图 (参考你提供的小门道风格) -->
<img src="https://www.xiaomendao.cn/assets/img/wenda.jpg" alt="个人知识库 RAG 问答系统" width="800"/>
<br>
<br>

# 🧠 Personal-Knowledge-RAG
### 🚀 面向个人知识管理的多模态 RAG 问答系统

**基于 Java 21 & Spring Boot 3 的全链路工程化实践**

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://java.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-red.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

</div>

---

## 🌟 项目简介

这是一个专为**个人知识管理场景**设计的高性能多模态 RAG（检索增强生成）系统。

本项目不仅仅是一个简单的问答工具，而是围绕**离线索引、在线检索、效果测评、增量索引与缓存加速**完成了全链路的工程优化。系统支持 PDF、Markdown、图片等多种常见知识源，旨在解决私有数据与大模型结合时的“幻觉”与“延迟”痛点，显著提升个人知识场景下的问答准确率与系统响应效率。

---

## 🚀 核心特性

### 1. 📚 多模态知识源支持
- **文档解析**：支持 PDF (iText)、Markdown、TXT 等文本格式。
- **视觉理解**：集成 OCR 与 VLM (Vision-Language Model)，支持图片内容的问答与检索。

### 2. ⚙️ 全链路工程优化
- **离线索引**：基于高性能批处理构建向量库。
- **在线检索**：结合 **Chroma** 向量检索与 **BM25** 稀疏检索，实现混合搜索 (Hybrid Search)。
- **缓存加速**：引入 Kryo 高性能序列化与多级缓存策略，降低大模型调用成本，提升响应速度。
- **增量索引**：支持知识库的动态更新，无需全量重建。

### 3. 🧠 智能体与工具调用
- **ReAct Agent**：构建智能体框架，让 AI 能够自主思考与决策。
- **Tool Calling**：支持调用外部工具（如 SearchAPI, Pexels API）扩展模型能力。
- **MCP 协议**：基于模型上下文协议，实现模块化的上下文管理。

---

## 🛠️ 技术栈

本项目采用现代化 Java 技术栈，深度融合 AI 工程化最佳实践：

| 模块 | 技术选型 | 说明 |
| :--- | :--- | :--- |
| **核心框架** | Java 21 + Spring Boot 3 | 响应式编程与现代化 Java 特性 |
| **AI 开发** | Spring AI + LangChain4j | 统一的 AI 编程模型与组件 |
| **向量数据库** | **PGvector** | 基于 PostgreSQL 的向量扩展，保证数据一致性 |
| **大模型部署** | **Ollama** | 本地化大模型运行环境 |
| **云平台** | **Serverless** + **百炼** | 弹性计算与阿里云大模型开发平台 |
| **工具库** | Jsoup, iText, Kryo, Knife4j | 网页抓取、PDF生成、序列化、接口文档 |

---

## 📊 效果测评与优化

我们引入了 **RAGAS** 评估框架，对系统的**准确性 (Accuracy)**、**相关性 (Faithfulness)** 和**上下文召回率 (Context Recall)** 进行量化监控。

- **检索优化**：通过调整 Chunk 大小与重排序 (Rerank) 策略，确保 Top-K 结果的精准度。
- **Prompt 工程**：基于 ReAct 模式优化提示词，引导模型进行“思考-行动-观察”的循环。

---

## 🚀 快速开始

### 1. 环境准备
- JDK 21
- Maven 3.8+
- PostgreSQL (启用 PGvector 扩展)
- Ollama (运行本地模型，如 `llama3` 或 `qwen`)

### 2. 克隆与配置
```bash
git clone https://github.com/yourname/Personal-Knowledge-RAG.git
cd Personal-Knowledge-RAG
