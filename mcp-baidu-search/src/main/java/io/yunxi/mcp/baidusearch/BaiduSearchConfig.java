package io.yunxi.mcp.baidusearch;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度搜索配置
 *
 * @author yunxi-mcp-servers
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "baidu-search")
public class BaiduSearchConfig {

    /**
     * 是否启用百度搜索
     */
    private boolean enabled = true;

    /**
     * 百度千帆 API Key
     * 从环境变量 BAIDU_API_KEY 获取
     */
    private String apiKey;

    /**
     * API 端点
     */
    private String endpoint = "https://qianfan.baidubce.com/v2/ai_search/web_search";

    /**
     * 默认返回结果数
     */
    private int defaultCount = 10;

    /**
     * 最大结果数
     */
    private int maxCount = 50;
}