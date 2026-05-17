package com.tiantian.yuaiagent.app;

import com.tiantian.yuaiagent.advisor.MyLoggerAdvisor;
import com.tiantian.yuaiagent.rag.QueryRewriter;
import com.tiantian.yuaiagent.service.RedisChatMemoryService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;
    private final String baseSystemPrompt;
    private final RedisChatMemoryService redisChatMemoryService;

    public LoveApp(ChatModel dashscopeChatModel, RedisChatMemoryService redisChatMemoryService) {
        this.redisChatMemoryService = redisChatMemoryService;
        // 从模板加载系统提示词
        String basePrompt;
        try {
            org.springframework.core.io.Resource res = new ClassPathResource("prompts/assistant.st");
            basePrompt = res.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("加载 prompts/assistant.st 失败，使用默认提示词", e);
            basePrompt = "你是{{name}}，{{role}}领域的专家。";
        }
        PromptTemplate template = new PromptTemplate(basePrompt);
        baseSystemPrompt = template.render(Map.of(
                "name", "林薇",
                "role", "恋爱心理",
                "personality", "温柔耐心、善于倾听",
                "style", "共情优先，给出具体可操作的建议",
                "language", "中文",
                "userName", "用户"
        ));
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(baseSystemPrompt)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
    }

    /**
     * AI 基础对话（Redis ZSet 记忆）
     */
    public String doChat(String message, String userId) {
        List<RedisChatMemoryService.ChatRecord> history = redisChatMemoryService.getRecent(userId, "love");
        List<Message> messages = new ArrayList<>();
        for (var record : history) {
            if ("user".equals(record.getRole())) {
                messages.add(new UserMessage(record.getContent()));
            } else {
                messages.add(new AssistantMessage(record.getContent()));
            }
        }

        ChatResponse chatResponse = chatClient
                .prompt()
                .messages(messages)
                .user(message)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);

        redisChatMemoryService.save(userId, "love", message, content);
        return content;
    }

    /**
     * AI 基础对话（SSE 流式，Redis ZSet 记忆）
     */
    public Flux<String> doChatByStream(String message, String userId) {
        List<RedisChatMemoryService.ChatRecord> history = redisChatMemoryService.getRecent(userId, "love");
        List<Message> messages = new ArrayList<>();
        for (var record : history) {
            if ("user".equals(record.getRole())) {
                messages.add(new UserMessage(record.getContent()));
            } else {
                messages.add(new AssistantMessage(record.getContent()));
            }
        }

        Flux<String> contentFlux = chatClient
                .prompt()
                .messages(messages)
                .user(message)
                .stream()
                .content();

        StringBuilder sb = new StringBuilder();
        return contentFlux
                .doOnNext(sb::append)
                .doOnComplete(() -> {
                    redisChatMemoryService.save(userId, "love", message, sb.toString());
                    log.info("流式对话已保存到 Redis");
                });
    }

    record LoveReport(String title, List<String> suggestions) {

    }

    /**
     * AI 恋爱报告功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(baseSystemPrompt + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    // AI 恋爱知识库问答功能

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库服务）
//                .advisors(loveAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                loveAppVectorStore, "单身"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 恋爱报告功能（支持调用工具）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用 MCP 服务

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 恋爱报告功能（调用 MCP 服务）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
