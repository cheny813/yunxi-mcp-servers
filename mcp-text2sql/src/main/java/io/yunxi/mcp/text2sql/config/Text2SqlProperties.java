package io.yunxi.mcp.text2sql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Text2SQL 配置：数据源集合与默认语种。
 */
@Component
@ConfigurationProperties("text2sql")
public class Text2SqlProperties {

    private String defaultLanguage = "chinese";
    private Map<String, DatasourceConfig> datasources = new LinkedHashMap<>();

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public Map<String, DatasourceConfig> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, DatasourceConfig> datasources) {
        this.datasources = datasources;
    }

    /**
     * 单个数据源连接配置。
     */
    public static class DatasourceConfig {
        private String url;
        private String username;
        private String password;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
