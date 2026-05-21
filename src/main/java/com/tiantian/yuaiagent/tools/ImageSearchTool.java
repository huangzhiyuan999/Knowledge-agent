package com.tiantian.yuaiagent.tools;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;

/**
 * 图片搜索工具（Pixabay API）
 * 自动将中文关键词翻译为英文再搜索
 */
@Slf4j
@Component
public class ImageSearchTool {

    @Value("${pixabay.api-key}")
    private String apiKey;

    private final ChatModel chatModel;

    public ImageSearchTool(ChatModel dashscopeChatModel) {
        this.chatModel = dashscopeChatModel;
    }

    @Tool(description = "Search images from Pixabay. Returns REAL image URLs.")
    public String searchImage(
            @ToolParam(description = "Search keyword, supports both Chinese and English") String query,
            @ToolParam(description = "Number of results (3-20), default 5") Integer perPage) {
        if (perPage == null || perPage < 3) perPage = 3;
        if (perPage > 20) perPage = 20;
        try {
            String searchQuery = translateToEnglish(query);
            String encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8");
            String apiUrl = "https://pixabay.com/api/?key=" + apiKey
                    + "&q=" + encodedQuery
                    + "&image_type=photo&per_page=" + perPage + "&safesearch=true";
            log.info("Pixabay search: {} -> {}", query, searchQuery);
            URL url = URI.create(apiUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            if (code != 200) {
                BufferedReader er = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                StringBuilder err = new StringBuilder();
                String l;
                while ((l = er.readLine()) != null) err.append(l);
                er.close();
                return "图片搜索失败: " + err;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String l;
            while ((l = r.readLine()) != null) response.append(l);
            r.close();
            JSONObject json = JSONUtil.parseObj(response.toString());
            var hits = json.getJSONArray("hits");
            if (hits == null || hits.isEmpty()) return "未找到相关图片";

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hits.size(); i++) {
                JSONObject hit = hits.getJSONObject(i);
                sb.append(i + 1).append(". ").append(hit.getStr("tags", "")).append("\n");
                sb.append("   图片地址: ").append(hit.getStr("webformatURL", "")).append("\n\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "图片搜索失败: " + e.getMessage();
        }
    }

    /** 中文→英文翻译 */
    private String translateToEnglish(String query) {
        if (query.chars().allMatch(c -> c < 128)) return query;
        try {
            PromptTemplate pt = new PromptTemplate("将以下中文翻译成英文图片搜索关键词，只输出英文关键词：{query}");
            String result = chatModel.call(pt.render(java.util.Map.of("query", query)));
            return result.trim().replaceAll("[\\r\\n]", " ");
        } catch (Exception e) {
            log.warn("翻译失败: {}", e.getMessage());
            return query;
        }
    }
}
