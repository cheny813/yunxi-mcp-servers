package io.yunxi.mcp.code.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 代码搜索服务
 * <p>
 * 提供代码文件的搜索、浏览和读取功能。
 * 支持按关键词搜索代码内容，按文件类型过滤，查看目录结构和文件内容。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>关键词搜索 - 在代码文件中搜索指定关键词</li>
 * <li>文件类型过滤 - 支持多种编程语言文件类型</li>
 * <li>目录浏览 - 查看代码目录结构</li>
 * <li>文件读取 - 读取指定文件的内容</li>
 * </ul>
 *
 * <h3>支持的文件类型</h3>
 * <ul>
 * <li>Java: .java, .xml, .properties, .yml, .yaml</li>
 * <li>前端: .js, .ts, .jsx, .tsx, .vue</li>
 * <li>其他: .py, .go, .rs, .sql, .sh</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * 
 * <pre>
 * CodeSearchService service = new CodeSearchService("/path/to/code");
 * List&lt;CodeSearchResult&gt; results = service.search("UserService", "*.java", 20);
 * String content = service.readFile("src/main/java/Service.java");
 * </pre>
 *
 * @author yunxi-mcp-servers
 * @since 1.0.0
 */
@Service
public class CodeSearchService {

    private static final Logger log = LoggerFactory.getLogger(CodeSearchService.class);

    /** 代码搜索根路径 */
    private final String rootPath;

    /** 支持的代码文件扩展名 */
    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".xml", ".properties", ".yml", ".yaml",
            ".js", ".ts", ".jsx", ".tsx", ".vue",
            ".py", ".go", ".rs", ".sql", ".sh");

    /**
     * 默认构造函数
     * <p>
     * 使用系统属性 "code.search.path" 作为根路径，默认为 "./code"
     * </p>
     */
    public CodeSearchService() {
        this.rootPath = System.getProperty("code.search.path", "./code");
    }

    /**
     * 指定根路径的构造函数
     *
     * @param rootPath 代码搜索根路径
     */
    public CodeSearchService(String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * 搜索代码
     * <p>
     * 在代码文件中搜索指定关键词，支持文件类型过滤和结果数量限制。
     * 使用并行流提高搜索效率。
     * </p>
     *
     * @param keyword     搜索关键词（不区分大小写）
     * @param filePattern 文件匹配模式（可选，如 "*.java"）
     * @param maxResults  最大返回结果数
     * @return 搜索结果列表
     */
    public List<CodeSearchResult> search(String keyword, String filePattern, int maxResults) {
        Path basePath = Paths.get(rootPath);
        if (!Files.exists(basePath)) {
            log.warn("代码搜索路径不存在: {}", rootPath);
            return Collections.emptyList();
        }

        List<CodeSearchResult> results = new ArrayList<>();

        try {
            Files.walk(basePath)
                    .filter(Files::isRegularFile)
                    .filter(path -> isCodeFile(path.toString()))
                    .filter(path -> filePattern == null || path.toString().contains(filePattern))
                    .parallel()
                    .forEach(path -> {
                        try {
                            List<String> matchedLines = searchInFile(path, keyword);
                            if (!matchedLines.isEmpty()) {
                                results.add(new CodeSearchResult(
                                        path.toString(),
                                        path.getFileName().toString(),
                                        matchedLines));
                            }
                        } catch (IOException e) {
                            log.warn("搜索文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("遍历代码目录失败", e);
        }

        // 限制结果数量
        return results.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * 在文件中搜索关键词
     * <p>
     * 读取文件内容，返回包含关键词的所有行。
     * 搜索不区分大小写。
     * </p>
     *
     * @param path    文件路径
     * @param keyword 搜索关键词
     * @return 匹配的行列表
     * @throws IOException 文件读取失败时抛出
     */
    private List<String> searchInFile(Path path, String keyword) throws IOException {
        List<String> matchedLines = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(path);
            Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);

            for (String line : lines) {
                if (pattern.matcher(line).find()) {
                    matchedLines.add(line.trim());
                }
            }
        } catch (IOException e) {
            throw e;
        }

        return matchedLines;
    }

    /**
     * 判断是否为代码文件
     * <p>
     * 根据文件扩展名判断是否为支持的代码文件类型。
     * </p>
     *
     * @param fileName 文件名
     * @return 是否为代码文件
     */
    private boolean isCodeFile(String fileName) {
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    /**
     * 获取目录结构
     * <p>
     * 列出指定路径下的文件和子目录信息。
     * </p>
     *
     * @param path 相对路径（可选，默认为根路径）
     * @return 文件信息列表
     */
    public List<FileInfo> listDirectory(String path) {
        Path basePath = path != null ? Paths.get(rootPath, path) : Paths.get(rootPath);

        if (!Files.exists(basePath)) {
            return Collections.emptyList();
        }

        try {
            return Files.list(basePath)
                    .map(p -> new FileInfo(
                            p.getFileName().toString(),
                            Files.isDirectory(p),
                            p.toString()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("列出目录失败: {}", basePath, e);
            return Collections.emptyList();
        }
    }

    /**
     * 读取文件内容
     * <p>
     * 读取指定相对路径的文件内容。
     * </p>
     *
     * @param relativePath 文件相对路径
     * @return 文件内容
     * @throws IOException           文件读取失败时抛出
     * @throws FileNotFoundException 文件不存在时抛出
     */
    public String readFile(String relativePath) throws IOException {
        Path filePath = Paths.get(rootPath, relativePath);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("文件不存在: " + relativePath);
        }
        return Files.readString(filePath);
    }

    /**
     * 代码搜索结果记录
     *
     * @param filePath     文件完整路径
     * @param fileName     文件名
     * @param matchedLines 匹配的行列表
     */
    public record CodeSearchResult(String filePath, String fileName, List<String> matchedLines) {
    }

    /**
     * 文件信息记录
     *
     * @param name        文件/目录名
     * @param isDirectory 是否为目录
     * @param fullPath    完整路径
     */
    public record FileInfo(String name, boolean isDirectory, String fullPath) {
    }
}
