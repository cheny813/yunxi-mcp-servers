package io.yunxi.mcp.baiduocr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度 OCR 配置
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "baidu-ocr")
public class BaiduOcrConfig {

    /**
     * 是否启用百度 OCR
     */
    private boolean enabled = true;

    /**
     * API Key
     * 从环境变量 BAIDU_OCR_API_KEY 获取
     */
    private String apiKey;

    /**
     * Secret Key
     * 从环境变量 BAIDU_OCR_SECRET_KEY 获取
     */
    private String secretKey;

    /**
     * API 端点
     */
    private String endpoint = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";

    /**
     * Token 端点
     */
    private String tokenEndpoint = "https://aip.baidubce.com/oauth/2.0/token";

    /**
     * 超时时间（秒）
     */
    private int timeout = 30;
}