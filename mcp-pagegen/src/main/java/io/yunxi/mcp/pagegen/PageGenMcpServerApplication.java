package io.yunxi.mcp.pagegen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;

/**
 * MCP Page Generator Server 主启动类
 * 
 * 提供页面自动生成能力，支持统计页面、列表页、详情页。
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li>统计仪表盘生成</li>
 *   <li>数据列表页生成</li>
 *   <li>详情页生成</li>
 *   <li>图表集成</li>
 *   <li>数据导出（Excel/PDF）</li>
 * </ul>
 *
 * <h3>应用场景</h3>
 * <ul>
 *   <li>自动生成统计报表</li>
 *   <li>快速生成管理后台</li>
 *   <li>数据可视化页面</li>
 *   <li>动态页面生成</li>
 * </ul>
 *
 * @author yunxi
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
public class PageGenMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PageGenMcpServerApplication.class, args);
    }
}
