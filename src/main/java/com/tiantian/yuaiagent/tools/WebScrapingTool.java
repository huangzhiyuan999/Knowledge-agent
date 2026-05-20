package com.tiantian.yuaiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;

/**
 * 网页抓取工具（提取页面文本 + 图片链接）
 */
public class WebScrapingTool {

    @Tool(description = "Scrape the content and image URLs from a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            StringBuilder result = new StringBuilder();

            // 提取页面标题
            result.append("标题: ").append(document.title()).append("\n\n");

            // 提取正文文本（限前 500 字）
            String bodyText = document.body().text();
            if (bodyText.length() > 500) bodyText = bodyText.substring(0, 500);
            result.append("正文:\n").append(bodyText).append("\n\n");

            // 提取图片链接
            Elements imgs = document.select("img[src]");
            List<String> imageUrls = new ArrayList<>();
            for (Element img : imgs) {
                String src = img.attr("abs:src");
                if (src != null && !src.isEmpty()
                        && (src.contains(".jpg") || src.contains(".png") || src.contains(".jpeg") || src.contains(".webp"))) {
                    imageUrls.add(src);
                }
            }
            if (!imageUrls.isEmpty()) {
                result.append("图片链接:\n");
                for (int i = 0; i < Math.min(imageUrls.size(), 10); i++) {
                    result.append(i + 1).append(". ").append(imageUrls.get(i)).append("\n");
                }
            }

            return result.toString().trim();
        } catch (Exception e) {
            return "抓取失败: " + e.getMessage();
        }
    }
}
