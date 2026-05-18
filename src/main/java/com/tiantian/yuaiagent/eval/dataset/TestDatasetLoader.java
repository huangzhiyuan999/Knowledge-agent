package com.tiantian.yuaiagent.eval.dataset;

import cn.hutool.json.JSONUtil;
import com.tiantian.yuaiagent.eval.model.TestQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 测试数据集加载器：读取 testdata/test-questions.json
 */
@Slf4j
@Component
public class TestDatasetLoader {

    private List<TestQuestion> questions;

    /** 加载测试数据集 */
    public List<TestQuestion> load() {
        if (questions != null) return questions;
        try {
            String json = new String(
                    new ClassPathResource("testdata/test-questions.json")
                            .getContentAsByteArray(), StandardCharsets.UTF_8);
            questions = JSONUtil.toList(json, TestQuestion.class);
            log.info("加载测试数据集完成，共 {} 条", questions.size());
        } catch (IOException e) {
            log.error("加载测试数据集失败", e);
            questions = List.of();
        }
        return questions;
    }
}
