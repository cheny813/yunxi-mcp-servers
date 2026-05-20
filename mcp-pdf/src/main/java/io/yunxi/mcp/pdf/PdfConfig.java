package io.yunxi.mcp.pdf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * PDF 处理配置
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pdf")
public class PdfConfig {

    /**
     * 是否启用 PDF 服务
     */
    private boolean enabled = true;

    /**
     * 默认输出目录
     */
    private String outputDir = "./output";
}