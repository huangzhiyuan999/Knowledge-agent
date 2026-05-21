package com.tiantian.yuaiagent.controller;

import com.tiantian.yuaiagent.agent.YuManus;
import com.tiantian.yuaiagent.annotation.RequireAuth;
import com.tiantian.yuaiagent.app.KnowledgeApp;
import com.tiantian.yuaiagent.service.RedisChatMemoryService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
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

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

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

    /** 超级智能体（无历史加载，每次独立处理） */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        // 合并内置工具和 MCP 工具
        ToolCallback[] mcpTools = toolCallbackProvider.getToolCallbacks();
        ToolCallback[] combined = new ToolCallback[allTools.length + mcpTools.length];
        System.arraycopy(allTools, 0, combined, 0, allTools.length);
        System.arraycopy(mcpTools, 0, combined, allTools.length, mcpTools.length);

        YuManus yuManus = new YuManus(combined, dashscopeChatModel);
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
