package com.tiantian.yuaiagent.eval.metrics;

import com.tiantian.yuaiagent.eval.model.EvalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hit 命中率：评估召回层是否能找到相关文档
 * 统计多少比例的测试问题至少命中一个相关 chunk
 */
@Slf4j
@Component
public class HitRateCalculator {

    /** 计算整体 Hit Rate */
    public double calculate(List<EvalResult> results) {
        if (results.isEmpty()) return 0;
        long hits = results.stream().filter(EvalResult::isHit).count();
        double rate = (double) hits / results.size();
        log.info("Hit Rate: {}/{} = {:.2%}", hits, results.size(), rate);
        return rate;
    }
}
