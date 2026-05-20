package io.yunxi.mcp.pagegen.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 页面生成服务
 */
@Slf4j
@Service
public class PageGenService {

    @Value("${pagegen.theme:bootstrap5}")
    private String theme;

    /**
     * 生成统计仪表盘页面
     */
    public String generateDashboard(Map<String, Object> config) {
        try {
            String title = (String) config.getOrDefault("title", "数据统计仪表盘");
            List<Map<String, Object>> kpiCards = (List<Map<String, Object>>) config.getOrDefault("kpi_cards", new ArrayList<>());
            List<Map<String, Object>> charts = (List<Map<String, Object>>) config.getOrDefault("charts", new ArrayList<>());

            return buildDashboard(title, kpiCards, charts);
        } catch (Exception e) {
            log.error("Failed to generate dashboard", e);
            return "";
        }
    }

    /**
     * 生成数据列表页面
     */
    public String generateListPage(Map<String, Object> config) {
        try {
            String title = (String) config.getOrDefault("title", "数据列表");
            List<String> columns = (List<String>) config.getOrDefault("columns", new ArrayList<>());
            List<Map<String, Object>> data = (List<Map<String, Object>>) config.getOrDefault("data", new ArrayList<>());

            return buildListPage(title, columns, data);
        } catch (Exception e) {
            log.error("Failed to generate list page", e);
            return "";
        }
    }

    /**
     * 生成详情页面
     */
    public String generateDetailPage(Map<String, Object> config) {
        try {
            String title = (String) config.getOrDefault("title", "详情页面");
            Map<String, Object> entity = (Map<String, Object>) config.getOrDefault("entity", new HashMap<>());
            List<Map<String, Object>> relatedData = (List<Map<String, Object>>) config.getOrDefault("related_data", new ArrayList<>());

            return buildDetailPage(title, entity, relatedData);
        } catch (Exception e) {
            log.error("Failed to generate detail page", e);
            return "";
        }
    }

    /**
     * 构建仪表盘 HTML
     */
    private String buildDashboard(String title, List<Map<String, Object>> kpiCards, List<Map<String, Object>> charts) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='zh-CN'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <title>").append(title).append("</title>\n");
        html.append("  <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css' rel='stylesheet'>\n");
        html.append("  <script src='https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js'></script>\n");
        html.append("</head>\n");
        html.append("<body class='bg-light'>\n");

        // 导航栏
        html.append("  <nav class='navbar navbar-expand-lg navbar-dark bg-primary mb-4'>\n");
        html.append("    <div class='container'>\n");
        html.append("      <a class='navbar-brand' href='#'>").append(title).append("</a>\n");
        html.append("    </div>\n");
        html.append("  </nav>\n");

        html.append("  <div class='container'>\n");

        // KPI 卡片
        if (!kpiCards.isEmpty()) {
            html.append("    <div class='row mb-4'>\n");
            for (int i = 0; i < kpiCards.size(); i++) {
                Map<String, Object> card = kpiCards.get(i);
                html.append("      <div class='col-md-3'>\n");
                html.append("        <div class='card shadow-sm'>\n");
                html.append("          <div class='card-body'>\n");
                html.append("            <h6 class='card-title text-muted'>").append(card.getOrDefault("label", "")).append("</h6>\n");
                html.append("            <h3 class='card-title'>").append(card.getOrDefault("value", "")).append("</h3>\n");
                if (card.containsKey("trend")) {
                    String trend = (String) card.get("trend");
                    String trendClass = trend.startsWith("+") ? "text-success" : "text-danger";
                    html.append("            <small class='").append(trendClass).append("'>").append(trend).append("</small>\n");
                }
                html.append("          </div>\n");
                html.append("        </div>\n");
                html.append("      </div>\n");
            }
            html.append("    </div>\n");
        }

        // 图表区域
        if (!charts.isEmpty()) {
            html.append("    <div class='row'>\n");
            for (int i = 0; i < charts.size(); i++) {
                Map<String, Object> chart = charts.get(i);
                int colSize = charts.size() == 1 ? 12 : (charts.size() == 2 ? 6 : 4);
                html.append("      <div class='col-md-").append(colSize).append(" mb-4'>\n");
                html.append("        <div class='card shadow-sm'>\n");
                html.append("          <div class='card-body'>\n");
                html.append("            <h5 class='card-title'>").append(chart.getOrDefault("title", "")).append("</h5>\n");
                html.append("            <div id='chart-").append(i).append("' style='width:100%;height:400px;'></div>\n");
                html.append("          </div>\n");
                html.append("        </div>\n");
                html.append("      </div>\n");
            }
            html.append("    </div>\n");
        }

        html.append("  </div>\n");

        // JavaScript 图表配置
        html.append("  <script>\n");
        for (int i = 0; i < charts.size(); i++) {
            Map<String, Object> chart = charts.get(i);
            String chartId = "chart-" + i;
            html.append("    var ").append(chartId).append(" = echarts.init(document.getElementById('").append(chartId).append("'));\n");
            html.append("    var option").append(i).append(" = ").append(chart.getOrDefault("config", "{}")).append(";\n");
            html.append("    ").append(chartId).append(".setOption(option").append(i).append(");\n");
        }
        html.append("  </script>\n");

        html.append("  <script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js'></script>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 构建列表页 HTML
     */
    private String buildListPage(String title, List<String> columns, List<Map<String, Object>> data) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='zh-CN'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <title>").append(title).append("</title>\n");
        html.append("  <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css' rel='stylesheet'>\n");
        html.append("</head>\n");
        html.append("<body class='bg-light'>\n");
        html.append("  <div class='container py-4'>\n");
        html.append("    <h2>").append(title).append("</h2>\n");

        // 搜索和筛选
        html.append("    <div class='row mb-3'>\n");
        html.append("      <div class='col-md-6'>\n");
        html.append("        <input type='text' class='form-control' placeholder='搜索...'>\n");
        html.append("      </div>\n");
        html.append("      <div class='col-md-6 text-end'>\n");
        html.append("        <button class='btn btn-primary'>导出</button>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // 表格
        html.append("    <div class='card shadow-sm'>\n");
        html.append("      <div class='card-body'>\n");
        html.append("        <table class='table table-striped table-hover'>\n");
        html.append("          <thead>\n");
        html.append("            <tr>\n");
        for (String column : columns) {
            html.append("              <th>").append(column).append("</th>\n");
        }
        html.append("              <th>操作</th>\n");
        html.append("            </tr>\n");
        html.append("          </thead>\n");
        html.append("          <tbody>\n");

        // 数据行
        for (int i = 0; i < Math.min(data.size(), 20); i++) {
            Map<String, Object> row = data.get(i);
            html.append("            <tr>\n");
            for (String column : columns) {
                html.append("              <td>").append(row.getOrDefault(column, "")).append("</td>\n");
            }
            html.append("              <td><button class='btn btn-sm btn-info'>查看</button></td>\n");
            html.append("            </tr>\n");
        }

        html.append("          </tbody>\n");
        html.append("        </table>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 构建详情页 HTML
     */
    private String buildDetailPage(String title, Map<String, Object> entity, List<Map<String, Object>> relatedData) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='zh-CN'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <title>").append(title).append("</title>\n");
        html.append("  <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css' rel='stylesheet'>\n");
        html.append("</head>\n");
        html.append("<body class='bg-light'>\n");
        html.append("  <div class='container py-4'>\n");

        // 标题和操作
        html.append("    <div class='d-flex justify-content-between align-items-center mb-4'>\n");
        html.append("      <h2>").append(title).append("</h2>\n");
        html.append("      <div>\n");
        html.append("        <button class='btn btn-secondary me-2'>编辑</button>\n");
        html.append("        <button class='btn btn-danger'>删除</button>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // 详情信息
        html.append("    <div class='card shadow-sm mb-4'>\n");
        html.append("      <div class='card-header bg-primary text-white'>基本信息</div>\n");
        html.append("      <div class='card-body'>\n");
        html.append("        <dl class='row'>\n");

        for (Map.Entry<String, Object> entry : entity.entrySet()) {
            html.append("          <dt class='col-sm-3'>").append(entry.getKey()).append(":</dt>\n");
            html.append("          <dd class='col-sm-9'>").append(entry.getValue()).append("</dd>\n");
        }

        html.append("        </dl>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // 关联数据
        if (!relatedData.isEmpty()) {
            for (Map<String, Object> related : relatedData) {
                html.append("    <div class='card shadow-sm mb-4'>\n");
                html.append("      <div class='card-header'>").append(related.getOrDefault("title", "")).append("</div>\n");
                html.append("      <div class='card-body'>\n");
                html.append("        <p>关联数据展示区域</p>\n");
                html.append("      </div>\n");
                html.append("    </div>\n");
            }
        }

        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 页面生成结果
     */
    @Data
    public static class PageResult {
        private final boolean success;
        private final String html;
        private final String error;

        public PageResult(boolean success, String html, String error) {
            this.success = success;
            this.html = html;
            this.error = error;
        }
    }
}
