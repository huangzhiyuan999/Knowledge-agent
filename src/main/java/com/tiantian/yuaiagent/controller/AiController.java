package com.tiantian.yuaiagent.controller;

import com.tiantian.yuaiagent.agent.YuManus;
import com.tiantian.yuaiagent.annotation.RequireAuth;
import com.tiantian.yuaiagent.app.KnowledgeApp;
import com.tiantian.yuaiagent.service.RedisChatMemoryService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private KnowledgeApp knowledgeApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private RedisChatMemoryService redisChatMemoryService;

    @GetMapping("/love_app/chat/sync")
    @RequireAuth
    public String doChatWithLoveAppSync(String message, HttpServletRequest request) {
        return knowledgeApp.doChat(message, (String) request.getAttribute("userId"));
    }

    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, HttpServletRequest request) {
        return knowledgeApp.doChatByStream(message, (String) request.getAttribute("userId"));
    }

    @GetMapping(value = "/love_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return knowledgeApp.doChatByStream(message, userId)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

    @GetMapping(value = "/love_app/chat/sse_emitter")
    public SseEmitter doChatWithLoveAppServerSseEmitter(String message, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        SseEmitter sseEmitter = new SseEmitter(180000L);
        knowledgeApp.doChatByStream(message, userId)
                .subscribe(chunk -> {
                    try { sseEmitter.send(chunk); }
                    catch (IOException e) { sseEmitter.completeWithError(e); }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        YuManus yuManus = new YuManus(allTools, dashscopeChatModel);

        // 加载历史对话
        for (var record : redisChatMemoryService.getRecent(userId, "agent")) {
            if ("user".equals(record.getRole())) {
                yuManus.getMessageList().add(new org.springframework.ai.chat.messages.UserMessage(record.getContent()));
            } else {
                yuManus.getMessageList().add(new org.springframework.ai.chat.messages.AssistantMessage(record.getContent()));
            }
        }
        // 前置保存，确保用户消息不丢失
        redisChatMemoryService.save(userId, "agent", message, "执行中");

        SseEmitter emitter = yuManus.runStream(message);
        emitter.onCompletion(() ->
                redisChatMemoryService.save(userId, "agent", message, "执行完成"));
        return emitter;
    }

    @PostMapping("/context/clear")
    @RequireAuth
    public ResponseEntity<Map<String, Object>> clearContext(String scene, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (scene == null || scene.isBlank()) scene = "love";
        redisChatMemoryService.clear(userId, scene);
        return ResponseEntity.ok(Map.of("message", "对话历史已清除"));
    }
}
