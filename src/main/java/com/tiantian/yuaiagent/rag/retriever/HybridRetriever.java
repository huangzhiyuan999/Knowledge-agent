package com.tiantian.yuaiagent.rag.retriever;

import com.tiantian.yuaiagent.rag.config.RagProperties;
import com.tiantian.yuaiagent.rag.index.Bm25Index;
import com.tiantian.yuaiagent.rag.index.VectorIndexService;
import com.tiantian.yuaiagent.rag.loader.ChunkEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索器：BM25 + 向量多路召回 → 融合排序
 * 权重（bm25-weight / vector-weight）从 rag.retrieval 配置读取
 */
@Slf4j
@Component
public class HybridRetriever {

    private final Bm25Index bm25Index;
    private final VectorIndexService vectorIndexService;
    private final ChunkEngine chunkEngine;
    private final RagProperties ragProperties;

    public HybridRetriever(Bm25Index bm25Index, VectorIndexService vectorIndexService,
                           ChunkEngine chunkEngine, RagProperties ragProperties) {
        this.bm25Index = bm25Index;
        this.vectorIndexService = vectorIndexService;
        this.chunkEngine = chunkEngine;
        this.ragProperties = ragProperties;
    }

    /** 混合检索 */
    public List<RetrievalResult> retrieve(String query) {
        List<Bm25Index.Bm25Hit> bm25Hits = bm25Index.search(query);
        List<VectorIndexService.VectorHit> vectorHits = vectorIndexService.search(query);

        Map<Integer, Double> scoreMap = new HashMap<>();
        double maxBm25 = bm25Hits.isEmpty() ? 1 : bm25Hits.get(0).getScore();
        double bm25W = ragProperties.getBm25Weight();
        for (var hit : bm25Hits)
            scoreMap.merge(hit.getDocIndex(), hit.getScore() / maxBm25 * bm25W, Double::sum);

        double maxVec = vectorHits.isEmpty() ? 1 : vectorHits.get(0).getScore();
        double vecW = ragProperties.getVectorWeight();
        for (var hit : vectorHits)
            scoreMap.merge(hit.getDocIndex(), hit.getScore() / maxVec * vecW, Double::sum);

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(ragProperties.getReRankTopK())
                .map(entry -> new RetrievalResult(
                        chunkEngine.getAllChunks().get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    public static class RetrievalResult {
        private final ChunkEngine.ChunkRecord chunk;
        private final double score;
        public RetrievalResult(ChunkEngine.ChunkRecord chunk, double score) { this.chunk = chunk; this.score = score; }
        public ChunkEngine.ChunkRecord getChunk() { return chunk; }
        public double getScore() { return score; }
        public String getContent() { return chunk.getText(); }
    }
}
