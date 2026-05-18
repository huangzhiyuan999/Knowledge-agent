package com.tiantian.yuaiagent.rag.retriever;

import com.tiantian.yuaiagent.rag.config.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 查询处理器：LLM 改写口语化问题为标准问句
 * 提升 BM25 关键词匹配率
 */
@Slf4j
@Component
public class QueryProcessor {

    private final ChatModel chatModel;
    private final RagProperties ragProperties;
    private final PromptTemplate rewriteTemplate;

    public QueryProcessor(ChatModel dashscopeChatModel, RagProperties ragProperties) {
        this.chatModel = dashscopeChatModel;
        this.ragProperties = ragProperties;
        this.rewriteTemplate = loadTemplate("prompts/query-rewrite.st");
    }

    /** 改写问题（配置关闭时直接返回原文） */
    public String rewrite(String question) {
        if (!ragProperties.isRewriteEnabled() || question == null || question.isBlank()) {
            return question;
        }
        try {
            String result = chatModel.call(rewriteTemplate.render(java.util.Map.of("question", question)));
            log.info("查询改写: {} → {}", question, result.trim());
            return result.trim();
        } catch (Exception e) {
            log.warn("查询改写失败，使用原文: {}", e.getMessage());
            return question;
        }
    }

    private PromptTemplate loadTemplate(String path) {
        try {
            org.springframework.core.io.Resource res = new ClassPathResource(path);
            return new PromptTemplate(res.getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("加载 {} 失败", path, e);
            return new PromptTemplate("{{question}}");
        }
    }
}
