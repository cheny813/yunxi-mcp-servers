package io.yunxi.mcp.docker.tool;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Docker 工具集合
 * <p>
 * 提供 Docker 容器和镜像操作的 MCP 工具实现，包括列出容器、列出镜像、拉取镜像、启动容器和停止容器。
 * 使用 Docker Java Client 进行操作。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link ListContainersTool} - 列出容器</li>
 * <li>{@link ListImagesTool} - 列出镜像</li>
 * <li>{@link PullImageTool} - 拉取镜像</li>
 * <li>{@link StartContainerTool} - 启动容器</li>
 * <li>{@link StopContainerTool} - 停止容器</li>
 * </ul>
 */
public class DockerTools {

    /**
     * Docker 客户端
     * <p>
     * 用于执行 Docker 操作的客户端实例。
     * </p>
     */
    private final DockerClient dockerClient;

    /**
     * 构造函数
     *
     * @param dockerClient Docker 客户端
     */
    public DockerTools(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    /**
     * 列出容器
     * <p>
     * 工具名称: {@code list_containers}
     * </p>
     * <p>
     * 返回 Docker 容器的列表，包括容器 ID、镜像、状态和名称等信息。
     * </p>
     */
    public static class ListContainersTool implements ToolHandler {
        /**
         * Docker 客户端
         */
        private final DockerClient dockerClient;

        /**
         * 构造函数
         *
         * @param dockerClient Docker 客户端
         */
        public ListContainersTool(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_containers")
                    .description(
                            "List Docker containers. " +
                                    "列出Docker容器。 " +
                                    "Use this when you need to check running containers, verify deployment, or manage container lifecycle. "
                                    +
                                    "适用于检查运行中的容器、验证部署或管理容器生命周期等场景。 " +
                                    "Common use cases: deployment verification, container monitoring, resource management. "
                                    +
                                    "典型用例：部署验证、容器监控、资源管理。")
                    .inputSchema(schema("all", "boolean", "Show all containers (default: false). | 显示所有容器（默认: false）。"))
                    .build();
        }

        /**
         * 执行列出容器操作
         *
         * @param args 参数 Map
         * @return 工具执行结果，包含容器列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            boolean all = args.containsKey("all") && (Boolean) args.get("all");
            try {
                ListContainersCmd cmd = dockerClient.listContainersCmd().withShowAll(all);
                List<Container> containers = cmd.exec();

                StringBuilder sb = new StringBuilder();
                sb.append("Containers (").append(containers.size()).append("):\n\n");
                for (Container c : containers) {
                    sb.append("ID: ").append(c.getId().substring(0, 12)).append("\n");
                    sb.append("Image: ").append(c.getImage()).append("\n");
                    sb.append("Status: ").append(c.getStatus()).append("\n");
                    sb.append("Names: ").append(String.join(", ", c.getNames())).append("\n\n");
                }
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 列出镜像
     * <p>
     * 工具名称: {@code list_images}
     * </p>
     * <p>
     * 返回 Docker 镜像的列表，包括镜像 ID、标签和大小等信息。
     * </p>
     */
    public static class ListImagesTool implements ToolHandler {
        /**
         * Docker 客户端
         */
        private final DockerClient dockerClient;

        /**
         * 构造函数
         *
         * @param dockerClient Docker 客户端
         */
        public ListImagesTool(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_images")
                    .description(
                            "List Docker images. " +
                                    "列出Docker镜像。 " +
                                    "Use this when you need to check available images, verify image sizes, or manage local images. "
                                    +
                                    "适用于检查可用镜像、验证镜像大小或管理本地镜像等场景。 " +
                                    "Common use cases: image management, size optimization, deployment preparation. " +
                                    "典型用例：镜像管理、大小优化、部署准备。")
                    .inputSchema(emptySchema())
                    .build();
        }

        /**
         * 执行列出镜像操作
         *
         * @param args 参数 Map
         * @return 工具执行结果，包含镜像列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            try {
                List<Image> images = dockerClient.listImagesCmd().exec();

                StringBuilder sb = new StringBuilder();
                sb.append("Images (").append(images.size()).append("):\n\n");
                for (Image img : images) {
                    sb.append("ID: ").append(img.getId().replace("sha256:", "").substring(0, 12)).append("\n");
                    String[] tags = img.getRepoTags();
                    sb.append("Tags: ").append(tags != null ? String.join(", ", tags) : "<none>").append("\n");
                    sb.append("Size: ").append(img.getSize() / 1024 / 1024).append(" MB\n\n");
                }
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 拉取镜像
     * <p>
     * 工具名称: {@code pull_image}
     * </p>
     * <p>
     * 从 Docker Hub 拉取指定的镜像。
     * </p>
     */
    public static class PullImageTool implements ToolHandler {
        /**
         * Docker 客户端
         */
        private final DockerClient dockerClient;

        /**
         * 构造函数
         *
         * @param dockerClient Docker 客户端
         */
        public PullImageTool(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("pull_image")
                    .description(
                            "Pull a Docker image from registry. " +
                                    "从仓库拉取Docker镜像。 " +
                                    "Use this when you need to download images for deployment, update local images, or prepare environment. "
                                    +
                                    "适用于下载部署镜像、更新本地镜像或准备环境等场景。 " +
                                    "Common use cases: deployment preparation, environment setup, image updates. " +
                                    "典型用例：部署准备、环境设置、镜像更新。")
                    .inputSchema(schema("image", "string",
                            "Image name (e.g., nginx:latest, redis:7). | 镜像名称。示例: 'nginx:latest', 'redis:7'"))
                    .build();
        }

        /**
         * 执行拉取镜像操作
         *
         * @param args 参数 Map
         * @return 工具执行结果
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String image = (String) args.get("image");
            try {
                PullImageCmd cmd = dockerClient.pullImageCmd(image);
                cmd.exec(new PullImageResultCallback()).awaitCompletion();
                return ToolResult.text("Pulled: " + image);
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 启动容器
     * <p>
     * 工具名称: {@code start_container}
     * </p>
     * <p>
     * 启动指定 ID 或名称的 Docker 容器。
     * </p>
     */
    public static class StartContainerTool implements ToolHandler {
        /**
         * Docker 客户端
         */
        private final DockerClient dockerClient;

        /**
         * 构造函数
         *
         * @param dockerClient Docker 客户端
         */
        public StartContainerTool(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("start_container")
                    .description(
                            "Start a Docker container. " +
                                    "启动Docker容器。 " +
                                    "Use this when you need to restart stopped containers, deploy services, or scale applications. "
                                    +
                                    "适用于重启停止的容器、部署服务或扩展应用等场景。 " +
                                    "Common use cases: service deployment, container restart, scaling. " +
                                    "典型用例：服务部署、容器重启、扩展。")
                    .inputSchema(schema("container_id", "string",
                            "Container ID or name. Example: 'abc123', 'my-container' | 容器ID或名称。示例: 'abc123', 'my-container'"))
                    .build();
        }

        /**
         * 执行启动容器操作
         *
         * @param args 参数 Map
         * @return 工具执行结果
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String containerId = (String) args.get("container_id");
            try {
                dockerClient.startContainerCmd(containerId).exec();
                return ToolResult.text("Started container: " + containerId);
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 停止容器
     * <p>
     * 工具名称: {@code stop_container}
     * </p>
     * <p>
     * 停止指定 ID 或名称的 Docker 容器。
     * </p>
     */
    public static class StopContainerTool implements ToolHandler {
        /**
         * Docker 客户端
         */
        private final DockerClient dockerClient;

        /**
         * 构造函数
         *
         * @param dockerClient Docker 客户端
         */
        public StopContainerTool(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("stop_container")
                    .description(
                            "Stop a Docker container. " +
                                    "停止Docker容器。 " +
                                    "Use this when you need to stop running containers, perform maintenance, or free resources. "
                                    +
                                    "适用于停止运行中的容器、执行维护或释放资源等场景。 " +
                                    "Common use cases: maintenance, resource management, graceful shutdown. " +
                                    "典型用例：维护、资源管理、优雅关闭。")
                    .inputSchema(schema("container_id", "string",
                            "Container ID or name. Example: 'abc123', 'my-container' | 容器ID或名称。示例: 'abc123', 'my-container'"))
                    .build();
        }

        /**
         * 执行停止容器操作
         *
         * @param args 参数 Map
         * @return 工具执行结果
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String containerId = (String) args.get("container_id");
            try {
                dockerClient.stopContainerCmd(containerId).exec();
                return ToolResult.text("Stopped container: " + containerId);
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 构建空的输入参数 Schema
     *
     * @return 空的参数定义 Map
     */
    private static Map<String, Object> emptySchema() {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "object");
        s.put("properties", new HashMap<>());
        return s;
    }

    /**
     * 构建输入参数 Schema（支持多个参数）
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
