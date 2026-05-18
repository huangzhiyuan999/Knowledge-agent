package com.tiantian.yuaiagent.eval.model;

/**
 * 单条评估结果
 */
public class EvalResult {
    // === 召回层 ===
    private boolean hit;         // 是否命中相关 chunk
    private int hitRank;         // 命中位置（1-based，0=未命中）
    private double mrr;          // 该条 MRR 贡献

    // === 生成层（RAGAS） ===
    private double contextRelevance;   // 上下文相关性 1-5
    private double factualAccuracy;    // 事实准确性 1-5
    private double completeness;       // 回答完整性 1-5
    private String ragasReason;        // RAGAS 理由

    // === 原始数据 ===
    private String questionId;
    private String question;
    private String expectedAnswer;
    private String actualAnswer;
    private int retrievedCount;

    // getter / setter
    public boolean isHit() { return hit; }
    public void setHit(boolean hit) { this.hit = hit; }
    public int getHitRank() { return hitRank; }
    public void setHitRank(int hitRank) { this.hitRank = hitRank; }
    public double getMrr() { return mrr; }
    public void setMrr(double mrr) { this.mrr = mrr; }
    public double getContextRelevance() { return contextRelevance; }
    public void setContextRelevance(double contextRelevance) { this.contextRelevance = contextRelevance; }
    public double getFactualAccuracy() { return factualAccuracy; }
    public void setFactualAccuracy(double factualAccuracy) { this.factualAccuracy = factualAccuracy; }
    public double getCompleteness() { return completeness; }
    public void setCompleteness(double completeness) { this.completeness = completeness; }
    public String getRagasReason() { return ragasReason; }
    public void setRagasReason(String ragasReason) { this.ragasReason = ragasReason; }
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public String getActualAnswer() { return actualAnswer; }
    public void setActualAnswer(String actualAnswer) { this.actualAnswer = actualAnswer; }
    public int getRetrievedCount() { return retrievedCount; }
    public void setRetrievedCount(int retrievedCount) { this.retrievedCount = retrievedCount; }
}
