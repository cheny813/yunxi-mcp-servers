package io.yunxi.mcp.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 模块自动配置。
 * <p>
 * 通过 {@code @ComponentScan} 确保 {@code io.yunxi.mcp.common.embedding} 包下的
 * {@link io.yunxi.mcp.common.embedding.EmbeddingService} 以及所有
 * {@link io.yunxi.mcp.common.embedding.EmbeddingProvider} 实现类能被
 * 各 MCP 服务器模块自动发现（无需修改各模块的 scanBasePackages）。
 * </p>
 *
 * @author yunxi
 * @since 2.0.0
 */
@Configuration
@ComponentScan(basePackages = "io.yunxi.mcp.common.embedding")
public class EmbeddingAutoConfiguration {
}
