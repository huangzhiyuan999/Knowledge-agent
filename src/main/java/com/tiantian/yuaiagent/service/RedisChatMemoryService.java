package com.tiantian.yuaiagent.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis ZSet 的对话记忆服务
 * - Key: user:chat:context:{userId}:{scene}
 * - scene: love(恋爱大师) / agent(超级智能体)
 */
@Service
public class RedisChatMemoryService {

    private static final String KEY_PREFIX = "user:chat:context:";
    private static final long EXPIRY_SECONDS = 86400;
    private static final int MAX_MESSAGES = 5;
    private static final int SEND_MESSAGE_COUNT = 2;
    private static final int MAX_CONTENT_LENGTH = 200;

    private final StringRedisTemplate redisTemplate;

    public RedisChatMemoryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(String userId, String scene) {
        return KEY_PREFIX + userId + ":" + scene;
    }

    /** 获取最近对话记录 */
    public List<ChatRecord> getRecent(String userId, String scene) {
        String key = buildKey(userId, scene);
        Set<String> members = redisTemplate.opsForZSet()
                .reverseRange(key, 0, SEND_MESSAGE_COUNT - 1);
        if (members == null || members.isEmpty()) return List.of();

        List<ChatRecord> records = new ArrayList<>(members.size());
        for (String json : members) {
            records.add(JSONUtil.toBean(json, ChatRecord.class));
        }
        return records.reversed();
    }

    /** 保存一问一答 */
    public void save(String userId, String scene, String userMsg, String aiMsg) {
        String key = buildKey(userId, scene);
        long now = System.currentTimeMillis();

        if (userMsg.length() > MAX_CONTENT_LENGTH) userMsg = userMsg.substring(0, MAX_CONTENT_LENGTH);
        if (aiMsg.length() > MAX_CONTENT_LENGTH) aiMsg = aiMsg.substring(0, MAX_CONTENT_LENGTH);

        redisTemplate.opsForZSet().add(key, toJson("user", userMsg, now), now);
        redisTemplate.opsForZSet().add(key, toJson("assistant", aiMsg, now + 1), now + 1);

        Long size = redisTemplate.opsForZSet().zCard(key);
        if (size != null && size > MAX_MESSAGES) {
            long removeCount = size - MAX_MESSAGES;
            Set<String> oldest = redisTemplate.opsForZSet().range(key, 0, removeCount - 1);
            if (oldest != null && !oldest.isEmpty()) {
                redisTemplate.opsForZSet().remove(key, oldest.toArray());
            }
        }
        redisTemplate.expire(key, EXPIRY_SECONDS, TimeUnit.SECONDS);
    }

    /** 清除指定场景对话 */
    public void clear(String userId, String scene) {
        redisTemplate.delete(buildKey(userId, scene));
    }

    private String toJson(String role, String content, long time) {
        JSONObject obj = new JSONObject();
        obj.set("role", role);
        obj.set("content", content);
        obj.set("createTime", time);
        return obj.toString();
    }

    public static class ChatRecord {
        private String role;
        private String content;
        private long createTime;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }
    }
}
