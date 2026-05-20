package io.yunxi.mcp.mqtt.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 客户端配置
 * <p>
 * 配置 MQTT Broker 连接参数和连接选项。
 * </p>
 */
@Slf4j
@Configuration
public class MqttClientConfig {

    @Value("${mqtt.broker.host:localhost}")
    private String brokerHost;

    @Value("${mqtt.broker.port:1883}")
    private int brokerPort;

    @Value("${mqtt.broker.scheme:tcp}")
    private String scheme;

    @Value("${mqtt.broker.clientId:yunxi-mcp-server}")
    private String clientId;

    @Value("${mqtt.connection.timeout:10}")
    private int connectionTimeout;

    @Value("${mqtt.connection.automatic-reconnect:true}")
    private boolean automaticReconnect;

    @Value("${mqtt.connection.max-reconnect-delay:30}")
    private int maxReconnectDelay;

    /**
     * 创建 MQTT 连接选项
     *
     * @return MqttConnectOptions
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();

        String serverUri = String.format("%s://%s:%d", scheme, brokerHost, brokerPort);
        log.info("MQTT Broker URI: {}", serverUri);

        options.setServerURIs(new String[]{serverUri});
        // Note: clientId is set in MqttClient constructor, not here (MQTT v3 API)
        options.setConnectionTimeout(connectionTimeout);
        options.setAutomaticReconnect(automaticReconnect);
        options.setMaxReconnectDelay(maxReconnectDelay);

        // Clean Session 设置为 true，每次连接都是全新会话
        options.setCleanSession(true);

        log.info("MQTT 连接选项配置完成: automaticReconnect={}, maxReconnectDelay={}秒",
                automaticReconnect, maxReconnectDelay);

        return options;
    }

    /**
     * 创建 MQTT 客户端实例
     *
     * @return MqttClient
     * @throws Exception 异常
     */
    @Bean
    public MqttClient mqttClient(MqttConnectOptions connectOptions) throws Exception {
        String serverUri = connectOptions.getServerURIs()[0];
        MqttClient client = new MqttClient(serverUri, clientId);
        log.info("MQTT 客户端创建成功: clientId={}", clientId);
        return client;
    }
}
