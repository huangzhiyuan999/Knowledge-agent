package com.tiantian.yuaiagent.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 核心配置（从 application.yml 加载）
 */
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    // === 切块参数 ===
    private int chunkSize = 512;
    private int chunkOverlap = 128;
    private String docDir = "classpath:document/*.md";
    private String chunkDir = "chunk";

    // === 检索参数 ===
    private int keywordTopK = 5;
    private int semanticTopK = 5;
    private int reRankTopK = 3;
    private double bm25Weight = 0.5;
    private double vectorWeight = 0.5;

    // === BM25 参数 ===
    private double bm25K1 = 1.5;
    private double bm25B = 0.75;

    // === 向量模型 ===
    private String embeddingModel = "dashscope";
    private String vectorDbType = "simple";

    // === 查询改写 ===
    private boolean rewriteEnabled = true;

    // === 对话上下文 ===
    private String contextPrefix = "参考知识：\n";

    // === 热加载 ===
    /** 文档热加载间隔（秒），0 表示不开启 */
    private int hotReloadInterval = 30;

    // getter / setter
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
    public String getDocDir() { return docDir; }
    public void setDocDir(String docDir) { this.docDir = docDir; }
    public String getChunkDir() { return chunkDir; }
    public void setChunkDir(String chunkDir) { this.chunkDir = chunkDir; }
    public int getKeywordTopK() { return keywordTopK; }
    public void setKeywordTopK(int keywordTopK) { this.keywordTopK = keywordTopK; }
    public int getSemanticTopK() { return semanticTopK; }
    public void setSemanticTopK(int semanticTopK) { this.semanticTopK = semanticTopK; }
    public int getReRankTopK() { return reRankTopK; }
    public void setReRankTopK(int reRankTopK) { this.reRankTopK = reRankTopK; }
    public double getBm25Weight() { return bm25Weight; }
    public void setBm25Weight(double bm25Weight) { this.bm25Weight = bm25Weight; }
    public double getVectorWeight() { return vectorWeight; }
    public void setVectorWeight(double vectorWeight) { this.vectorWeight = vectorWeight; }
    public double getBm25K1() { return bm25K1; }
    public void setBm25K1(double bm25K1) { this.bm25K1 = bm25K1; }
    public double getBm25B() { return bm25B; }
    public void setBm25B(double bm25B) { this.bm25B = bm25B; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public String getVectorDbType() { return vectorDbType; }
    public void setVectorDbType(String vectorDbType) { this.vectorDbType = vectorDbType; }
    public boolean isRewriteEnabled() { return rewriteEnabled; }
    public void setRewriteEnabled(boolean rewriteEnabled) { this.rewriteEnabled = rewriteEnabled; }
    public String getContextPrefix() { return contextPrefix; }
    public void setContextPrefix(String contextPrefix) { this.contextPrefix = contextPrefix; }
    public int getHotReloadInterval() { return hotReloadInterval; }
    public void setHotReloadInterval(int hotReloadInterval) { this.hotReloadInterval = hotReloadInterval; }
}
