package io.yunxi.mcp.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP MQTT Server 主启动类
 * 
 * 提供 MQTT 实时订阅/发布能力，支持温湿度、摄像头等 IoT 设备。
 * 历史数据查询请使用 mcp-database 查询 MySQL。
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li>MQTT Broker 连接与管理</li>
 *   <li>Topic 订阅与发布</li>
 *   <li>实时传感器数据接收</li>
 *   <li>设备控制（发布消息）</li>
 * </ul>
 *
 * <h3>支持的设备类型</h3>
 * <ul>
 *   <li>温湿度传感器 - sensor/temperature/#, sensor/humidity/#</li>
 *   <li>摄像头 - sensor/camera/#</li>
 *   <li>自定义设备 - 任意 Topic</li>
 * </ul>
 *
 * @author yunxi
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
public class MqttMcpServerApplication {

    public static void main(String[] args) {
        log.info("========================================");
        log.info("  MCP MQTT Server Starting...");
        log.info("  Port: 8088");
        log.info("  MQTT Broker: tcp://localhost:1883");
        log.info("  Note: For historical data, use mcp-database");
        log.info("========================================");
        SpringApplication.run(MqttMcpServerApplication.class, args);
        log.info("MCP MQTT Server started successfully!");
    }
}
