package io.yunxi.mcp.xlsx;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Excel 处理配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "xlsx")
public class XlsxConfig {

    /**
     * 是否启用服务
     */
    private boolean enabled = true;

    /**
     * 默认输出目录
     */
    private String outputDir = "./output";
}