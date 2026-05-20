package io.yunxi.mcp.common.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.common.constants.McpErrorCode;
import io.yunxi.mcp.common.model.ToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 通用协议和工具定义测试
 */
public class McpProtocolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldSerializeMcpErrorWithTraceId() throws Exception {
        McpResponse response = McpResponse.error(1, McpErrorCode.INVALID_PARAMS, "Missing params", "trace-123");

        String json = objectMapper.writeValueAsString(response);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("2.0", node.get("jsonrpc").asText());
        assertEquals(1, node.get("id").asInt());
        assertTrue(node.has("error"));
        assertEquals(McpErrorCode.INVALID_PARAMS, node.get("error").get("code").asInt());
        assertEquals("Missing params", node.get("error").get("message").asText());
        assertEquals("trace-123", node.get("error").get("traceId").asText());
    }

    @Test
    public void shouldCreateToolDefinitionWithOutputSchemaAndExamples() {
        ToolDefinition definition = ToolDefinition.simpleText(
                "echo_text",
                "Echo input text back",
                "text",
                "The text to echo back"
        );

        assertNotNull(definition.getInputSchema());
        assertNotNull(definition.getOutputSchema());
        assertNotNull(definition.getExamples());
        assertEquals("string", definition.getOutputSchema().get("type"));
        assertTrue(definition.getExamples().stream().anyMatch(example -> example.containsKey("output")));
    }
}
