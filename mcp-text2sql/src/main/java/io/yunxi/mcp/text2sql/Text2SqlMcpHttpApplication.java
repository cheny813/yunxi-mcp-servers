package io.yunxi.mcp.text2sql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Text2SQL MCP 服务器入口（HTTP/SSE 模式）。
 * <p>
 * 基于 Focus_MCP_SQL 的两步法设计哲学（先 schema 关联定位、再生成 SQL，降低大模型幻觉），
 * 但去掉其 DataFocus 云依赖，改为自有 OpenAI 兼容 LLM + JDBC 自动内省 schema，自包含运行。
 * </p>
 * <p>
 * 排除 {@link DataSourceAutoConfiguration}：数据源连接由 {@code DatasourceRegistry} 按配置
 * 懒加载自建 Hikari 连接池，不由 Spring 自动配置（配置前缀为 {@code text2sql.datasources}，
 * 非 {@code spring.datasource}）。
 * </p>
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class Text2SqlMcpHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(Text2SqlMcpHttpApplication.class, args);
    }
}
