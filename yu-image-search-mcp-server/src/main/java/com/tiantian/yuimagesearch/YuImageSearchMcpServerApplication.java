package com.tiantian.yuimagesearch;

import com.tiantian.yuimagesearch.tools.ImageSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * MCP 图片搜索服务端
 * 使用 Pixabay API 搜索图片，通过 stdio 与客户端通信
 *
 * 编译: mvn clean package -DskipTests -f yu-image-search-mcp-server/pom.xml
 * 客户端连接配置: src/main/resources/mcp-servers.json
 */
@SpringBootApplication
public class YuImageSearchMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuImageSearchMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageSearchTool)
                .build();
    }
}
