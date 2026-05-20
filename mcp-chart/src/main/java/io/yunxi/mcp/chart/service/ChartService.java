package io.yunxi.mcp.chart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 图表生成服务
 * <p>
 * 使用 Playwright + ECharts 生成高质量图表。
 * 支持生成柱状图、折线图、饼图、热力图等多种图表类型。
 * </p>
 */
@Slf4j
@Service
public class ChartService {

    @Value("${chart.output-dir:./charts}")
    private String outputDir;

    @Value("${chart.image-format:png}")
    private String imageFormat;

    @Value("${chart.image-width:800}")
    private int imageWidth;

    @Value("${chart.image-height:600}")
    private int imageHeight;

    @Value("${chart.headless:true}")
    private boolean headless;

    @Value("${chart.browser:chromium}")
    private String browserType;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成图表
     * <p>
     * 根据 ECharts 配置生成图表图片。
     * </p>
     *
     * @param chartConfig 图表配置（ECharts option 格式）
     * @return 图表结果，包含 URL 和 Base64 编码
     */
    public ChartResult generateChart(ChartConfig chartConfig) {
        log.info("开始生成图表: type={}, title={}", chartConfig.getType(), chartConfig.getTitle());

        String chartId = UUID.randomUUID().toString();
        String fileName = String.format("%s.%s", chartId, imageFormat);
        String filePath = Paths.get(outputDir, fileName).toString();

        try {
            // 确保输出目录存在
            Files.createDirectories(Paths.get(outputDir));

            // 使用 Playwright 生成图表
            String eChartsConfig = buildEChartsConfig(chartConfig);
            generateChartWithPlaywright(eChartsConfig, filePath);

            // 读取图片并转换为 Base64
            File imageFile = new File(filePath);
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String fileUrl = String.format("/charts/%s", fileName);

            log.info("图表生成成功: id={}, path={}", chartId, filePath);

            return ChartResult.builder()
                    .chartId(chartId)
                    .fileUrl(fileUrl)
                    .filePath(filePath)
                    .base64Data(base64Image)
                    .format(imageFormat)
                    .width(imageWidth)
                    .height(imageHeight)
                    .build();

        } catch (Exception e) {
            log.error("图表生成失败: id={}, error={}", chartId, e.getMessage(), e);
            throw new RuntimeException("图表生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 Playwright 生成图表
     *
     * @param eChartsConfig ECharts 配置
     * @param outputPath   输出路径
     */
    private void generateChartWithPlaywright(String eChartsConfig, String outputPath) {
        try (Playwright playwright = Playwright.create()) {
            log.debug("启动 Playwright 浏览器: type={}, headless={}", browserType, headless);

            Browser browser;
            switch (browserType.toLowerCase()) {
                case "firefox":
                    browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(headless));
                    break;
                case "webkit":
                    browser = playwright.webkit().launch(new BrowserType.LaunchOptions().setHeadless(headless));
                    break;
                default: // chromium
                    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            }

            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            // 构建包含 ECharts 的 HTML
            String html = buildEChartsHtml(eChartsConfig);

            // 设置页面内容
            page.setContent(html);

            // 等待 ECharts 渲染完成
            page.waitForSelector("canvas", new Page.WaitForSelectorOptions().setTimeout(10000));

            // 截图保存
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(outputPath))
                    .setFullPage(false));

            log.debug("图表截图保存成功: {}", outputPath);

            browser.close();
        }
    }

    /**
     * 构建 ECharts HTML
     *
     * @param eChartsConfig ECharts 配置
     * @return HTML 内容
     */
    private String buildEChartsHtml(String eChartsConfig) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>Chart</title>
                    <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                        }
                        body {
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            background: white;
                        }
                        #chart {
                            width: %dpx;
                            height: %dpx;
                        }
                    </style>
                </head>
                <body>
                    <div id="chart"></div>
                    <script>
                        var chartDom = document.getElementById('chart');
                        var myChart = echarts.init(chartDom);
                        var option = %s;
                        myChart.setOption(option);
                    </script>
                </body>
                </html>
                """, imageWidth, imageHeight, eChartsConfig);
    }

    /**
     * 构建 ECharts 配置
     *
     * @param chartConfig 图表配置
     * @return JSON 格式的 ECharts 配置
     */
    private String buildEChartsConfig(ChartConfig chartConfig) {
        try {
            Map<String, Object> option = new HashMap<>();

            // 标题
            if (chartConfig.getTitle() != null) {
                Map<String, Object> title = new HashMap<>();
                title.put("text", chartConfig.getTitle());
                option.put("title", title);
            }

            // 提示框
            Map<String, Object> tooltip = new HashMap<>();
            tooltip.put("trigger", "axis");
            option.put("tooltip", tooltip);

            // 图例
            if (chartConfig.getLegend() != null && chartConfig.getLegend()) {
                option.put("legend", Map.of("data", chartConfig.getSeriesNames()));
            }

        // X 轴
        if (chartConfig.getXAxis() != null) {
            Map<String, Object> xAxis = new HashMap<>();
            // 根据图表类型选择合适的 X 轴类型
            String xAxisType = "category".equals(chartConfig.getType()) || "pie".equals(chartConfig.getType()) 
                ? "category" 
                : "value";
            xAxis.put("type", xAxisType);
            xAxis.put("data", chartConfig.getXAxis());
            option.put("xAxis", new Map[]{xAxis});
        }

        // Y 轴
        Map<String, Object> yAxis = new HashMap<>();
        // 根据图表类型选择合适的 Y 轴类型
        String yAxisType = "category".equals(chartConfig.getType()) || "pie".equals(chartConfig.getType())
            ? "category"
            : "value";
        yAxis.put("type", yAxisType);
        option.put("yAxis", new Map[]{yAxis});

            // 系列
            option.put("series", buildSeries(chartConfig));

            // 主题颜色
            if (chartConfig.getColors() != null && !chartConfig.getColors().isEmpty()) {
                option.put("color", chartConfig.getColors());
            }

            return objectMapper.writeValueAsString(option);

        } catch (Exception e) {
            log.error("构建 ECharts 配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("构建图表配置失败", e);
        }
    }

    /**
     * 构建系列数据
     *
     * @param chartConfig 图表配置
     * @return 系列数组
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object>[] buildSeries(ChartConfig chartConfig) {
        if (chartConfig.getSeriesData() == null || chartConfig.getSeriesData().isEmpty()) {
            return new Map[]{};
        }

        return chartConfig.getSeriesData().stream().map(series -> {
            Map<String, Object> seriesMap = new HashMap<>();
            seriesMap.put("name", series.get("name"));
            seriesMap.put("type", chartConfig.getType());
            seriesMap.put("data", series.get("data"));

            // 饼图专用配置
            if ("pie".equals(chartConfig.getType())) {
                seriesMap.put("radius", series.getOrDefault("radius", "50%"));
            }

            return seriesMap;
        }).toArray(Map[]::new);
    }

    /**
     * 图表配置类
     */
    @Data
    public static class ChartConfig {
        /**
         * 图表类型：bar, line, pie, area, scatter, radar
         */
        private String type;

        /**
         * 图表标题
         */
        private String title;

        /**
         * 是否显示图例
         */
        private Boolean legend = true;

        /**
         * X 轴数据（类目轴）
         */
        private Object[] xAxis;

        /**
         * 系列数据
         */
        private java.util.List<Map<String, Object>> seriesData;

        /**
         * 系列名称
         */
        private java.util.List<String> seriesNames;

        /**
         * 颜色主题
         */
        private java.util.List<String> colors;
    }

    /**
     * 图表结果类
     */
    @Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ChartResult {
        private String chartId;
        private String fileUrl;
        private String filePath;
        private String base64Data;
        private String format;
        private int width;
        private int height;
    }
}
