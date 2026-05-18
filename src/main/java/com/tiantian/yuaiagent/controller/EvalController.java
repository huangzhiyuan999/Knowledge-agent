package com.tiantian.yuaiagent.controller;

import com.tiantian.yuaiagent.annotation.RequireAuth;
import com.tiantian.yuaiagent.eval.experiment.ExperimentRunner;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 评估接口：触发 RAG 全链路批量评测
 */
@RestController
@RequestMapping("/eval")
public class EvalController {

    private final ExperimentRunner experimentRunner;

    public EvalController(ExperimentRunner experimentRunner) {
        this.experimentRunner = experimentRunner;
    }

    @PostMapping("/run")
    @RequireAuth
    public ResponseEntity<Map<String, Object>> runEval(HttpServletRequest request) {
        try {
            String report = experimentRunner.runFullEval();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "report", report
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}
