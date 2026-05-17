package com.tiantian.yuaiagent.agent;

import com.tiantian.yuaiagent.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 天天的 AI 超级智能体（拥有自主规划能力，可以直接使用）
 */
@Slf4j
@Component
public class YuManus extends ToolCallAgent {

    public YuManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("yuManus");
        // 从模板加载系统提示词
        String systemPrompt = loadTemplate("prompts/agent-system.st", Map.of(
                "name", "YuManus",
                "language", "中文",
                "tools", "文件操作、联网搜索、网页抓取、终端操作、资源下载、PDF生成"
        ));
        this.setSystemPrompt(systemPrompt);
        // 从模板加载下一步提示词
        String nextStepPrompt = loadTemplate("prompts/agent-next-step.st", Map.of());
        this.setNextStepPrompt(nextStepPrompt);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

    /**
     * 从 classpath 加载模板并渲染
     */
    private String loadTemplate(String path, Map<String, Object> variables) {
        try {
            org.springframework.core.io.Resource res = new ClassPathResource(path);
            String content = res.getContentAsString(StandardCharsets.UTF_8);
            PromptTemplate template = new PromptTemplate(content);
            return template.render(variables);
        } catch (IOException e) {
            log.warn("加载 {} 失败，使用默认提示词", path, e);
            return "";
        }
    }
}
