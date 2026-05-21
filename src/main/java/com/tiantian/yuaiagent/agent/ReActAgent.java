package com.tiantian.yuaiagent.agent;

import com.tiantian.yuaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * ReAct 模式代理：AI 输出文本回答后自动结束，避免重复死循环
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    public abstract boolean think();

    public abstract String act();

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                List<Message> msgs = getMessageList();
                for (int i = msgs.size() - 1; i >= 0; i--) {
                    if (msgs.get(i) instanceof AssistantMessage am) {
                        String text = am.getText();
                        if (text != null && !text.isBlank()) {
                            // AI 输出文本回答后直接结束
                            setState(AgentState.FINISHED);
                            return text;
                        }
                    }
                }
                setState(AgentState.FINISHED);
                return "任务完成";
            }
            return act();
        } catch (Exception e) {
            e.printStackTrace();
            return "步骤执行失败：" + e.getMessage();
        }
    }
}
