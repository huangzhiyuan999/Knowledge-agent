package com.tiantian.yuaiagent.rag.index;

import com.tiantian.yuaiagent.rag.config.RagProperties;
import com.tiantian.yuaiagent.rag.loader.ChunkEngine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 稠密向量索引（语义检索）
 * 索引与 chunkEngine.getAllChunks() 对齐，通过 originIndex 元数据映射
 */
@Slf4j
@Component
public class VectorIndexService {

    private final ChunkEngine chunkEngine;
    private final EmbeddingModel embeddingModel;
    private final RagProperties ragProperties;
    private volatile SimpleVectorStore vectorStore;

    public VectorIndexService(ChunkEngine chunkEngine, EmbeddingModel embeddingModel, RagProperties ragProperties) {
        this.chunkEngine = chunkEngine;
        this.embeddingModel = embeddingModel;
        this.ragProperties = ragProperties;
        chunkEngine.onRebuild(this::rebuild);
    }

    @PostConstruct
    public void init() { rebuild(); }

    public synchronized void rebuild() {
        List<ChunkEngine.ChunkRecord> allChunks = chunkEngine.getAllChunks();
        if (allChunks.isEmpty()) { log.warn("向量索引无文档"); return; }

        try {
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < allChunks.size(); i++) {
                String text = allChunks.get(i).getText();
                if (text == null || text.isBlank()) continue;
                docs.add(new Document(text, Map.of("originIndex", i)));
            }
            if (docs.isEmpty()) { log.warn("向量索引无有效文档"); return; }
            store.add(docs);
            vectorStore = store;
            log.info("向量索引构建完成，共 {} 条 Chunk", docs.size());
        } catch (Exception e) {
            log.warn("向量索引构建失败（不影响 BM25 检索）: {}", e.getMessage());
            vectorStore = null;
        }
    }

    public List<VectorHit> search(String query) {
        var store = vectorStore;
        if (store == null) return List.of();
        try {
            List<Document> results = store.similaritySearch(query);
            int topK = ragProperties.getSemanticTopK();
            List<VectorHit> hits = new ArrayList<>();
            for (int i = 0; i < Math.min(results.size(), topK); i++) {
                Document doc = results.get(i);
                Object idx = doc.getMetadata().get("originIndex");
                if (idx == null) continue;
                hits.add(new VectorHit((int) idx, 1.0 - (i / (double) topK), doc.getText()));
            }
            return hits;
        } catch (Exception e) {
            log.warn("向量检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    public static class VectorHit {
        private final int docIndex; private final double score; private final String text;
        public VectorHit(int docIndex, double score, String text) { this.docIndex = docIndex; this.score = score; this.text = text; }
        public int getDocIndex() { return docIndex; }
        public double getScore() { return score; }
        public String getText() { return text; }
    }
}
