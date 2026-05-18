package com.tiantian.yuaiagent.app;

import com.tiantian.yuaiagent.advisor.MyLoggerAdvisor;
import com.tiantian.yuaiagent.service.RedisChatMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库问答应用
 * 基于文档检索 + AI 的多轮对话问答
 */
@Component
@Slf4j
public class KnowledgeApp {

    private final ChatClient chatClient;
    private final String baseSystemPrompt;
    private final RedisChatMemoryService redisChatMemoryService;

    @Resource
    private VectorStore loveAppVectorStore;

    public KnowledgeApp(ChatModel dashscopeChatModel, RedisChatMemoryService redisChatMemoryService) {
        this.redisChatMemoryService = redisChatMemoryService;
        // 从模板加载系统提示词
        String basePrompt;
        try {
            org.springframework.core.io.Resource res = new ClassPathResource("prompts/assistant.st");
            basePrompt = res.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("加载 prompts/assistant.st 失败，使用默认提示词", e);
            basePrompt = "你是{{name}}，一个知识库问答助手。";
        }
        PromptTemplate template = new PromptTemplate(basePrompt);
        baseSystemPrompt = template.render(Map.of(
                "name", "小知",
                "language", "中文"
        ));
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(baseSystemPrompt)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
    }

    /** 知识库问答（同步，自动检索 + Redis 记忆） */
    public String doChat(String message, String userId) {
        // 加载历史
        List<RedisChatMemoryService.ChatRecord> history = redisChatMemoryService.getRecent(userId, "knowledge");
        List<Message> messages = new ArrayList<>();
        for (var record : history) {
            messages.add("user".equals(record.getRole())
                    ? new UserMessage(record.getContent())
                    : new AssistantMessage(record.getContent()));
        }

        ChatResponse chatResponse = chatClient
                .prompt()
                .messages(messages)
                .user(message)
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);

        redisChatMemoryService.save(userId, "knowledge", message, content);
        return content;
    }

    /** 知识库问答（SSE 流式，自动检索 + Redis 记忆） */
    public Flux<String> doChatByStream(String message, String userId) {
        List<RedisChatMemoryService.ChatRecord> history = redisChatMemoryService.getRecent(userId, "knowledge");
        List<Message> messages = new ArrayList<>();
        for (var record : history) {
            messages.add("user".equals(record.getRole())
                    ? new UserMessage(record.getContent())
                    : new AssistantMessage(record.getContent()));
        }

        Flux<String> contentFlux = chatClient
                .prompt()
                .messages(messages)
                .user(message)
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .stream()
                .content();

        StringBuilder sb = new StringBuilder();
        return contentFlux
                .doOnNext(sb::append)
                .doOnComplete(() -> {
                    redisChatMemoryService.save(userId, "knowledge", message, sb.toString());
                    log.info("知识库问答已保存到 Redis");
                });
    }
}
