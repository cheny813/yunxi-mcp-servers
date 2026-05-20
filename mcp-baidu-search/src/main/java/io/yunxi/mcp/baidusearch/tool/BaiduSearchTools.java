package io.yunxi.mcp.baidusearch.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.baidusearch.BaiduSearchConfig;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 百度搜索工具
 * <p>
 * 提供百度 AI 搜索 API 的 MCP 工具实现。
 * 使用百度千帆平台的 Web Search API 进行实时信息搜索。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link SearchTool} - 百度搜索</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <p>
 * 需要配置 BAIDU_API_KEY 环境变量或在配置文件中设置 baidu-search.api-key
 * </p>
 */
@Slf4j
@Component
public class BaiduSearchTools {

    private final BaiduSearchConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public BaiduSearchTools(BaiduSearchConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 百度搜索工具
     */
    public static class SearchTool implements ToolHandler {

        private final BaiduSearchTools parent;

        @Autowired
        public SearchTool(BaiduSearchTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("baidu_search")
                    .description(
                            "Search the web using Baidu AI Search Engine. " +
                                    "使用百度 AI 搜索引擎进行网页搜索。 " +
                                    "Use this when you need live information, recent news, or real-time data. " +
                                    "适用于需要实时信息、新闻或最新数据等场景。 " +
                                    "Common use cases: news, current events, latest information. " +
                                    "典型用例：新闻、时事、最新信息。")
                    .inputSchema(schema(
                            "query", "string", "Search query. Example: '人工智能最新发展' | 搜索关键词。示例: '人工智能最新发展'",
                            "count", "integer", "Number of results (1-50, default: 10) | 返回结果数（1-50，默认: 10）",
                            "freshness", "string", "Time range: pd (past day), pw (past week), pm (past month), py (past year) | 时间范围: pd(过去24小时), pw(过去7天), pm(过去30天), py(过去365天)"))
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String query = (String) args.get("query");
            if (query == null || query.isBlank()) {
                return ToolResult.error("Error: query is required");
            }

            Integer count = args.containsKey("count") ? ((Number) args.get("count")).intValue() : null;
            String freshness = (String) args.get("freshness");

            return parent.search(query, count, freshness);
        }
    }

    /**
     * 执行百度搜索
     */
    public ToolResult search(String query, Integer count, String freshness) {
        try {
            // 获取 API Key
            String apiKey = config.getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                // 尝试从环境变量获取
                apiKey = System.getenv("BAIDU_API_KEY");
                if (apiKey == null || apiKey.isBlank()) {
                    return ToolResult.error("Error: BAIDU_API_KEY not configured");
                }
            }

            // 处理参数
            int resultCount = count != null ? count : config.getDefaultCount();
            if (resultCount < 1) resultCount = 1;
            if (resultCount > config.getMaxCount()) resultCount = config.getMaxCount();

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messages", new Object[]{
                    Map.of("content", query, "role", "user")
            });
            requestBody.put("search_source", "baidu_search_v2");
            requestBody.put("resource_type_filter", new Object[]{
                    Map.of("type", "web", "top_k", resultCount)
            });

            // 处理时间范围过滤
            if (freshness != null && !freshness.isBlank()) {
                Map<String, Object> searchFilter = buildSearchFilter(freshness);
                if (searchFilter != null) {
                    requestBody.put("search_filter", searchFilter);
                }
            }

            // 发送请求
            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEndpoint()))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-Appbuilder-From", "yunxi-mcp")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Baidu API error: status={}, body={}", response.statusCode(), response.body());
                return ToolResult.error("Error: API request failed with status " + response.statusCode());
            }

            // 解析响应
            JsonNode root = objectMapper.readTree(response.body());

            if (root.has("code")) {
                String errorMsg = root.has("message") ? root.get("message").asText() : "Unknown error";
                return ToolResult.error("Error: " + errorMsg);
            }

            // 提取结果
            JsonNode references = root.get("references");
            if (references == null || references.isEmpty()) {
                return ToolResult.text("No search results found for: " + query);
            }

            // 格式化输出
            StringBuilder sb = new StringBuilder();
            sb.append("🔍 Search results for \"").append(query).append("\" (").append(references.size()).append("):\n\n");

            int index = 1;
            for (JsonNode result : references) {
                String title = result.has("title") ? result.get("title").asText() : "Untitled";
                String url = result.has("url") ? result.get("url").asText() : "";
                String time = result.has("time") ? result.get("time").asText() : "";
                String content = result.has("content") ? result.get("content").asText() : "";

                sb.append(index).append(". ").append(title).append("\n");
                if (!time.isEmpty()) {
                    sb.append("   📅 ").append(time).append("\n");
                }
                if (!url.isEmpty()) {
                    sb.append("   🔗 ").append(url).append("\n");
                }
                if (!content.isEmpty()) {
                    sb.append("   📝 ").append(content.length() > 200 ? content.substring(0, 200) + "..." : content).append("\n");
                }
                sb.append("\n");
                index++;
            }

            return ToolResult.text(sb.toString());

        } catch (Exception e) {
            log.error("Baidu search error: {}", e.getMessage(), e);
            return ToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 构建搜索过滤条件
     */
    private Map<String, Object> buildSearchFilter(String freshness) {
        if (freshness == null || freshness.isBlank()) {
            return null;
        }

        // 预定义的时间范围
        Map<String, String> rangeMap = Map.of(
                "pd", "1",   // past day
                "pw", "7",   // past week
                "pm", "30",  // past month
                "py", "365"  // past year
        );

        if (rangeMap.containsKey(freshness)) {
            Map<String, Object> filter = new HashMap<>();
            filter.put("range", Map.of("page_time", Map.of(
                    "gte", rangeMap.get(freshness),
                    "lt", "1"
            )));
            return filter;
        }

        // 自定义日期范围格式: YYYY-MM-DDtoYYYY-MM-DD
        if (freshness.contains("to")) {
            String[] parts = freshness.split("to");
            if (parts.length == 2) {
                Map<String, Object> filter = new HashMap<>();
                filter.put("range", Map.of("page_time", Map.of(
                        "gte", parts[0].trim(),
                        "lt", parts[1].trim()
                )));
                return filter;
            }
        }

        return null;
    }

    /**
     * 构建输入参数 Schema
     */
    private static Map<String, Object> schema(String... props) {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < props.length; i += 3) {
            Map<String, Object> p = new HashMap<>();
            p.put("type", props[i + 1]);
            p.put("description", props[i + 2]);
            properties.put(props[i], p);
        }
        s.put("properties", properties);
        return s;
    }
}