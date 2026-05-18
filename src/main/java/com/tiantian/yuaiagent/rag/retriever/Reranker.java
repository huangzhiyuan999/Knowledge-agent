package com.tiantian.yuaiagent.rag.retriever;

import com.tiantian.yuaiagent.rag.config.RagProperties;
import com.tiantian.yuaiagent.rag.loader.ChunkEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 融合排序：按源文件分组，每篇文档取最佳 chunk
 * 确保召回覆盖所有相关文档，不遗漏
 */
@Slf4j
@Component
public class Reranker {

    private final RagProperties ragProperties;

    public Reranker(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /** 融合排序：按源文件分组 → 每组取最佳 → 轮流取直到 topK */
    public List<ScoredChunk> fuse(List<ChunkEngine.ChunkRecord> merged, String query) {
        if (merged.isEmpty()) return List.of();

        // 按源文件分组，每组内部按位置排序
        Map<String, List<ChunkEngine.ChunkRecord>> grouped = new LinkedHashMap<>();
        for (ChunkEngine.ChunkRecord c : merged) {
            grouped.computeIfAbsent(c.getSourceFile(), k -> new ArrayList<>()).add(c);
        }

        // 轮流从每个组取一条，保证各文档都有代表
        int topK = ragProperties.getReRankTopK();
        List<ScoredChunk> result = new ArrayList<>();
        int maxGroup = grouped.values().stream().mapToInt(List::size).max().orElse(0);

        for (int i = 0; i < maxGroup && result.size() < topK; i++) {
            for (var entry : grouped.entrySet()) {
                if (i < entry.getValue().size()) {
                    double score = 1.0 - (result.size() / (double) topK) * 0.3;
                    result.add(new ScoredChunk(entry.getValue().get(i), score));
                    if (result.size() >= topK) break;
                }
            }
        }

        log.info("融合排序完成，保留 {} 条，来源文档: {}", result.size(),
                result.stream().map(s -> s.chunk.getSourceFile()).distinct().collect(Collectors.toList()));
        return result;
    }

    public static class ScoredChunk {
        private final ChunkEngine.ChunkRecord chunk;
        private final double score;
        public ScoredChunk(ChunkEngine.ChunkRecord chunk, double score) {
            this.chunk = chunk; this.score = score;
        }
        public ChunkEngine.ChunkRecord getChunk() { return chunk; }
        public double getScore() { return score; }
    }
}
