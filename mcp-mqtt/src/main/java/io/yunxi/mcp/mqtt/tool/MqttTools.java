package io.yunxi.mcp.mqtt.tool;

import io.yunxi.mcp.mqtt.service.MqttService;
import io.yunxi.mcp.mqtt.service.MqttService.CachedMessage;
import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MQTT MCP 工具集
 * 
 * 提供 MQTT 实时订阅/发布功能。
 * 历史数据查询请使用 mcp-database 查询 MySQL。
 */
@Slf4j
@Component
public class MqttTools {
    
    private final MqttService mqttService;

    public MqttTools(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    /**
     * 订阅 MQTT 主题
     */
    public ToolHandler subscribe() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("mqtt_subscribe")
                        .description("Subscribe to MQTT topic to receive real-time sensor data. " +
                                "Use this to start receiving messages from sensors like temperature, humidity, etc. " +
                                "| 订阅MQTT主题以接收实时传感器数据。用于开始接收温度、湿度等传感器的消息。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "topic", Map.of(
                                                "type", "string",
                                                "description", "MQTT topic pattern to subscribe. " +
                                                        "Example: sensor/temperature/#, sensor/humidity/room1 | " +
                                                        "要订阅的MQTT主题模式。例如: sensor/temperature/#, sensor/humidity/room1"
                                        ),
                                        "qos", Map.of(
                                                "type", "integer",
                                                "description", "Quality of Service level (0, 1, or 2). Default: 1 | " +
                                                        "服务质量等级 (0, 1, 或 2)。默认: 1",
                                                "default", 1
                                        )
                                ),
                                "required", List.of("topic")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String topic = (String) arguments.get("topic");
                    Integer qos = arguments.containsKey("qos") ? ((Number) arguments.get("qos")).intValue() : 1;
                    mqttService.subscribe(topic, qos);
                    return ToolResult.text("Subscribed successfully! Topic: " + topic + ", QoS: " + qos);
                } catch (Exception e) {
                    log.error("Subscribe failed", e);
                    return ToolResult.error("Subscribe failed: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 取消订阅 MQTT 主题
     */
    public ToolHandler unsubscribe() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("mqtt_unsubscribe")
                        .description("Unsubscribe from MQTT topic. | 取消订阅MQTT主题。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "topic", Map.of(
                                                "type", "string",
                                                "description", "Topic to unsubscribe from | 要取消订阅的主题"
                                        )
                                ),
                                "required", List.of("topic")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String topic = (String) arguments.get("topic");
                    mqttService.unsubscribe(topic);
                    return ToolResult.text("Unsubscribed successfully! Topic: " + topic);
                } catch (Exception e) {
                    log.error("Unsubscribe failed", e);
                    return ToolResult.error("Unsubscribe failed: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 发布消息到 MQTT
     */
    public ToolHandler publish() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("mqtt_publish")
                        .description("Publish message to MQTT topic. Use this to send commands or data to devices. " +
                                "| 发布消息到MQTT主题。用于向设备发送命令或数据。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "topic", Map.of(
                                                "type", "string",
                                                "description", "Topic to publish to | 要发布到的主题"
                                        ),
                                        "payload", Map.of(
                                                "type", "string",
                                                "description", "Message payload (JSON or plain text) | 消息内容(JSON或纯文本)"
                                        ),
                                        "qos", Map.of(
                                                "type", "integer",
                                                "description", "Quality of Service level. Default: 1 | 服务质量等级。默认: 1",
                                                "default", 1
                                        ),
                                        "retained", Map.of(
                                                "type", "boolean",
                                                "description", "Whether to retain the message. Default: false | 是否保留消息。默认: false",
                                                "default", false
                                        )
                                ),
                                "required", List.of("topic", "payload")
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String topic = (String) arguments.get("topic");
                    String payload = (String) arguments.get("payload");
                    Integer qos = arguments.containsKey("qos") ? ((Number) arguments.get("qos")).intValue() : 1;
                    Boolean retained = arguments.containsKey("retained") ? (Boolean) arguments.get("retained") : false;
                    
                    mqttService.publish(topic, payload, qos, retained);
                    return ToolResult.text("Published successfully! Topic: " + topic + ", QoS: " + qos);
                } catch (Exception e) {
                    log.error("Publish failed", e);
                    return ToolResult.error("Publish failed: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 查询缓存的实时消息
     */
    public ToolHandler queryRealtime() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("mqtt_query_realtime")
                        .description("Query recent real-time MQTT messages from memory cache. " +
                                "Use this to get the latest sensor data without querying database. " +
                                "| 从内存缓存查询最近的实时MQTT消息。用于获取最新的传感器数据，无需查询数据库。 " +
                                "Note: For historical data, use mcp-database to query MySQL. " +
                                "注意：查询历史数据请使用 mcp-database 查询 MySQL。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "topic", Map.of(
                                                "type", "string",
                                                "description", "Topic pattern to filter (optional). " +
                                                        "Example: sensor/temperature | 主题过滤模式(可选)"
                                        ),
                                        "limit", Map.of(
                                                "type", "integer",
                                                "description", "Maximum number of messages to return. Default: 20 | " +
                                                        "返回的最大消息数。默认: 20",
                                                "default", 20
                                        )
                                )
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    String topic = (String) arguments.get("topic");
                    Integer limit = arguments.containsKey("limit") ? ((Number) arguments.get("limit")).intValue() : 20;
                    
                    List<CachedMessage> messages = mqttService.queryCachedMessages(topic, limit);

                    if (messages.isEmpty()) {
                        return ToolResult.text("No cached messages found. Cache size: " + mqttService.getCacheSize());
                    }

                    StringBuilder result = new StringBuilder();
                    result.append("Found ").append(messages.size()).append(" recent messages:\n\n");

                    for (CachedMessage msg : messages) {
                        result.append(String.format("[%s] Topic: %s\n",
                                msg.getReceivedTime(), msg.getTopic()));
                        result.append(String.format("  Device: %s/%s\n",
                                msg.getDeviceInfo().getDeviceType(),
                                msg.getDeviceInfo().getDeviceId()));
                        result.append(String.format("  Payload: %s\n\n",
                                msg.getPayload().length() > 200 
                                        ? msg.getPayload().substring(0, 200) + "..." 
                                        : msg.getPayload()));
                    }

                    return ToolResult.text(result.toString());
                } catch (Exception e) {
                    log.error("Query realtime failed", e);
                    return ToolResult.error("Query failed: " + e.getMessage());
                }
            }
        };
    }

    /**
     * 获取 MQTT 连接状态
     */
    public ToolHandler status() {
        return new ToolHandler() {
            @Override
            public ToolDefinition getDefinition() {
                return ToolDefinition.builder()
                        .name("mqtt_status")
                        .description("Get MQTT connection status and subscribed topics. " +
                                "| 获取MQTT连接状态和已订阅的主题列表。")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of()
                        ))
                        .build();
            }

            @Override
            public ToolResult execute(Map<String, Object> arguments) {
                try {
                    StringBuilder result = new StringBuilder();
                    result.append("MQTT Status:\n");
                    result.append("- Connected: ").append(mqttService.isConnected()).append("\n");
                    result.append("- Cache Size: ").append(mqttService.getCacheSize()).append(" messages\n");
                    result.append("- Subscribed Topics:\n");
                    
                    for (String topic : mqttService.getSubscribedTopics()) {
                        result.append("  - ").append(topic).append("\n");
                    }

                    return ToolResult.text(result.toString());
                } catch (Exception e) {
                    log.error("Get status failed", e);
                    return ToolResult.error("Get status failed: " + e.getMessage());
                }
            }
        };
    }
}
