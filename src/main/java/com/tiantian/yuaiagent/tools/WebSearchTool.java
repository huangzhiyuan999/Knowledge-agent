package com.tiantian.yuaiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                return "未找到相关结果";
            }
            int count = Math.min(organicResults.size(), 5);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                JSONObject item = (JSONObject) organicResults.get(i);
                sb.append(i + 1).append(". ").append(item.getStr("title", "")).append("\n");
                sb.append("   链接: ").append(item.getStr("link", "")).append("\n");
                sb.append("   摘要: ").append(item.getStr("snippet", "")).append("\n\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "搜索失败: " + e.getMessage();
        }
    }
}
