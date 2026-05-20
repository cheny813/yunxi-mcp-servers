package io.yunxi.mcp.knowledge.service;

import io.yunxi.mcp.knowledge.service.KnowledgeVectorService.KnowledgeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 代码知识提取服务
 * 从代码仓库中提取架构、业务逻辑、数据库结构等知识
 *
 * @author yunxi-mcp-servers
 */
@Service
public class KnowledgeExtractionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeExtractionService.class);

    // 代码文件扩展名
    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".xml", ".properties", ".yml", ".yaml", ".sql");

    // 需要跳过的目录
    private static final Set<String> SKIP_DIRS = Set.of(
            "target", "build", ".git", "node_modules", "dist", "vendor");

    private final KnowledgeVectorService knowledgeVectorService;

    public KnowledgeExtractionService(KnowledgeVectorService knowledgeVectorService) {
        this.knowledgeVectorService = knowledgeVectorService;
    }

    /**
     * 从代码目录提取知识并入库
     *
     * @param codePath   代码目录路径
     * @param districtId 区县ID
     * @return 提取结果
     */
    public ExtractionResult extractFromCode(String codePath, String districtId) {
        Path basePath = Paths.get(codePath);

        if (!Files.exists(basePath)) {
            return new ExtractionResult(false, "代码路径不存在: " + codePath, 0, 0);
        }

        int extractedCount = 0;
        int addedCount = 0;

        try {
            // 1. 提取架构信息
            extractedCount += extractArchitecture(basePath, districtId);

            // 2. 提取业务逻辑
            extractedCount += extractBusinessLogic(basePath, districtId);

            // 3. 提取数据库结构
            extractedCount += extractDatabaseSchema(basePath, districtId);

            log.info("知识提取完成: 共提取 {} 条知识", extractedCount);

            return new ExtractionResult(true,
                    String.format("成功提取 %d 条知识，已添加到知识库", extractedCount),
                    extractedCount, addedCount);

        } catch (Exception e) {
            log.error("知识提取失败", e);
            return new ExtractionResult(false, "提取失败: " + e.getMessage(), 0, 0);
        }
    }

    /**
     * 提取架构信息
     *
     * @param basePath   基础路径
     * @param districtId 区县ID
     * @return 提取的知识条目数量
     * @throws IOException IO异常
     */
    private int extractArchitecture(Path basePath, String districtId) throws IOException {
        int count = 0;

        // 提取项目结构
        StringBuilder structure = new StringBuilder();
        structure.append("项目结构:\n");

        Files.walk(basePath, 3)
                .filter(Files::isDirectory)
                .filter(p -> !SKIP_DIRS.contains(p.getFileName().toString()))
                .limit(20)
                .forEach(p -> {
                    String relative = basePath.relativize(p).toString();
                    if (!relative.isEmpty()) {
                        structure.append("- ").append(relative).append("\n");
                    }
                });

        addKnowledge(districtId, "架构信息", structure.toString(), "architecture",
                "项目结构,模块划分");

        count++;

        // 提取 pom.xml / package.json 信息
        List<Path> configFiles = Files.walk(basePath, 2)
                .filter(p -> p.getFileName().toString().matches("pom\\.xml|package\\.json"))
                .limit(10)
                .collect(Collectors.toList());

        for (Path config : configFiles) {
            String content = Files.readString(config);
            String type = config.getFileName().toString().equals("pom.xml")
                    ? "Maven依赖"
                    : "NPM依赖";

            addKnowledge(districtId, config.getFileName().toString(),
                    truncate(content, 2000), "architecture", type);
            count++;
        }

        return count;
    }

    /**
     * 提取业务逻辑
     *
     * @param basePath   基础路径
     * @param districtId 区县ID
     * @return 提取的知识条目数量
     * @throws IOException IO异常
     */
    private int extractBusinessLogic(Path basePath, String districtId) throws IOException {
        int count = 0;

        // 查找 Service/Controller 类
        List<Path> serviceFiles = Files.walk(basePath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith("Service.java"))
                .limit(20)
                .collect(Collectors.toList());

        for (Path file : serviceFiles) {
            try {
                String content = Files.readString(file);

                // 提取类名和方法
                String className = extractClassName(content);
                List<String> methods = extractMethods(content);

                String summary = String.format("Service: %s\n方法: %s",
                        className, String.join(", ", methods));

                addKnowledge(districtId, className, summary, "business_logic", "Service层");
                count++;
            } catch (Exception e) {
                log.warn("提取 Service 失败: {}", file, e);
            }
        }

        // 查找 Controller 类
        List<Path> controllerFiles = Files.walk(basePath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith("Controller.java"))
                .limit(10)
                .collect(Collectors.toList());

        for (Path file : controllerFiles) {
            try {
                String content = Files.readString(file);
                String className = extractClassName(content);
                List<String> apis = extractApiEndpoints(content);

                String summary = String.format("Controller: %s\nAPI接口: %s",
                        className, String.join(", ", apis));

                addKnowledge(districtId, className, summary, "business_logic", "Controller,API");
                count++;
            } catch (Exception e) {
                log.warn("提取 Controller 失败: {}", file, e);
            }
        }

        return count;
    }

    /**
     * 提取数据库结构
     */
    private int extractDatabaseSchema(Path basePath, String districtId) throws IOException {
        int count = 0;

        // 查找 SQL 文件
        List<Path> sqlFiles = Files.walk(basePath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".sql"))
                .limit(10)
                .collect(Collectors.toList());

        for (Path file : sqlFiles) {
            try {
                String content = Files.readString(file);

                // 提取表名
                List<String> tables = extractTableNames(content);

                String summary = "SQL文件: " + file.getFileName() + "\n表: "
                        + String.join(", ", tables);

                addKnowledge(districtId, file.getFileName().toString(),
                        truncate(content, 1500), "database_schema", "SQL脚本,表结构");
                count++;
            } catch (Exception e) {
                log.warn("提取 SQL 失败: {}", file, e);
            }
        }

        // 查找 Entity 类
        List<Path> entityFiles = Files.walk(basePath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith("Entity.java")
                        || p.toString().endsWith("PO.java"))
                .limit(15)
                .collect(Collectors.toList());

        for (Path file : entityFiles) {
            try {
                String content = Files.readString(file);
                String className = extractClassName(content);
                List<String> fields = extractFields(content);

                String summary = String.format("Entity: %s\n字段: %s",
                        className, String.join(", ", fields));

                addKnowledge(districtId, className, summary, "database_schema", "实体类,字段");
                count++;
            } catch (Exception e) {
                log.warn("提取 Entity 失败: {}", file, e);
            }
        }

        return count;
    }

    /**
     * 添加知识到向量库
     */
    private void addKnowledge(String districtId, String title, String content,
            String type, String tags) {
        try {
            KnowledgeItem item = new KnowledgeItem();
            item.setId(UUID.randomUUID().toString());
            item.setDistrictId(districtId);
            item.setTitle(title);
            item.setContent(content);
            item.setType(type);
            item.setTags(tags);

            knowledgeVectorService.addKnowledge(item);
        } catch (Exception e) {
            log.error("添加知识失败: {}", title, e);
        }
    }

    private String extractClassName(String content) {
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        var matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "Unknown";
    }

    private List<String> extractMethods(String content) {
        Pattern pattern = Pattern.compile("(public|private|protected)\\s+\\w+\\s+(\\w+)\\s*\\(");
        List<String> methods = new ArrayList<>();
        var matcher = pattern.matcher(content);
        while (matcher.find() && methods.size() < 10) {
            methods.add(matcher.group(2));
        }
        return methods;
    }

    private List<String> extractApiEndpoints(String content) {
        Pattern pattern = Pattern
                .compile("@(Get|Post|Put|Delete|DeleteMapping|PostMapping|GetMapping)\\s*\\(\"([^\"]+)\"\\)");
        List<String> apis = new ArrayList<>();
        var matcher = pattern.matcher(content);
        while (matcher.find() && apis.size() < 10) {
            apis.add(matcher.group(1) + " " + matcher.group(2));
        }
        return apis;
    }

    private List<String> extractTableNames(String content) {
        Pattern pattern = Pattern.compile("CREATE\\s+TABLE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        List<String> tables = new ArrayList<>();
        var matcher = pattern.matcher(content);
        while (matcher.find() && tables.size() < 20) {
            tables.add(matcher.group(1));
        }
        return tables;
    }

    private List<String> extractFields(String content) {
        Pattern pattern = Pattern.compile("(private|protected)\\s+(\\w+)\\s+(\\w+)");
        List<String> fields = new ArrayList<>();
        var matcher = pattern.matcher(content);
        while (matcher.find() && fields.size() < 15) {
            fields.add(matcher.group(3) + " (" + matcher.group(2) + ")");
        }
        return fields;
    }

    private String truncate(String content, int maxLen) {
        if (content == null || content.length() <= maxLen) {
            return content;
        }
        return content.substring(0, maxLen) + "...";
    }

    /**
     * 提取结果
     *
     * @param success        是否成功
     * @param message        结果消息
     * @param extractedCount 提取的知识条目数量
     * @param addedCount     添加的知识条目数量
     */
    public record ExtractionResult(boolean success, String message, int extractedCount, int addedCount) {
    }
}