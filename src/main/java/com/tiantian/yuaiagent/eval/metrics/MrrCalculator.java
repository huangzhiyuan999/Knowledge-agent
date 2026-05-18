package com.tiantian.yuaiagent.eval.metrics;

import com.tiantian.yuaiagent.eval.model.EvalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MRR（Mean Reciprocal Rank）：平均倒数排名
 * 衡量相关 chunk 在检索结果中的排位
 */
@Slf4j
@Component
public class MrrCalculator {

    /** 计算整体 MRR */
    public double calculate(List<EvalResult> results) {
        if (results.isEmpty()) return 0;
        double sum = results.stream().mapToDouble(EvalResult::getMrr).sum();
        double mrr = sum / results.size();
        log.info("MRR: {:.3f}", mrr);
        return mrr;
    }
}
