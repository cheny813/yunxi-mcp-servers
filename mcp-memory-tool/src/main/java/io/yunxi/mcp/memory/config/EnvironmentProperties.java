package io.yunxi.mcp.memory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 环境模式配置属性
 * <p>
 * 配置应用运行的环境模式，影响存储路径和行为选择。
 * </p>
 * <p>
 * 配置前缀：{@code yunxi.environment}
 * </p>
 * <p>
 * 环境模式说明：
 * </p>
 * <ul>
 *   <li>dev（开发模式）：使用本地存储路径，适合单机开发和测试</li>
 *   <li>prod（生产模式）：使用共享存储路径，适合集群部署</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 */
@Data
@ConfigurationProperties(prefix = "yunxi.environment")
public class EnvironmentProperties {

    /**
     * 环境模式：dev（开发）或 prod（生产）
     * 默认值为 prod，可通过系统属性 {@code yunxi.environment.mode} 覆盖
     */
    private String mode = "prod";
}
