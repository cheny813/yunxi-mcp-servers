package io.yunxi.mcp.git.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Git 工具集合
 * <p>
 * 提供 Git 仓库操作的 MCP 工具实现，包括获取状态、提交日志和分支列表。
 * 使用 JGit 库进行 Git 操作。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link StatusTool} - 获取 Git 仓库状态</li>
 * <li>{@link LogTool} - 获取 Git 提交日志</li>
 * <li>{@link BranchesTool} - 获取分支列表</li>
 * </ul>
 */
public class GitTools {

    /**
     * 获取仓库状态
     * <p>
     * 工具名称: {@code git_status}
     * </p>
     * <p>
     * 返回 Git 仓库的当前状态，包括：
     * <ul>
     * <li>当前分支名称</li>
     * <li>已修改文件列表</li>
     * <li>未跟踪文件列表</li>
     * </ul>
     * </p>
     */
    public static class StatusTool implements ToolHandler {
        /**
         * Git 仓库路径
         * <p>
         * 要操作的 Git 仓库目录路径。
         * </p>
         */
        private final File repoPath;

        /**
         * 构造函数
         *
         * @param repoPath Git 仓库路径
         */
        public StatusTool(String repoPath) {
            this.repoPath = new File(repoPath);
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("git_status")
                    .description(
                            "Get Git repository status. " +
                                    "获取Git仓库状态。 " +
                                    "Use this when you need to check current branch, modified files, or untracked files. "
                                    +
                                    "适用于检查当前分支、已修改文件或未跟踪文件等场景。 " +
                                    "Common use cases: checking repo status, reviewing changes, preparing for commit. "
                                    +
                                    "典型用例：检查仓库状态、审查更改、准备提交。")
                    .inputSchema(Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "repo_path", Map.of(
                                            "type", "string",
                                            "description",
                                            "Path to the Git repository. Example: '/path/to/repo' | Git仓库路径。示例: '/path/to/repo'"))))
                    .build();
        }

        /**
         * 执行获取仓库状态操作
         * <p>
         * 打开指定仓库，获取当前分支、已修改文件列表和未跟踪文件列表。
         * </p>
         *
         * @param args 参数 Map，支持以下字段：
         *             <ul>
         *             <li>repo_path (可选) - 仓库路径，默认为构造时指定的路径</li>
         *             </ul>
         * @return 工具执行结果，包含仓库状态信息
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            // 获取仓库路径，优先使用参数中的路径
            String repoPath = (String) args.getOrDefault("repo_path", this.repoPath.getPath());
            try (Git git = Git.open(new File(repoPath))) {
                StringBuilder sb = new StringBuilder();
                sb.append("Repository: ").append(repoPath).append("\n");
                sb.append("Branch: ").append(git.getRepository().getBranch()).append("\n");

                // 获取并输出修改的文件列表
                var status = git.status().call();
                sb.append("\nModified files: ").append(status.getModified().size()).append("\n");
                for (String f : status.getModified()) {
                    sb.append("  M ").append(f).append("\n");
                }

                // 输出未跟踪文件列表
                sb.append("\nUntracked files: ").append(status.getUntracked().size()).append("\n");
                for (String f : status.getUntracked()) {
                    sb.append("  ? ").append(f).append("\n");
                }

                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }

        /**
         * 构建输入参数 Schema
         *
         * @param name 参数名称
         * @param type 参数类型
         * @param desc 参数描述
         * @return 参数定义 Map
         */
        private Map<String, Object> schema(String name, String type, String desc) {
            Map<String, Object> s = new HashMap<>();
            s.put("type", type);
            s.put("description", desc);
            return s;
        }
    }

    /**
     * 获取提交日志
     * <p>
     * 工具名称: {@code git_log}
     * </p>
     * <p>
     * 返回 Git 仓库的提交历史记录，包括：
     * <ul>
     * <li>提交哈希（短哈希）</li>
     * <li>提交者信息</li>
     * <li>提交时间</li>
     * <li>提交消息</li>
     * </ul>
     * </p>
     */
    public static class LogTool implements ToolHandler {
        /**
         * Git 仓库路径
         */
        private final File repoPath;

        /**
         * 构造函数
         *
         * @param repoPath Git 仓库路径
         */
        public LogTool(String repoPath) {
            this.repoPath = new File(repoPath);
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("git_log")
                    .description(
                            "Get Git commit log. " +
                                    "获取Git提交日志。 " +
                                    "Use this when you need to view commit history, check recent changes, or find specific commits. "
                                    +
                                    "适用于查看提交历史、检查最近更改或查找特定提交等场景。 " +
                                    "Common use cases: reviewing history, finding bugs, understanding changes. " +
                                    "典型用例：查看历史、查找问题、理解变更。")
                    .inputSchema(Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "repo_path", Map.of(
                                            "type", "string",
                                            "description",
                                            "Path to the Git repository. Example: '/path/to/repo' | Git仓库路径。示例: '/path/to/repo'"),
                                    "max_count", Map.of(
                                            "type", "integer",
                                            "description",
                                            "Maximum number of commits to show (default: 10). Example: 20 | 显示的最大提交数（默认: 10）。示例: 20"))))
                    .build();
        }

        /**
         * 执行获取提交日志操作
         * <p>
         * 打开指定仓库，获取最近的提交历史记录。
         * </p>
         *
         * @param args 参数 Map，支持以下字段：
         *             <ul>
         *             <li>repo_path (可选) - 仓库路径</li>
         *             <li>max_count (可选) - 最大显示的提交数，默认 10</li>
         *             </ul>
         * @return 工具执行结果，包含提交日志信息
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            // 获取仓库路径，优先使用参数中的路径
            String repoPath = (String) args.getOrDefault("repo_path", this.repoPath.getPath());
            // 获取最大提交数，默认 10
            int maxCount = args.containsKey("max_count") ? ((Number) args.get("max_count")).intValue() : 10;

            try (Git git = Git.open(new File(repoPath))) {
                StringBuilder sb = new StringBuilder();
                // 获取提交日志
                Iterable<RevCommit> commits = git.log().setMaxCount(maxCount).call();

                int count = 0;
                for (RevCommit commit : commits) {
                    // 输出提交哈希（取前7位）
                    sb.append("commit ").append(commit.getName().substring(0, 7)).append("\n");
                    // 输出提交者信息
                    sb.append("Author: ").append(commit.getAuthorIdent().getName())
                            .append(" <").append(commit.getAuthorIdent().getEmailAddress()).append(">\n");
                    // 输出提交时间
                    sb.append("Date: ").append(commit.getAuthorIdent().getWhen()).append("\n");
                    // 输出提交消息
                    sb.append("\n    ").append(commit.getFullMessage()).append("\n\n");
                    count++;
                }

                return ToolResult.text("Showing " + count + " commit(s):\n\n" + sb);
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }

        /**
         * 构建输入参数 Schema（支持多个参数）
         *
         * @param props 可变参数，每3个为一组：名称、类型、描述
         * @return 参数定义 Map
         */
        private Map<String, Object> schema(String... props) {
            Map<String, Object> s = new HashMap<>();
            for (int i = 0; i < props.length; i += 3) {
                Map<String, Object> p = new HashMap<>();
                p.put("type", props[i + 1]);
                p.put("description", props[i + 2]);
                s.put(props[i], p);
            }
            return s;
        }
    }

    /**
     * 获取分支列表
     * <p>
     * 工具名称: {@code git_branches}
     * </p>
     * <p>
     * 返回 Git 仓库的所有本地分支，并在当前分支后标注 (current)。
     * </p>
     */
    public static class BranchesTool implements ToolHandler {
        /**
         * Git 仓库路径
         */
        private final File repoPath;

        /**
         * 构造函数
         *
         * @param repoPath Git 仓库路径
         */
        public BranchesTool(String repoPath) {
            this.repoPath = new File(repoPath);
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("git_branches")
                    .description(
                            "List Git branches. " +
                                    "列出Git分支。 " +
                                    "Use this when you need to see all branches, check current branch, or plan branch operations. "
                                    +
                                    "适用于查看所有分支、检查当前分支或规划分支操作等场景。 " +
                                    "Common use cases: branch management, release planning, feature development. " +
                                    "典型用例：分支管理、发布规划、功能开发。")
                    .inputSchema(Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "repo_path", Map.of(
                                            "type", "string",
                                            "description",
                                            "Path to the Git repository. Example: '/path/to/repo' | Git仓库路径。示例: '/path/to/repo'"))))
                    .build();
        }

        /**
         * 执行获取分支列表操作
         * <p>
         * 打开指定仓库，获取所有本地分支列表。
         * </p>
         *
         * @param args 参数 Map，支持以下字段：
         *             <ul>
         *             <li>repo_path (可选) - 仓库路径</li>
         *             </ul>
         * @return 工具执行结果，包含分支列表信息
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String repoPath = (String) args.getOrDefault("repo_path", this.repoPath.getPath());

            try (Git git = Git.open(new File(repoPath))) {
                StringBuilder sb = new StringBuilder();
                sb.append("Current branch: ").append(git.getRepository().getBranch()).append("\n\n");

                sb.append("Local branches:\n");
                // 列出所有本地分支
                for (Ref ref : git.branchList().call()) {
                    // 获取分支名称（去掉 refs/heads/ 前缀）
                    sb.append("  ").append(ref.getName().replace("refs/heads/", ""));
                    // 如果是当前分支，标注 (current)
                    if (ref.getName().equals("refs/heads/" + git.getRepository().getBranch())) {
                        sb.append(" (current)");
                    }
                    sb.append("\n");
                }

                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }

        /**
         * 构建输入参数 Schema
         *
         * @param name 参数名称
         * @param type 参数类型
         * @param desc 参数描述
         * @return 参数定义 Map
         */
        private Map<String, Object> schema(String name, String type, String desc) {
            Map<String, Object> s = new HashMap<>();
            s.put("type", type);
            s.put("description", desc);
            return s;
        }
    }
}
