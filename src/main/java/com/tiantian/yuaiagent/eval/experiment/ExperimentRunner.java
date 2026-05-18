package com.tiantian.yuaiagent.eval.experiment;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.tiantian.yuaiagent.eval.dataset.TestDatasetLoader;
import com.tiantian.yuaiagent.eval.metrics.HitRateCalculator;
import com.tiantian.yuaiagent.eval.metrics.MrrCalculator;
import com.tiantian.yuaiagent.eval.metrics.RAGASEvaluator;
import com.tiantian.yuaiagent.eval.model.EvalResult;
import com.tiantian.yuaiagent.eval.model.TestQuestion;
import com.tiantian.yuaiagent.rag.loader.ChunkEngine;
import com.tiantian.yuaiagent.rag.retriever.RetrievalPipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量测评引擎：加载测试集 → 逐条检索+评估 → 汇总指标 → 输出报告
 */
@Slf4j
@Component
public class ExperimentRunner {

    private final TestDatasetLoader datasetLoader;
    private final RetrievalPipeline retrievalPipeline;
    private final HitRateCalculator hitRateCalculator;
    private final MrrCalculator mrrCalculator;
    private final RAGASEvaluator ragasEvaluator;

    public ExperimentRunner(TestDatasetLoader datasetLoader, RetrievalPipeline retrievalPipeline,
                            HitRateCalculator hitRateCalculator, MrrCalculator mrrCalculator,
                            RAGASEvaluator ragasEvaluator) {
        this.datasetLoader = datasetLoader;
        this.retrievalPipeline = retrievalPipeline;
        this.hitRateCalculator = hitRateCalculator;
        this.mrrCalculator = mrrCalculator;
        this.ragasEvaluator = ragasEvaluator;
    }

    /** 运行完整评测 */
    public String runFullEval() {
        List<TestQuestion> questions = datasetLoader.load();
        List<EvalResult> results = new ArrayList<>();
        int total = questions.size();

        for (int i = 0; i < total; i++) {
            TestQuestion q = questions.get(i);
            log.info("[{}/{}] 评测: {}", i + 1, total, q.getId());

            RetrievalPipeline.RetrievalResult retrieved = retrievalPipeline.execute(q.getQuestion());
            EvalResult result = evaluateRetrieval(q, retrieved);
            ragasEvaluator.evaluate(result, q, retrieved.getContext(), "");
            result.setRetrievedCount(retrieved.getChunks().size());
            results.add(result);
        }

        JSONObject report = buildReport(results, questions.size());
        saveReport(report);
        return report.toStringPretty();
    }

    /** 召回层评估 */
    private EvalResult evaluateRetrieval(TestQuestion question, RetrievalPipeline.RetrievalResult retrieved) {
        EvalResult result = new EvalResult();
        result.setQuestionId(question.getId());
        result.setQuestion(question.getQuestion());

        List<ChunkEngine.ChunkRecord> chunks = retrieved.getChunks();
        for (int rank = 0; rank < chunks.size(); rank++) {
            String text = chunks.get(rank).getText();
            for (String keyword : question.getRelevantChunks()) {
                if (text.contains(keyword)) {
                    result.setHit(true);
                    result.setHitRank(rank + 1);
                    result.setMrr(1.0 / (rank + 1));
                    return result;
                }
            }
        }
        result.setHit(false);
        result.setHitRank(0);
        result.setMrr(0);
        return result;
    }

    private JSONObject buildReport(List<EvalResult> results, int total) {
        JSONObject report = new JSONObject();
        report.set("eval_date", java.time.LocalDate.now().toString());
        report.set("total_questions", total);

        double hitRate = hitRateCalculator.calculate(results);
        double mrr = mrrCalculator.calculate(results);
        JSONObject recall = new JSONObject();
        recall.set("hit_rate", hitRate);
        recall.set("mrr", mrr);
        report.set("recall_metrics", recall);

        double avgContext = results.stream().mapToDouble(EvalResult::getContextRelevance).average().orElse(0);
        double avgFactual = results.stream().mapToDouble(EvalResult::getFactualAccuracy).average().orElse(0);
        double avgComplete = results.stream().mapToDouble(EvalResult::getCompleteness).average().orElse(0);
        JSONObject generation = new JSONObject();
        generation.set("context_relevance_avg", avgContext);
        generation.set("factual_accuracy_avg", avgFactual);
        generation.set("completeness_avg", avgComplete);
        report.set("generation_metrics", generation);

        JSONArray details = new JSONArray();
        for (EvalResult r : results) {
            JSONObject d = new JSONObject();
            d.set("id", r.getQuestionId());
            d.set("hit", r.isHit());
            d.set("hit_rank", r.getHitRank());
            d.set("mrr", r.getMrr());
            d.set("retrieved_count", r.getRetrievedCount());
            d.set("context_relevance", r.getContextRelevance());
            d.set("factual_accuracy", r.getFactualAccuracy());
            d.set("completeness", r.getCompleteness());
            details.add(d);
        }
        report.set("details", details);
        return report;
    }

    private void saveReport(JSONObject report) {
        try {
            String path = "src/main/resources/testdata/evaluation-report.json";
            Files.writeString(Paths.get(path), report.toStringPretty());
            log.info("评估报告已保存到 {}", path);
        } catch (IOException e) {
            log.error("保存评估报告失败", e);
        }
    }
}
