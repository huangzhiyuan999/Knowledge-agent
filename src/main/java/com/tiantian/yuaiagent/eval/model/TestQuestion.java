package com.tiantian.yuaiagent.eval.model;

import java.util.List;

/**
 * 测试问题实体（对应 testdata/test-questions.json）
 */
public class TestQuestion {
    private String id;
    private String question;
    private String expectedAnswer;
    private List<String> relevantChunks;
    private String sourceDoc;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
    public List<String> getRelevantChunks() { return relevantChunks; }
    public void setRelevantChunks(List<String> relevantChunks) { this.relevantChunks = relevantChunks; }
    public String getSourceDoc() { return sourceDoc; }
    public void setSourceDoc(String sourceDoc) { this.sourceDoc = sourceDoc; }
}
