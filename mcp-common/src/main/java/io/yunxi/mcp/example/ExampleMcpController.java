package io.yunxi.mcp.example;

import io.yunxi.mcp.common.controller.AbstractMcpController;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolMetadata;
import io.yunxi.mcp.common.model.ToolParameter;
import io.yunxi.mcp.common.model.ToolResult;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Map;

/**
 * MCP 统一控制器使用示例
 * <p>
 * 演示如何继承 AbstractMcpController 来创建具体的 MCP 服务。
 * </p>
 */
@SpringBootApplication
public class ExampleMcpController extends AbstractMcpController {

    public static void main(String[] args) {
        SpringApplication.run(ExampleMcpController.class, args);
    }

    @Override
    protected String getServerName() {
        return "example-mcp-server";
    }

    @Override
    protected void registerTools() {
        // 注册示例工具
        registerTool(new EchoTool());
        registerTool(new CalculatorTool());
    }

    /**
     * 回声工具示例
     */
    static class EchoTool implements ToolHandler {
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("echo")
                    .description("Echo the input message back")
                    .parameters(List.of(
                            ToolParameter.builder()
                                    .name("message")
                                    .type("string")
                                    .description("The message to echo")
                                    .required(true)
                                    .build()))
                    .returnType("string")
                    .metadata(ToolMetadata.builder()
                            .category("utility")
                            .tags(List.of("simple"))
                            .build())
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> arguments) {
            String message = (String) arguments.get("message");
            return ToolResult.text("Echo: " + message);
        }
    }

    /**
     * 计算器工具示例
     */
    static class CalculatorTool implements ToolHandler {
        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("calculate")
                    .description("Perform basic arithmetic operations")
                    .parameters(List.of(
                            ToolParameter.builder()
                                    .name("operation")
                                    .type("string")
                                    .description("The operation (+, -, *, /)")
                                    .required(true)
                                    .build(),
                            ToolParameter.builder()
                                    .name("a")
                                    .type("number")
                                    .description("First number")
                                    .required(true)
                                    .build(),
                            ToolParameter.builder()
                                    .name("b")
                                    .type("number")
                                    .description("Second number")
                                    .required(true)
                                    .build()))
                    .returnType("number")
                    .metadata(ToolMetadata.builder()
                            .category("math")
                            .tags(List.of("calculator"))
                            .build())
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> arguments) {
            String operation = (String) arguments.get("operation");
            double a = ((Number) arguments.get("a")).doubleValue();
            double b = ((Number) arguments.get("b")).doubleValue();

            double result;
            switch (operation) {
                case "+" -> result = a + b;
                case "-" -> result = a - b;
                case "*" -> result = a * b;
                case "/" -> {
                    if (b == 0) {
                        return ToolResult.error("Division by zero");
                    }
                    result = a / b;
                }
                default -> {
                    return ToolResult.error("Unknown operation: " + operation);
                }
            }

            return ToolResult.text("Result: " + result);
        }
    }
}