package io.yunxi.mcp.wikipedia.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Wikipedia 工具集合
 * <p>
 * 提供 Wikipedia 文章搜索和获取摘要的 MCP 工具实现。
 * 使用 Wikipedia MediaWiki API 进行数据查询。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link SearchTool} - 搜索 Wikipedia 文章</li>
 * <li>{@link SummaryTool} - 获取 Wikipedia 文章摘要</li>
 * </ul>
 *
 * <h3>API 文档</h3>
 * <p>
 * Wikipedia MediaWiki API: https://en.wikipedia.org/w/api.php
 * </p>
 */
public class WikipediaTools {

    /**
     * Wikipedia API 基础 URL
     * <p>
     * 使用英文 Wikipedia 的 MediaWiki API。
     * </p>
     */
    private static final String API_URL = "https://en.wikipedia.org/w/api.php";

    /**
     * 搜索 Wikipedia 文章
     * <p>
     * 工具名称: {@code wikipedia_search}
     * </p>
     * <p>
     * 在 Wikipedia 上搜索指定关键词的文章，返回匹配的文章列表。
     * </p>
     */
    public static class SearchTool implements ToolHandler {

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("search")
                    .description(
                            "Search Wikipedia articles. " +
                                    "搜索维基百科文章。 " +
                                    "Use this when you need to find information, research topics, or get general knowledge. "
                                    +
                                    "适用于查找信息、研究主题或获取通用知识等场景。 " +
                                    "Common use cases: research, fact-checking, general knowledge. " +
                                    "典型用例：研究、事实核查、通用知识。")
                    .inputSchema(schema(
                            "query", "string", "Search query. Example: 'artificial intelligence' | 搜索关键词。示例: '人工智能'",
                            "limit", "integer", "Maximum results (default: 5). Example: 10 | 最大结果数（默认: 5）。示例: 10"))
                    .build();
        }

        /**
         * 执行搜索 Wikipedia 操作
         * <p>
         * 调用 Wikipedia MediaWiki API 的搜索接口，返回匹配的文章列表。
         * </p>
         *
         * @param args 参数 Map：
         *             <ul>
         *             <li>query (必填) - 搜索关键词</li>
         *             <li>limit (可选) - 返回结果数量限制，默认 5</li>
         *             </ul>
         * @return 工具执行结果，包含搜索到的文章列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            // 获取搜索参数
            String query = (String) args.get("query");
            // 获取返回数量限制
            int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 5;

            try {
                // 构建搜索 API URL
                String urlStr = API_URL + "?action=query&list=search&srsearch="
                        + URLEncoder.encode(query, "UTF-8")
                        + "&srlimit=" + limit + "&format=json";

                // 获取 API 响应
                String response = fetch(urlStr);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);
                JsonNode searchResults = root.path("query").path("search");

                // 格式化输出搜索结果
                StringBuilder sb = new StringBuilder();
                sb.append("Search results for \"").append(query).append("\" (").append(searchResults.size())
                        .append("):\n\n");

                for (JsonNode result : searchResults) {
                    String title = result.get("title").asText();
                    int pageId = result.get("pageid").asInt();
                    sb.append("- ").append(title).append("\n");
                    sb.append("  URL: https://en.wikipedia.org/wiki/").append(URLEncoder.encode(title, "UTF-8"))
                            .append("\n\n");
                }
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 获取 Wikipedia 文章摘要
     * <p>
     * 工具名称: {@code wikipedia_summary}
     * </p>
     * <p>
     * 获取指定 Wikipedia 文章的开头摘要部分。
     * </p>
     */
    public static class SummaryTool implements ToolHandler {

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("summary")
                    .description(
                            "Get Wikipedia article summary. " +
                                    "获取维基百科文章摘要。 " +
                                    "Use this when you need a quick overview of a topic, extract key points, or get concise information. "
                                    +
                                    "适用于快速了解主题、提取要点或获取简洁信息等场景。 " +
                                    "Common use cases: quick reference, topic overview, information extraction. " +
                                    "典型用例：快速参考、主题概览、信息提取。")
                    .inputSchema(
                            schema("title", "string", "Article title. Example: 'Machine learning' | 文章标题。示例: '机器学习'"))
                    .build();
        }

        /**
         * 执行获取文章摘要操作
         * <p>
         * 调用 Wikipedia MediaWiki API 获取指定文章的摘要内容。
         * </p>
         *
         * @param args 参数 Map：
         *             <ul>
         *             <li>title (必填) - 文章标题</li>
         *             </ul>
         * @return 工具执行结果，包含文章摘要内容
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String title = (String) args.get("title");

            try {
                // 构建摘要 API URL
                String urlStr = API_URL + "?action=query&prop=extracts&exintro=true&explaintext=true&titles="
                        + URLEncoder.encode(title, "UTF-8") + "&format=json";

                // 获取 API 响应
                String response = fetch(urlStr);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);
                JsonNode pages = root.path("query").path("pages");
                JsonNode page = pages.elements().next();

                // 提取文章摘要
                String extract = page.path("extract").asText("No content found");
                StringBuilder sb = new StringBuilder();
                sb.append("# ").append(page.get("title").asText()).append("\n\n");
                sb.append(extract);
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 发起 HTTP GET 请求并获取响应
     * <p>
     * 同步 HTTP 请求方法，用于调用 Wikipedia API。
     * </p>
     *
     * @param urlStr 请求的 URL
     * @return 响应内容字符串
     * @throws Exception 如果请求失败
     */
    private static String fetch(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // 读取响应内容
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * 构建输入参数 Schema
     *
     * @param props 可变参数，每3个为一组：名称、类型、描述
     * @return 参数定义 Map
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
