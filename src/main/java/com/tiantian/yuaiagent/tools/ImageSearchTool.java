package com.tiantian.yuaiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 图片搜索工具（Pixabay API）
 */
@Component
public class ImageSearchTool {

    @Value("${pixabay.api-key}")
    private String apiKey;

    @Tool(description = "Search images from Pixabay, returns image URLs and descriptions")
    public String searchImage(
            @ToolParam(description = "Search keyword") String query,
            @ToolParam(description = "Number of results, default 5") Integer perPage) {
        if (perPage == null || perPage < 1) perPage = 5;
        if (perPage > 20) perPage = 20;
        try {
            String url = String.format(
                    "https://pixabay.com/api/?key=%s&q=%s&image_type=photo&per_page=%d&safesearch=true",
                    apiKey, java.net.URLEncoder.encode(query, "UTF-8"), perPage);
            JSONObject json = JSONUtil.parseObj(HttpUtil.get(url));
            var hits = json.getJSONArray("hits");
            if (hits == null || hits.isEmpty()) return "未找到相关图片";

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hits.size(); i++) {
                JSONObject hit = hits.getJSONObject(i);
                sb.append(i + 1).append(". ").append(hit.getStr("tags", "")).append("\n");
                sb.append("   地址: ").append(hit.getStr("webformatURL", "")).append("\n\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "图片搜索失败: " + e.getMessage();
        }
    }
}
