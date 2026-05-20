package io.yunxi.mcp.chart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Chart Server 主启动类
 * <p>
 * 提供 26+ 种图表生成的 MCP 服务，支持柱状图、折线图、饼图、热力图等。
 * 使用 Playwright + ECharts 生成高质量图表。
 * </p>
 *
 * <h3>支持的图表类型</h3>
 * <ul>
 *   <li>柱状图 (bar) - 对比数据</li>
 *   <li>折线图 (line) - 趋势分析</li>
 *   <li>饼图 (pie) - 占比分析</li>
 *   <li>面积图 (area) - 趋势+面积</li>
 *   <li>散点图 (scatter) - 数据分布</li>
 *   <li>雷达图 (radar) - 多维对比</li>
 *   <li>热力图 (heatmap) - 数据密度</li>
 *   <li>仪表盘 (gauge) - 进度显示</li>
 *   <li>漏斗图 (funnel) - 转化分析</li>
 *   <li>桑基图 (sankey) - 流向分析</li>
 * </ul>
 *
 * @author yunxi
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
public class ChartMcpServerApplication {

    public static void main(String[] args) {
        log.info("========================================");
        log.info("  MCP Chart Server 正在启动...");
        log.info("  端口: 8087");
        log.info("  图表输出目录: ./charts");
        log.info("========================================");
        SpringApplication.run(ChartMcpServerApplication.class, args);
        log.info("MCP Chart Server 启动成功！");
    }
}
