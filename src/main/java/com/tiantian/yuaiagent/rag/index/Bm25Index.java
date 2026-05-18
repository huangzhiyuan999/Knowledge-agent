package com.tiantian.yuaiagent.rag.index;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tiantian.yuaiagent.rag.config.RagProperties;
import com.tiantian.yuaiagent.rag.loader.ChunkEngine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BM25 倒排索引（中文 jieba 分词）
 * 索引与 getAllChunks 对齐，热加载时自动重建
 */
@Slf4j
@Component
public class Bm25Index {

    private final ChunkEngine chunkEngine;
    private final RagProperties ragProperties;
    private final JiebaSegmenter segmenter = new JiebaSegmenter();
    private final double k1;
    private final double b;

    private volatile Map<String, Integer> df = new HashMap<>();
    private volatile List<Map<String, Integer>> tfs = new ArrayList<>();
    private volatile List<Integer> docLengths = new ArrayList<>();
    private volatile int totalDocs;
    private volatile double avgDocLen;

    public Bm25Index(ChunkEngine chunkEngine, RagProperties ragProperties) {
        this.chunkEngine = chunkEngine;
        this.ragProperties = ragProperties;
        this.k1 = ragProperties.getBm25K1();
        this.b = ragProperties.getBm25B();
        // 注册热加载回调
        chunkEngine.onRebuild(this::rebuild);
    }

    @PostConstruct
    public void init() { rebuild(); }

    public synchronized void rebuild() {
        List<ChunkEngine.ChunkRecord> chunks = chunkEngine.getAllChunks();
        totalDocs = chunks.size();
        Map<String, Integer> newDf = new HashMap<>();
        List<Map<String, Integer>> newTfs = new ArrayList<>();
        List<Integer> newLengths = new ArrayList<>();

        if (totalDocs == 0) { log.warn("BM25 无文档可索引"); return; }

        double totalLen = 0;
        for (ChunkEngine.ChunkRecord chunk : chunks) {
            String text = chunk.getText();
            List<String> tokens = (text != null) ? tokenize(text) : List.of();
            Map<String, Integer> tf = new HashMap<>();
            for (String token : tokens) tf.merge(token, 1, Integer::sum);
            for (String token : tf.keySet()) newDf.merge(token, 1, Integer::sum);
            newTfs.add(tf);
            newLengths.add(tokens.size());
            totalLen += tokens.size();
        }
        avgDocLen = totalDocs > 0 ? totalLen / totalDocs : 0;
        df = newDf; tfs = newTfs; docLengths = newLengths;
        log.info("BM25 索引构建完成，共 {} 篇文档", totalDocs);
    }

    public List<Bm25Hit> search(String query) {
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty() || totalDocs == 0) return List.of();
        Map<String, Integer> localDf = df;
        List<Map<String, Integer>> localTfs = tfs;
        List<Integer> localLengths = docLengths;
        int localTotal = totalDocs;
        double localAvg = avgDocLen;

        List<Bm25Hit> hits = new ArrayList<>();
        for (int docIdx = 0; docIdx < localTotal; docIdx++) {
            double score = 0;
            Map<String, Integer> tf = localTfs.get(docIdx);
            int dl = localLengths.get(docIdx);
            for (String qt : queryTokens) {
                int termFreq = tf.getOrDefault(qt, 0);
                if (termFreq == 0) continue;
                int docFreq = localDf.getOrDefault(qt, 0);
                double idf = Math.log((localTotal - docFreq + 0.5) / (docFreq + 0.5) + 1.0);
                score += idf * (termFreq * (k1 + 1)) / (termFreq + k1 * (1 - b + b * dl / localAvg));
            }
            if (score > 0) hits.add(new Bm25Hit(docIdx, score));
        }
        hits.sort((a, b) -> Double.compare(b.score, a.score));
        int topK = ragProperties.getKeywordTopK();
        return hits.size() > topK ? hits.subList(0, topK) : hits;
    }

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        try {
            return segmenter.process(text, JiebaSegmenter.SegMode.SEARCH).stream()
                    .map(t -> t.word.trim()).filter(w -> w.length() >= 2)
                    .filter(w -> !w.matches("[\\d\\W]+")).collect(Collectors.toList());
        } catch (Exception e) { return List.of(); }
    }

    public static class Bm25Hit {
        private final int docIndex;
        private final double score;
        public Bm25Hit(int docIndex, double score) { this.docIndex = docIndex; this.score = score; }
        public int getDocIndex() { return docIndex; }
        public double getScore() { return score; }
    }
}
