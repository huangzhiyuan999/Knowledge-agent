package com.tiantian.yuaiagent.agent;

import com.tiantian.yuaiagent.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 超级智能体（ReAct 模式，配备多种工具）
 */
@Slf4j
@Component
public class YuManus extends ToolCallAgent {

    public YuManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("yuManus");
        this.setSystemPrompt(loadText("prompts/agent-system.st"));
        this.setNextStepPrompt(loadText("prompts/agent-next-step.st"));
        this.setMaxSteps(20);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

    private String loadText(String path) {
        try {
            return new String(new ClassPathResource(path).getContentAsByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("加载 {} 失败", path, e);
            return "";
        }
    }
}
