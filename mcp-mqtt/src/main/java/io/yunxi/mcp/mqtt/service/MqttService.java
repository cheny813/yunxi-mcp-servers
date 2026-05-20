package io.yunxi.mcp.mqtt.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MQTT 服务
 * 
 * 只负责 MQTT 实时订阅/发布，不持久化数据。
 * 历史数据查询请使用 mcp-database 查询 MySQL。
 */
@Slf4j
@Service
public class MqttService {

    @Autowired
    private MqttClient mqttClient;

    @Autowired
    private MqttConnectOptions connectionOptions;

    @Value("${mqtt.subscriptions:[]}")
    private List<Map<String, Object>> defaultSubscriptions;

    @Value("${mqtt.cache.max-size:100}")
    private int cacheMaxSize;

    private final List<String> subscribedTopics = new CopyOnWriteArrayList<>();
    
    // 内存缓存最近的实时消息（供 Agent 查询实时数据）
    private final LinkedList<CachedMessage> messageCache = new LinkedList<>();
    
    private volatile boolean connected = false;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            connect();
        } catch (Exception e) {
            log.error("MQTT 连接失败: {}", e.getMessage(), e);
        }
    }

    public void connect() throws Exception {
        if (connected) {
            log.info("MQTT 已连接，无需重复连接");
            return;
        }

        log.info("正在连接 MQTT Broker...");
        IMqttToken token = mqttClient.connectWithResult(connectionOptions);
        token.waitForCompletion();
        if (mqttClient.isConnected()) {
            connected = true;
            log.info("MQTT 连接成功");

            // 设置回调
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT 连接丢失: {}", cause.getMessage());
                    connected = false;
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) {
                    onMessageArrived(topic, mqttMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    try {
                        log.debug("消息发送完成: messageId={}", token.getMessageId());
                    } catch (Exception e) {
                        log.warn("获取消息 ID 失败: {}", e.getMessage());
                    }
                }
            });

            // 订阅默认 Topic
            subscribeDefaultTopics();
        } else {
            throw new Exception("MQTT 连接失败");
        }
    }

    public void disconnect() {
        try {
            mqttClient.disconnect();
            connected = false;
            log.info("MQTT 已断开连接");
        } catch (Exception e) {
            log.error("MQTT 断开连接失败: {}", e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        return connected && mqttClient.isConnected();
    }

    public void subscribe(String topic, int qos) {
        try {
            mqttClient.subscribe(topic, qos);
            subscribedTopics.add(topic);
            log.info("订阅 Topic 成功: topic={}, qos={}", topic, qos);
        } catch (Exception e) {
            log.error("订阅 Topic 失败: topic={}, error={}", topic, e.getMessage(), e);
        }
    }

    public void unsubscribe(String topic) {
        try {
            mqttClient.unsubscribe(topic);
            subscribedTopics.remove(topic);
            log.info("取消订阅 Topic 成功: topic={}", topic);
        } catch (Exception e) {
            log.error("取消订阅 Topic 失败: topic={}, error={}", topic, e.getMessage(), e);
        }
    }

    public void publish(String topic, String payload, int qos, boolean retained) {
        try {
            MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
            mqttMessage.setQos(qos);
            mqttMessage.setRetained(retained);
            mqttClient.publish(topic, mqttMessage);
            log.info("发布消息成功: topic={}, qos={}, retained={}", topic, qos, retained);
        } catch (Exception e) {
            log.error("发布消息失败: topic={}, error={}", topic, e.getMessage(), e);
            throw new RuntimeException("发布消息失败", e);
        }
    }

    public void publish(String topic, String payload) {
        publish(topic, payload, 1, false);
    }

    private void subscribeDefaultTopics() {
        if (defaultSubscriptions == null || defaultSubscriptions.isEmpty()) {
            log.info("未配置默认订阅 Topic");
            return;
        }

        log.info("开始订阅默认 Topic，共 {} 个", defaultSubscriptions.size());
        for (Map<String, Object> sub : defaultSubscriptions) {
            String topic = (String) sub.get("topic");
            Integer qos = sub.containsKey("qos") ? (Integer) sub.get("qos") : 1;
            String description = sub.containsKey("description") ? (String) sub.get("description") : "";

            if (topic != null) {
                subscribe(topic, qos);
                log.info("  - topic: {}, qos: {}, description: {}", topic, qos, description);
            }
        }
    }

    private void onMessageArrived(String topic, MqttMessage mqttMessage) {
        try {
            String payload = new String(mqttMessage.getPayload());
            int qos = mqttMessage.getQos();

            log.info("收到 MQTT 消息: topic={}, qos={}, payload={}",
                    topic, qos, 
                    payload.length() > 100 ? payload.substring(0, 100) + "..." : payload);

            // 解析设备信息
            DeviceInfo deviceInfo = parseDeviceInfo(topic);

            // 缓存到内存（用于实时查询）
            synchronized (messageCache) {
                messageCache.addFirst(new CachedMessage(
                        topic, payload, qos, deviceInfo, LocalDateTime.now()
                ));
                // 保持缓存大小
                while (messageCache.size() > cacheMaxSize) {
                    messageCache.removeLast();
                }
            }

        } catch (Exception e) {
            log.error("处理 MQTT 消息失败: topic={}, error={}", topic, e.getMessage(), e);
        }
    }

    /**
     * 查询缓存的实时消息
     * 用于 Agent 查询最近的实时数据
     */
    public List<CachedMessage> queryCachedMessages(String topic, int limit) {
        synchronized (messageCache) {
            if (topic == null || topic.isEmpty()) {
                return messageCache.stream()
                        .limit(limit)
                        .toList();
            } else {
                return messageCache.stream()
                        .filter(m -> m.getTopic().startsWith(topic.replace("#", "").replace("+", "")))
                        .limit(limit)
                        .toList();
            }
        }
    }

    /**
     * 获取已订阅的 Topic 列表
     */
    public List<String> getSubscribedTopics() {
        return new ArrayList<>(subscribedTopics);
    }

    /**
     * 获取缓存的消息数量
     */
    public int getCacheSize() {
        return messageCache.size();
    }

    /**
     * 清空消息缓存
     */
    public void clearCache() {
        synchronized (messageCache) {
            messageCache.clear();
        }
        log.info("消息缓存已清空");
    }

    private DeviceInfo parseDeviceInfo(String topic) {
        String[] parts = topic.split("/");
        if (parts.length < 3) {
            return new DeviceInfo("unknown", "unknown");
        }
        return new DeviceInfo(parts[1], parts[2]);
    }

    /**
     * 设备信息
     */
    @Data
    public static class DeviceInfo {
        private final String deviceType;
        private final String deviceId;

        public DeviceInfo(String deviceType, String deviceId) {
            this.deviceType = deviceType;
            this.deviceId = deviceId;
        }
    }

    /**
     * 缓存的消息
     */
    @Data
    public static class CachedMessage {
        private final String topic;
        private final String payload;
        private final int qos;
        private final DeviceInfo deviceInfo;
        private final LocalDateTime receivedTime;

        public CachedMessage(String topic, String payload, int qos, DeviceInfo deviceInfo, LocalDateTime receivedTime) {
            this.topic = topic;
            this.payload = payload;
            this.qos = qos;
            this.deviceInfo = deviceInfo;
            this.receivedTime = receivedTime;
        }
    }
}
