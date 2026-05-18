package com.tiantian.yuaiagent.app;

import com.tiantian.yuaiagent.advisor.MyLoggerAdvisor;
import com.tiantian.yuaiagent.rag.retriever.RetrievalPipeline;
import com.tiantian.yuaiagent.service.RedisChatMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 知识库问答应用
 * 简化流程：检索知识 → 与问题一起送入 AI
 */
@Component
@Slf4j
public class KnowledgeApp {

    private final ChatClient chatClient;
    private final RetrievalPipeline retrievalPipeline;
    private final RedisChatMemoryService redisChatMemoryService;

    public KnowledgeApp(ChatModel dashscopeChatModel, RedisChatMemoryService redisChatMemoryService,
                        RetrievalPipeline retrievalPipeline) {
        this.redisChatMemoryService = redisChatMemoryService;
        this.retrievalPipeline = retrievalPipeline;
        // 加载系统提示词（已写死模板变量名，无需渲染）
        String systemPrompt = loadPrompt("prompts/assistant.st");
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
    }

    public String doChat(String message, String userId) {
        String knowledge = retrievalPipeline.execute(message).getContext();
        ChatResponse resp = chatClient.prompt()
                .user("参考知识：\n" + knowledge + "\n\n用户问题：" + message)
                .call().chatResponse();
        String content = resp.getResult().getOutput().getText();
        redisChatMemoryService.save(userId, "knowledge", message, content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String userId) {
        String knowledge = retrievalPipeline.execute(message).getContext();
        StringBuilder sb = new StringBuilder();
        return chatClient.prompt()
                .user("参考知识：\n" + knowledge + "\n\n用户问题：" + message)
                .stream().content()
                .doOnNext(sb::append)
                .doOnComplete(() -> {
                    redisChatMemoryService.save(userId, "knowledge", message, sb.toString());
                    log.info("知识库问答已保存到 Redis");
                });
    }

    private String loadPrompt(String path) {
        try {
            org.springframework.core.io.Resource res = new ClassPathResource(path);
            return res.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("加载 {} 失败", path, e);
            return "你是小知，一个知识库问答助手。";
        }
    }
}
