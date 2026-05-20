package io.yunxi.mcp.github.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedIterable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub 工具集合
 */
public class GitHubTools {

    private static Map<String, Object> prop(String name, String type, String desc) {
        Map<String, Object> p = new HashMap<>();
        p.put("type", type);
        p.put("description", desc);
        return p;
    }

    private static Map<String, Object> schema(Object... props) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new java.util.ArrayList<>();
        for (int i = 0; i < props.length; i += 3) {
            String name = (String) props[i];
            String type = (String) props[i + 1];
            String desc = (String) props[i + 2];
            properties.put(name, prop(name, type, desc));
            required.add(name);
        }
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    /**
     * 列出用户仓库
     */
    public static class ListRepositoriesTool implements ToolHandler {
        private final GitHub github;

        public ListRepositoriesTool(GitHub github) {
            this.github = github;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_repositories")
                    .description(
                            "List repositories for the authenticated user. " +
                                    "列出已认证用户的仓库。 " +
                                    "Use this when you need to see your repositories, find a specific repo, or manage your projects. "
                                    +
                                    "适用于查看您的仓库、查找特定仓库或管理项目等场景。 " +
                                    "Common use cases: project discovery, repository management, code organization. " +
                                    "典型用例：项目发现、仓库管理、代码组织。")
                    .inputSchema(schema(
                            "sort", "string",
                            "Sort by: updated, created, pushed, full_name | 排序方式: updated, created, pushed, full_name",
                            "direction", "string", "Direction: asc, desc | 排序方向: asc, desc",
                            "per_page", "integer", "Results per page (default: 30) | 每页结果数（默认: 30）"))
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> arguments) {
            try {
                PagedIterable<GHRepository> repos = github.getMyself().listRepositories();

                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (GHRepository repo : repos) {
                    if (count >= 30)
                        break;
                    sb.append("- ").append(repo.getFullName())
                            .append(" (").append(repo.getVisibility().name().toLowerCase()).append(")")
                            .append("\n  ").append(repo.getDescription())
                            .append("\n  Stars: ").append(repo.getStargazersCount())
                            .append(" | Updated: ").append(repo.getUpdatedAt())
                            .append("\n\n");
                    count++;
                }

                return ToolResult.text("Repositories (showing " + count + "):\n\n" + sb);
            } catch (IOException e) {
                return ToolResult.error("Error listing repositories: " + e.getMessage());
            }
        }
    }

    /**
     * 获取仓库信息
     */
    public static class GetRepositoryTool implements ToolHandler {
        private final GitHub github;

        public GetRepositoryTool(GitHub github) {
            this.github = github;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("get_repository")
                    .description(
                            "Get information about a specific repository. " +
                                    "获取特定仓库的信息。 " +
                                    "Use this when you need to check repo details, view stats, or verify settings. " +
                                    "适用于检查仓库详情、查看统计信息或验证设置等场景。 " +
                                    "Common use cases: repo inspection, stats review, access verification. " +
                                    "典型用例：仓库检查、统计审查、访问验证。")
                    .inputSchema(schema(
                            "owner", "string", "Repository owner. Example: 'octocat' | 仓库所有者。示例: 'octocat'",
                            "repo", "string", "Repository name. Example: 'hello-world' | 仓库名称。示例: 'hello-world'"))
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> arguments) {
            String owner = (String) arguments.get("owner");
            String repo = (String) arguments.get("repo");

            try {
                GHRepository repository = github.getRepository(owner + "/" + repo);

                StringBuilder sb = new StringBuilder();
                sb.append("Repository: ").append(repository.getFullName()).append("\n");
                sb.append("Visibility: ").append(repository.getVisibility().name().toLowerCase()).append("\n");
                sb.append("Description: ").append(repository.getDescription()).append("\n\n");
                sb.append("Stars: ").append(repository.getStargazersCount()).append("\n");
                sb.append("Forks: ").append(repository.getForksCount()).append("\n");
                sb.append("Watchers: ").append(repository.getWatchersCount()).append("\n\n");
                sb.append("Language: ").append(repository.getLanguage()).append("\n");
                sb.append("Created: ").append(repository.getCreatedAt()).append("\n");
                sb.append("Updated: ").append(repository.getUpdatedAt()).append("\n");
                sb.append("Pushed: ").append(repository.getPushedAt()).append("\n\n");
                sb.append("URL: ").append(repository.getHtmlUrl());

                return ToolResult.text(sb.toString());
            } catch (IOException e) {
                return ToolResult.error("Error getting repository: " + e.getMessage());
            }
        }
    }

    /**
     * 列出 Issue
     */
    public static class ListIssuesTool implements ToolHandler {
        private final GitHub github;

        public ListIssuesTool(GitHub github) {
            this.github = github;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_issues")
                    .description(
                            "List issues in a repository. " +
                                    "列出仓库中的Issue。 " +
                                    "Use this when you need to check open issues, review bugs, or manage tasks. " +
                                    "适用于检查开放问题、审查错误或管理任务等场景。 " +
                                    "Common use cases: bug tracking, task management, issue review. " +
                                    "典型用例：错误跟踪、任务管理、问题审查。")
                    .inputSchema(schema(
                            "owner", "string", "Repository owner. Example: 'octocat' | 仓库所有者。示例: 'octocat'",
                            "repo", "string", "Repository name. Example: 'hello-world' | 仓库名称。示例: 'hello-world'",
                            "state", "string", "State: open, closed, all | 状态: open, closed, all",
                            "per_page", "integer", "Results per page. Example: 30 | 每页结果数。示例: 30"))
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> arguments) {
            String owner = (String) arguments.get("owner");
            String repo = (String) arguments.get("repo");

            try {
                GHRepository repository = github.getRepository(owner + "/" + repo);
                PagedIterable<GHIssue> issues = repository.listIssues(GHIssueState.OPEN);

                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (GHIssue issue : issues) {
                    if (count >= 30)
                        break;
                    sb.append("#").append(issue.getNumber())
                            .append(" [").append(issue.getState().name().toLowerCase()).append("]")
                            .append(" ").append(issue.getTitle()).append("\n");
                    sb.append("  Author: ").append(issue.getUser().getLogin())
                            .append(" | Comments: ").append(issue.getCommentsCount())
                            .append(" | Created: ").append(issue.getCreatedAt()).append("\n\n");
                    count++;
                }

                return ToolResult.text("Issues in " + owner + "/" + repo + " (showing " + count + "):\n\n" + sb);
            } catch (IOException e) {
                return ToolResult.error("Error listing issues: " + e.getMessage());
            }
        }
    }

    /**
     * 创建 Issue
     */
    public static class CreateIssueTool implements ToolHandler {
        private final GitHub github;

        public CreateIssueTool(GitHub github) {
            this.github = github;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("create_issue")
                    .description(
                            "Create a new issue in a repository. " +
                                    "在仓库中创建新的Issue。 " +
                                    "Use this when you need to report bugs, request features, or create tasks. " +
                                    "适用于报告错误、请求功能或创建任务等场景。 " +
                                    "Common use cases: bug reporting, feature requests, task creation. " +
                                    "典型用例：错误报告、功能请求、任务创建。")
                    .inputSchema(schema(
                            "owner", "string", "Repository owner. Example: 'octocat' | 仓库所有者。示例: 'octocat'",
                            "repo", "string", "Repository name. Example: 'hello-world' | 仓库名称。示例: 'hello-world'",
                            "title", "string", "Issue title. Example: 'Bug in login page' | Issue标题。示例: '登录页面有错误'",
                            "body", "string",
                            "Issue body/description. Example: 'When I click login...' | Issue正文/描述。示例: '当我点击登录时...'"))
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> arguments) {
            String owner = (String) arguments.get("owner");
            String repo = (String) arguments.get("repo");
            String title = (String) arguments.get("title");
            String body = (String) arguments.get("body");

            try {
                GHRepository repository = github.getRepository(owner + "/" + repo);
                GHIssue issue = repository.createIssue(title)
                        .body(body != null ? body : "")
                        .create();

                return ToolResult.text("Issue created successfully!\n\n" +
                        "Number: #" + issue.getNumber() + "\n" +
                        "URL: " + issue.getHtmlUrl());
            } catch (IOException e) {
                return ToolResult.error("Error creating issue: " + e.getMessage());
            }
        }
    }

    /**
     * 列出 Pull Request
     */
    public static class ListPullRequestsTool implements ToolHandler {
        private final GitHub github;

        public ListPullRequestsTool(GitHub github) {
            this.github = github;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_pull_requests")
                    .description(
                            "List pull requests in a repository. " +
                                    "列出仓库中的Pull Request。 " +
                                    "Use this when you need to review PRs, check merge status, or manage code reviews. "
                                    +
                                    "适用于审查PR、检查合并状态或管理代码审查等场景。 " +
                                    "Common use cases: code review, merge management, contribution tracking. " +
                                    "典型用例：代码审查、合并管理、贡献跟踪。")
                    .inputSchema(schema(
                            "owner", "string", "Repository owner. Example: 'octocat' | 仓库所有者。示例: 'octocat'",
                            "repo", "string", "Repository name. Example: 'hello-world' | 仓库名称。示例: 'hello-world'",
                            "state", "string", "State: open, closed, all | 状态: open, closed, all",
                            "per_page", "integer", "Results per page. Example: 30 | 每页结果数。示例: 30"))
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> arguments) {
            String owner = (String) arguments.get("owner");
            String repo = (String) arguments.get("repo");

            try {
                GHRepository repository = github.getRepository(owner + "/" + repo);
                PagedIterable<GHPullRequest> prs = repository.listPullRequests(GHIssueState.OPEN);

                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (GHPullRequest pr : prs) {
                    if (count >= 30)
                        break;
                    sb.append("#").append(pr.getNumber())
                            .append(" [").append(pr.getState().name().toLowerCase()).append("]")
                            .append(" ").append(pr.getTitle()).append("\n");
                    sb.append("  Author: ").append(pr.getUser().getLogin())
                            .append(" | Created: ").append(pr.getCreatedAt()).append("\n");
                    sb.append("  Branch: ").append(pr.getHead().getRef())
                            .append(" -> ").append(pr.getBase().getRef()).append("\n\n");
                    count++;
                }

                return ToolResult.text("Pull Requests in " + owner + "/" + repo + " (showing " + count + "):\n\n" + sb);
            } catch (IOException e) {
                return ToolResult.error("Error listing pull requests: " + e.getMessage());
            }
        }
    }
}
