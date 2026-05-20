package io.yunxi.mcp.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP Tool 定义单元测试
 *
 * <p>
 * 验证 ToolDefinition 的构建、字段设置和便捷方法。
 * </p>
 */
class ToolDefinitionTest {

    @Nested
    @DisplayName("Builder 构建测试")
    class BuilderTests {

        @Test
        @DisplayName("应使用 Builder 正确构建 ToolDefinition")
        void shouldBuildWithBuilder() {
            ToolDefinition definition = ToolDefinition.builder()
                    .name("read_file")
                    .description("Read a file from the filesystem")
                    .inputSchema(Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "path", Map.of("type", "string", "description", "File path")
                            ),
                            "required", List.of("path")
                    ))
                    .outputSchema(Map.of("type", "string"))
                    .returnType("string")
                    .build();

            assertNotNull(definition);
            assertEquals("read_file", definition.getName());
            assertEquals("Read a file from the filesystem", definition.getDescription());
            assertNotNull(definition.getInputSchema());
            assertEquals("string", definition.getReturnType());
        }

        @Test
        @DisplayName("应支持最小化构建")
        void shouldSupportMinimalBuild() {
            ToolDefinition definition = ToolDefinition.builder()
                    .name("simple_tool")
                    .build();

            assertNotNull(definition);
            assertEquals("simple_tool", definition.getName());
            assertNull(definition.getDescription());
        }
    }

    @Nested
    @DisplayName("simpleText 便捷方法测试")
    class SimpleTextTests {

        @Test
        @DisplayName("应创建简单文本工具定义")
        void shouldCreateSimpleTextTool() {
            ToolDefinition definition = ToolDefinition.simpleText(
                    "echo",
                    "Echo the input text",
                    "text",
                    "The text to echo"
            );

            assertNotNull(definition);
            assertEquals("echo", definition.getName());
            assertEquals("Echo the input text", definition.getDescription());
            assertNotNull(definition.getInputSchema());
            assertNotNull(definition.getOutputSchema());
            assertNotNull(definition.getParameters());
            assertNotNull(definition.getExamples());
            assertEquals("string", definition.getReturnType());
        }

        @Test
        @DisplayName("simpleText 应正确设置 inputSchema")
        void simpleTextShouldSetInputSchema() {
            ToolDefinition definition = ToolDefinition.simpleText(
                    "query",
                    "Execute a query",
                    "sql",
                    "The SQL query to execute"
            );

            Map<String, Object> schema = definition.getInputSchema();
            assertEquals("object", schema.get("type"));
            assertTrue(schema.containsKey("properties"));
            assertTrue(schema.containsKey("required"));

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            assertTrue(properties.containsKey("sql"));
        }

        @Test
        @DisplayName("simpleText 应正确设置 outputSchema")
        void simpleTextShouldSetOutputSchema() {
            ToolDefinition definition = ToolDefinition.simpleText(
                    "query",
                    "Execute a query",
                    "sql",
                    "The SQL query"
            );

            Map<String, Object> schema = definition.getOutputSchema();
            assertEquals("string", schema.get("type"));
            assertTrue(schema.containsKey("description"));
        }

        @Test
        @DisplayName("simpleText 应正确设置 parameters")
        void simpleTextShouldSetParameters() {
            ToolDefinition definition = ToolDefinition.simpleText(
                    "query",
                    "Execute a query",
                    "sql",
                    "The SQL query to execute"
            );

            List<ToolParameter> params = definition.getParameters();
            assertEquals(1, params.size());

            ToolParameter param = params.get(0);
            assertEquals("sql", param.getName());
            assertEquals("The SQL query to execute", param.getDescription());
            assertEquals("string", param.getType());
            assertTrue(param.isRequired());
            assertEquals("example value", param.getExample());
        }

        @Test
        @DisplayName("simpleText 应正确设置 examples")
        void simpleTextShouldSetExamples() {
            ToolDefinition definition = ToolDefinition.simpleText(
                    "query",
                    "Execute a query",
                    "sql",
                    "The SQL query"
            );

            List<Map<String, Object>> examples = definition.getExamples();
            assertEquals(1, examples.size());

            Map<String, Object> example = examples.get(0);
            assertTrue(example.containsKey("input"));
            assertTrue(example.containsKey("output"));
        }
    }

    @Nested
    @DisplayName("Getter/Setter 测试")
    class GetterSetterTests {

        @Test
        @DisplayName("应正确设置和获取所有字段")
        void shouldSetAndGetAllFields() {
            ToolDefinition definition = new ToolDefinition();

            definition.setName("test_tool");
            definition.setDescription("Test tool description");
            definition.setInputSchema(Map.of("type", "object"));
            definition.setOutputSchema(Map.of("type", "string"));
            definition.setParameters(List.of(ToolParameter.builder().name("param1").build()));
            definition.setReturnType("string");
            definition.setMetadata(ToolMetadata.builder().version("1.0.0").build());
            definition.setExamples(List.of(Map.of("input", "test")));

            assertEquals("test_tool", definition.getName());
            assertEquals("Test tool description", definition.getDescription());
            assertNotNull(definition.getInputSchema());
            assertNotNull(definition.getOutputSchema());
            assertNotNull(definition.getParameters());
            assertEquals("string", definition.getReturnType());
            assertNotNull(definition.getMetadata());
            assertNotNull(definition.getExamples());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空字符串名称应被接受")
        void emptyNameShouldBeAccepted() {
            ToolDefinition definition = ToolDefinition.builder()
                    .name("")
                    .build();

            assertEquals("", definition.getName());
        }

        @Test
        @DisplayName("null 值应被接受")
        void nullValuesShouldBeAccepted() {
            ToolDefinition definition = ToolDefinition.builder()
                    .name("test")
                    .description(null)
                    .inputSchema(null)
                    .build();

            assertEquals("test", definition.getName());
            assertNull(definition.getDescription());
            assertNull(definition.getInputSchema());
        }

        @Test
        @DisplayName("复杂嵌套 schema 应正确处理")
        void complexNestedSchemaShouldBeHandled() {
            Map<String, Object> nestedSchema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "user", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "name", Map.of("type", "string"),
                                            "age", Map.of("type", "integer")
                                    )
                            ),
                            "tags", Map.of(
                                    "type", "array",
                                    "items", Map.of("type", "string")
                            )
                    ),
                    "required", List.of("user")
            );

            ToolDefinition definition = ToolDefinition.builder()
                    .name("complex_tool")
                    .inputSchema(nestedSchema)
                    .build();

            assertNotNull(definition.getInputSchema());
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) definition.getInputSchema().get("properties");
            assertTrue(properties.containsKey("user"));
            assertTrue(properties.containsKey("tags"));
        }
    }
}
