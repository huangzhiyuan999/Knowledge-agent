package com.tiantian.yuaiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    public abstract boolean think();

    public abstract String act();

    /**
     * 执行单个步骤：思考和行动
     * 当 think() 返回 false（无需工具）时，返回 LLM 实际回答而非固定占位文字
     */
    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                // 从消息列表中取出 LLM 的文本回答
                List<Message> msgs = getMessageList();
                for (int i = msgs.size() - 1; i >= 0; i--) {
                    if (msgs.get(i) instanceof AssistantMessage am) {
                        String text = am.getText();
                        if (text != null && !text.isBlank()) {
                            return text;
                        }
                    }
                }
                return "思考完成 - 无需行动";
            }
            return act();
        } catch (Exception e) {
            e.printStackTrace();
            return "步骤执行失败：" + e.getMessage();
        }
    }
}
