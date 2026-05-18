package com.tiantian.yuaiagent.eval.metrics;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tiantian.yuaiagent.eval.model.EvalResult;
import com.tiantian.yuaiagent.eval.model.TestQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RAGAS 评估：用 LLM 从上下文相关性、事实准确性、回答完整性三个维度评分
 */
@Slf4j
@Component
public class RAGASEvaluator {

    private final ChatModel chatModel;
    private final PromptTemplate ragasTemplate;

    public RAGASEvaluator(ChatModel dashscopeChatModel) {
        this.chatModel = dashscopeChatModel;
        this.ragasTemplate = loadTemplate("prompts/eval-ragas.st");
    }

    /** 评估单条回答 */
    public void evaluate(EvalResult result, TestQuestion question, String knowledge, String answer) {
        result.setQuestionId(question.getId());
        result.setQuestion(question.getQuestion());
        result.setExpectedAnswer(question.getExpectedAnswer());
        result.setActualAnswer(answer);

        if (answer == null || answer.isBlank()) {
            result.setContextRelevance(1.0);
            result.setFactualAccuracy(1.0);
            result.setCompleteness(1.0);
            result.setRagasReason("AI 未返回回答");
            return;
        }

        try {
            String response = chatModel.call(ragasTemplate.render(java.util.Map.of(
                    "knowledge", knowledge,
                    "question", question.getQuestion(),
                    "answer", answer
            )));
            JSONObject json = JSONUtil.parseObj(response);
            result.setContextRelevance(json.getDouble("context_relevance", 0.0));
            result.setFactualAccuracy(json.getDouble("factual_accuracy", 0.0));
            result.setCompleteness(json.getDouble("completeness", 0.0));
            result.setRagasReason(json.getStr("reason", ""));
        } catch (Exception e) {
            log.warn("RAGAS 评估失败 [{}]: {}", question.getId(), e.getMessage());
            result.setContextRelevance(0.0);
            result.setFactualAccuracy(0.0);
            result.setCompleteness(0.0);
        }
    }

    private PromptTemplate loadTemplate(String path) {
        try {
            return new PromptTemplate(new ClassPathResource(path)
                    .getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("加载 {} 失败", path, e);
            return new PromptTemplate("{}");
        }
    }
}
