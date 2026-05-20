package io.yunxi.mcp.database;

import io.yunxi.mcp.common.controller.AbstractMcpController;
import io.yunxi.mcp.database.config.DatabaseConfigService;
import io.yunxi.mcp.database.config.DatabaseConfig;
import io.yunxi.mcp.database.tool.*;
import io.yunxi.mcp.database.tool.district.GetDistrictConfigTool;
import io.yunxi.mcp.database.tool.district.GetDistrictDatabasesTool;
import io.yunxi.mcp.database.service.DistrictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

/**
 * MCP 数据库控制器
 * <p>
 * 继承 {@link AbstractMcpController}，提供数据库操作的 MCP 端点。
 * 统一由基类提供 HTTP/SSE 端点实现，本类仅负责工具注册。
 * </p>
 *
 * <h3>提供的工具</h3>
 * <ul>
 * <li>query_db - 执行 SQL 查询（支持多数据库）</li>
 * <li>list_databases - 列出可用数据库</li>
 * <li>list_tables - 列出数据库表</li>
 * <li>describe_table - 描述表结构</li>
 * <li>batch_query_dish_ingredients - 批量查询菜品食材</li>
 * <li>get_district_config - 获取区县配置</li>
 * <li>get_district_databases - 获取区县数据库列表</li>
 * </ul>
 */
@Slf4j
@RestController
public class McpController extends AbstractMcpController {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private DatabaseConfigService configService;

    @Autowired(required = false)
    private DistrictService districtService;

    private DatabaseConfigService dbConfigService;

    @Override
    protected String getServerName() {
        return "yunxi-mcp-database";
    }

    @Override
    protected void registerTools() {
        // 初始化 DatabaseConfigService
        dbConfigService = configService;
        if (dbConfigService == null) {
            log.warn("DatabaseConfigService 未注入，使用旧模式（单数据源）");
            dbConfigService = new DatabaseConfigService();
            if (dataSource != null) {
                DatabaseConfig defaultDb = new DatabaseConfig();
                defaultDb.setId("default");
                defaultDb.setName("默认数据库");
                dbConfigService.addDatabase(defaultDb);
            }
        }

        // 注册数据库操作工具
        registerTool(new QueryTool(dbConfigService));
        registerTool(new ListDatabasesTool(dbConfigService));
        registerTool(new ListTablesTool(dataSource));
        registerTool(new DescribeTableTool(dataSource));
        registerTool(new BatchQueryDishIngredientsTool(dataSource));

        // 区县配置工具
        registerTool(new GetDistrictConfigTool(districtService));
        registerTool(new GetDistrictDatabasesTool(districtService));

        log.info("MCP Database 工具注册完成，可用工具: {}",
                httpEndpoint.getTools().stream().map(tool -> tool.getName()).toList());
    }
}
