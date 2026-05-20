package io.yunxi.mcp.redis;

import io.yunxi.mcp.common.controller.AbstractMcpController;
import io.yunxi.mcp.redis.tools.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP Redis 控制器
 * <p>
 * 继承 {@link AbstractMcpController}，提供 Redis 操作的 MCP 端点。
 * 统一由基类提供 HTTP/SSE 端点实现，本类仅负责工具注册。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>redis_get - 获取键值</li>
 * <li>redis_set - 设置键值</li>
 * <li>redis_delete - 删除键</li>
 * <li>redis_keys - 列出键</li>
 * <li>redis_list_ops - List 操作</li>
 * <li>redis_hash_ops - Hash 操作</li>
 * </ul>
 */
@Slf4j
@RestController
public class McpController extends AbstractMcpController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected String getServerName() {
        return "yunxi-mcp-redis";
    }

    @Override
    protected void registerTools() {
        // 注册 Redis 操作工具
        registerTool(new GetTool(redisTemplate));
        registerTool(new SetTool(redisTemplate));
        registerTool(new DeleteTool(redisTemplate));
        registerTool(new KeysTool(redisTemplate));
        registerTool(new ListOpsTool(redisTemplate));
        registerTool(new HashOpsTool(redisTemplate));

        log.info("MCP Redis 工具注册完成，可用工具: {}",
                httpEndpoint.getTools().stream().map(tool -> tool.getName()).toList());
    }
}
