package io.yunxi.mcp.baiduasr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度语音识别配置
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "baidu-asr")
public class BaiduAsrConfig {

    /**
     * 是否启用百度 ASR
     */
    private boolean enabled = true;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * API 端点
     */
    private String endpoint = "https://vop.baidu.com/server_api";

    /**
     * Token 端点
     */
    private String tokenEndpoint = "https://aip.baidubce.com/oauth/2.0/token";

    /**
     * 超时时间（秒）
     */
    private int timeout = 60;

    /**
     * 默认语言
     */
    private String devPid = "1537"; // 普通话
}