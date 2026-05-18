package com.tiantian.yuaiagent.rag.loader;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tiantian.yuaiagent.rag.config.RagProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档管道：加载 → 切块 → JSON 存储 → MD5 增量 → 定时热加载
 */
@Slf4j
@Component
@EnableScheduling
public class ChunkEngine {

    private final ResourcePatternResolver resourcePatternResolver;
    private final RagProperties ragProperties;

    /** chunkId → ChunkRecord */
    private volatile Map<String, ChunkRecord> chunkCache = new LinkedHashMap<>();

    /** 文档 MD5 快照（用于热加载对比） */
    private volatile Map<String, String> currentManifest = new HashMap<>();

    /** 回调：索引重建 */
    private Runnable rebuildCallback = () -> {};

    public ChunkEngine(ResourcePatternResolver resourcePatternResolver, RagProperties ragProperties) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.ragProperties = ragProperties;
    }

    @PostConstruct
    public void init() { fullRefresh(); }

    /** 注册索引重建回调（由 Bm25Index / VectorIndexService 调用） */
    public void onRebuild(Runnable callback) {
        this.rebuildCallback = callback;
    }

    /** 定时热加载（根据配置间隔） */
    @Scheduled(fixedDelayString = "#{@ragProperties.hotReloadInterval * 1000}")
    public void hotReload() {
        if (ragProperties.getHotReloadInterval() <= 0) return;
        try {
            Map<String, String> latest = computeManifest();
            if (!latest.equals(currentManifest)) {
                log.info("检测到文档变更，重新加载...");
                fullRefresh();
            }
        } catch (Exception e) {
            log.warn("热加载检查失败", e);
        }
    }

    /** 全量刷新 */
    public synchronized void fullRefresh() {
        try {
            boolean changed = checkAndUpdate();
            loadChunksFromDisk();
            if (changed) {
                rebuildCallback.run();
            }
        } catch (Exception e) {
            log.error("文档刷新失败", e);
        }
    }

    /** MD5 对比，有变更则重新切块 */
    public boolean checkAndUpdate() throws IOException {
        Map<String, String> latest = computeManifest();
        if (latest.equals(currentManifest)) return false;

        Resource[] resources = resourcePatternResolver.getResources(ragProperties.getDocDir());
        regenerateChunks(resources);
        currentManifest = latest;
        return true;
    }

    private Map<String, String> computeManifest() throws IOException {
        Map<String, String> map = new HashMap<>();
        Resource[] resources = resourcePatternResolver.getResources(ragProperties.getDocDir());
        for (Resource r : resources) {
            String name = r.getFilename();
            if (name == null) continue;
            String md5 = DigestUtil.md5Hex(r.getContentAsByteArray());
            map.put(name, md5);
        }
        return map;
    }

    private void regenerateChunks(Resource[] resources) throws IOException {
        File chunkDir = getChunkDirFile();
        if (chunkDir.exists()) {
            for (File f : Objects.requireNonNull(chunkDir.listFiles())) f.delete();
        }
        chunkDir.mkdirs();

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) continue;

            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .withAdditionalMetadata("filename", filename)
                    .build();
            List<Document> rawDocs = new MarkdownDocumentReader(resource, config).get();

            TokenTextSplitter splitter = new TokenTextSplitter(
                    ragProperties.getChunkSize(), ragProperties.getChunkOverlap(), 10, 5000, true);
            List<Document> chunks = splitter.apply(rawDocs);

            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                String chunkId = UUID.randomUUID().toString().replace("-", "");
                ChunkRecord record = new ChunkRecord(chunkId, chunk.getText(), filename, i + 1, chunks.size(), chunk.getMetadata());
                String json = JSONUtil.parseObj(record).toStringPretty();
                String dir = "src/main/resources/" + ragProperties.getChunkDir();
                new File(dir).mkdirs();
                Files.writeString(new File(dir, chunkId + ".json").toPath(), json);
            }
            log.info("文档 {} 已切为 {} 块", filename, chunks.size());
        }
        saveManifest();
    }

    private void loadChunksFromDisk() throws IOException {
        Map<String, ChunkRecord> map = new LinkedHashMap<>();
        File dir = getChunkDirFile();
        if (!dir.exists()) return;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            ChunkRecord record = JSONUtil.toBean(Files.readString(f.toPath()), ChunkRecord.class);
            map.put(record.getChunkId(), record);
        }
        chunkCache = map;
        log.info("已加载 {} 个切块", chunkCache.size());
    }

    private void saveManifest() throws IOException {
        JSONObject obj = new JSONObject();
        currentManifest.forEach(obj::set);
        getManifestFile().getParentFile().mkdirs();
        Files.writeString(getManifestFile().toPath(), obj.toStringPretty());
    }

    private File getManifestFile() { return new File("src/main/resources/" + ragProperties.getChunkDir(), ".manifest.json"); }
    private File getChunkDirFile() { return new File("src/main/resources/" + ragProperties.getChunkDir()); }

    public List<ChunkRecord> getAllChunks() { return List.copyOf(chunkCache.values()); }
    public List<String> getAllChunkTexts() { return chunkCache.values().stream().map(ChunkRecord::getText).collect(Collectors.toList()); }

    public static class ChunkRecord {
        private String chunkId; private String text; private String sourceFile;
        private int chunkIndex; private int totalChunks; private Map<String, Object> metadata;
        public ChunkRecord() {}
        public ChunkRecord(String chunkId, String text, String sourceFile, int chunkIndex, int totalChunks, Map<String, Object> metadata) {
            this.chunkId = chunkId; this.text = text; this.sourceFile = sourceFile;
            this.chunkIndex = chunkIndex; this.totalChunks = totalChunks; this.metadata = metadata;
        }
        public String getChunkId() { return chunkId; }
        public void setChunkId(String chunkId) { this.chunkId = chunkId; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getSourceFile() { return sourceFile; }
        public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}
