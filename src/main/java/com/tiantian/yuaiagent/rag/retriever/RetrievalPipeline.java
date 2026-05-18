package com.tiantian.yuaiagent.rag.retriever;

import com.tiantian.yuaiagent.rag.index.Bm25Index;
import com.tiantian.yuaiagent.rag.index.VectorIndexService;
import com.tiantian.yuaiagent.rag.loader.ChunkEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 检索管线：改写 → BM25 + 向量混合检索 → 分数融合 → 上下文拼装
 */
@Slf4j
@Component
public class RetrievalPipeline {

    private final QueryProcessor queryProcessor;
    private final Bm25Index bm25Index;
    private final VectorIndexService vectorIndexService;
    private final ChunkEngine chunkEngine;
    private final Reranker reranker;

    public RetrievalPipeline(QueryProcessor queryProcessor, Bm25Index bm25Index,
                             VectorIndexService vectorIndexService, ChunkEngine chunkEngine,
                             Reranker reranker) {
        this.queryProcessor = queryProcessor;
        this.bm25Index = bm25Index;
        this.vectorIndexService = vectorIndexService;
        this.chunkEngine = chunkEngine;
        this.reranker = reranker;
    }

    /** 执行检索链路 */
    public RetrievalResult execute(String rawQuestion) {
        // 1. 改写
        String query = queryProcessor.rewrite(rawQuestion);

        // 2. BM25 + 向量 双路检索
        List<ChunkEngine.ChunkRecord> allChunks = chunkEngine.getAllChunks();
        Set<String> seen = new LinkedHashSet<>();
        List<ChunkEngine.ChunkRecord> merged = new ArrayList<>();

        for (var hit : bm25Index.search(query)) {
            if (hit.getDocIndex() >= 0 && hit.getDocIndex() < allChunks.size()) {
                ChunkEngine.ChunkRecord c = allChunks.get(hit.getDocIndex());
                if (seen.add(c.getChunkId())) merged.add(c);
            }
        }
        for (var hit : vectorIndexService.search(query)) {
            if (hit.getDocIndex() >= 0 && hit.getDocIndex() < allChunks.size()) {
                ChunkEngine.ChunkRecord c = allChunks.get(hit.getDocIndex());
                if (seen.add(c.getChunkId())) merged.add(c);
            }
        }

        // 3. 融合排序
        List<Reranker.ScoredChunk> ranked = reranker.fuse(merged, query);
        List<ChunkEngine.ChunkRecord> finalChunks = ranked.stream()
                .map(Reranker.ScoredChunk::getChunk)
                .collect(Collectors.toList());

        // 4. 按源文件排序
        finalChunks = sortBySource(finalChunks);

        // 5. 组装上下文
        String context = finalChunks.stream()
                .map(c -> "【" + c.getSourceFile() + "】\n" + c.getText())
                .collect(Collectors.joining("\n\n---\n\n"));

        return new RetrievalResult(context, finalChunks, query);
    }

    private List<ChunkEngine.ChunkRecord> sortBySource(List<ChunkEngine.ChunkRecord> chunks) {
        return chunks.stream()
                .collect(Collectors.groupingBy(ChunkEngine.ChunkRecord::getSourceFile,
                        LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .peek(list -> list.sort(Comparator.comparingInt(ChunkEngine.ChunkRecord::getChunkIndex)))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public static class RetrievalResult {
        private final String context; private final List<ChunkEngine.ChunkRecord> chunks; private final String rewrittenQuery;
        public RetrievalResult(String context, List<ChunkEngine.ChunkRecord> chunks, String rewrittenQuery) {
            this.context = context; this.chunks = chunks; this.rewrittenQuery = rewrittenQuery;
        }
        public String getContext() { return context; }
        public List<ChunkEngine.ChunkRecord> getChunks() { return chunks; }
        public String getRewrittenQuery() { return rewrittenQuery; }
    }
}
