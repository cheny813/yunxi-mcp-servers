package io.yunxi.mcp.k8s.tool;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Kubernetes 工具集合
 * <p>
 * 提供 Kubernetes 资源操作的 MCP 工具实现，包括列出 Pod、Service 和 Namespace。
 * 使用 Kubernetes Java Client 进行操作。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>{@link ListPodsTool} - 获取 Pod 列表</li>
 * <li>{@link ListServicesTool} - 获取服务列表</li>
 * <li>{@link ListNamespacesTool} - 获取命名空间列表</li>
 * </ul>
 */
public class K8sTools {

    /**
     * Kubernetes Core V1 API 客户端
     */
    private final CoreV1Api api;

    /**
     * 构造函数
     *
     * @param client Kubernetes API 客户端
     */
    public K8sTools(ApiClient client) {
        this.api = new CoreV1Api(client);
    }

    /**
     * 获取 Pod 列表
     * <p>
     * 工具名称: {@code list_pods}
     * </p>
     * <p>
     * 返回指定命名空间中的所有 Pod 及其状态信息。
     * </p>
     */
    public static class ListPodsTool implements ToolHandler {
        /**
         * Kubernetes API 客户端
         */
        private final CoreV1Api api;

        /**
         * 构造函数
         *
         * @param api Kubernetes API 客户端
         */
        public ListPodsTool(CoreV1Api api) {
            this.api = api;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_pods")
                    .description("List pods in a namespace")
                    .inputSchema(schema("namespace", "string", "Namespace (default: default)"))
                    .build();
        }

        /**
         * 执行获取 Pod 列表操作
         *
         * @param args 参数 Map
         * @return 工具执行结果，包含 Pod 列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String namespace = (String) args.getOrDefault("namespace", "default");
            try {
                V1PodList podList = api.listNamespacedPod(namespace).execute();
                StringBuilder sb = new StringBuilder();
                sb.append("Pods in ").append(namespace).append(" (").append(podList.getItems().size()).append("):\n\n");

                for (V1Pod pod : podList.getItems()) {
                    sb.append("- ").append(pod.getMetadata().getName()).append("\n");
                    sb.append("  Status: ").append(pod.getStatus().getPhase()).append("\n");
                    sb.append("  Node: ").append(pod.getSpec().getNodeName()).append("\n\n");
                }
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 获取服务列表
     * <p>
     * 工具名称: {@code list_services}
     * </p>
     * <p>
     * 返回指定命名空间中的所有 Service 及其配置信息。
     * </p>
     */
    public static class ListServicesTool implements ToolHandler {
        /**
         * Kubernetes API 客户端
         */
        private final CoreV1Api api;

        /**
         * 构造函数
         *
         * @param api Kubernetes API 客户端
         */
        public ListServicesTool(CoreV1Api api) {
            this.api = api;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_services")
                    .description("List services in a namespace")
                    .inputSchema(schema("namespace", "string", "Namespace (default: default)"))
                    .build();
        }

        /**
         * 执行获取服务列表操作
         *
         * @param args 参数 Map
         * @return 工具执行结果，包含服务列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            String namespace = (String) args.getOrDefault("namespace", "default");
            try {
                V1ServiceList svcList = api.listNamespacedService(namespace).execute();
                StringBuilder sb = new StringBuilder();
                sb.append("Services in ").append(namespace).append(" (").append(svcList.getItems().size())
                        .append("):\n\n");

                for (V1Service svc : svcList.getItems()) {
                    sb.append("- ").append(svc.getMetadata().getName()).append("\n");
                    sb.append("  Type: ").append(svc.getSpec().getType()).append("\n");
                    sb.append("  Cluster IP: ").append(svc.getSpec().getClusterIP()).append("\n\n");
                }
                return ToolResult.text(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 获取命名空间列表
     * <p>
     * 工具名称: {@code list_namespaces}
     * </p>
     * <p>
     * 返回集群中的所有命名空间。
     * </p>
     */
    public static class ListNamespacesTool implements ToolHandler {
        /**
         * Kubernetes API 客户端
         */
        private final CoreV1Api api;

        /**
         * 构造函数
         *
         * @param api Kubernetes API 客户端
         */
        public ListNamespacesTool(CoreV1Api api) {
            this.api = api;
        }

        /**
         * 获取工具定义
         *
         * @return 工具定义对象
         */
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("list_namespaces")
                    .description("List all namespaces")
                    .inputSchema(emptySchema())
                    .build();
        }

        /**
         * 执行获取命名空间列表操作
         *
         * @param args 参数 Map
         * @return 工具执行结果，包含命名空间列表
         */
        @Override
        public ToolResult execute(Map<String, Object> args) {
            try {
                V1NamespaceList nsList = api.listNamespace().execute();
                StringBuilder sb = new StringBuilder();
                sb.append("Namespaces (").append(nsList.getItems().size()).append("):\n\n");

                for (V1Namespace ns : nsList.getItems()) {
                    sb.append("- ").append(ns.getMetadata().getName()).append("\n");
                }
                return ToolResult.text(sb.toString());
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
