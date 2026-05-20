package io.yunxi.mcp.knowledge.controller;

import io.yunxi.mcp.knowledge.service.ChangeMonitorService;
import io.yunxi.mcp.knowledge.service.KnowledgeExtractionService;
import io.yunxi.mcp.knowledge.service.KnowledgeVectorService;
import io.yunxi.mcp.knowledge.service.KnowledgeVectorService.KnowledgeItem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库 REST API 控制器
 *
 * @author yunxi-mcp-servers
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class KnowledgeController {

    @Autowired
    private KnowledgeVectorService knowledgeVectorService;

    @Autowired
    private KnowledgeExtractionService knowledgeExtractionService;

    @Autowired
    private ChangeMonitorService changeMonitorService;

    /**
     * 添加知识 (自动向量化)
     *
     * @param request 知识添加请求
     * @return 操作结果
     */
    @PostMapping("/knowledge")
    public Map<String, Object> addKnowledge(@RequestBody KnowledgeAddRequest request) {
        try {
            KnowledgeItem item = new KnowledgeItem();
            item.setId(request.getId());
            item.setDistrictId(request.getDistrictId());
            item.setTitle(request.getTitle());
            item.setContent(request.getContent());
            item.setType(request.getType());
            item.setTags(request.getTags());

            boolean success = knowledgeVectorService.addKnowledge(item);
            return Map.of(
                    "success", success,
                    "message", success ? "知识添加成功" : "知识添加失败");
        } catch (Exception e) {
            log.error("添加知识失败: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 向量语义搜索
     */
    @PostMapping("/knowledge/search")
    public Map<String, Object> searchKnowledge(@RequestBody KnowledgeSearchRequest request) {
        try {
            List<KnowledgeItem> results = knowledgeVectorService.search(
                    request.getKeyword(),
                    request.getDistrictId(),
                    request.getLimit() != null ? request.getLimit() : 10);

            return Map.of(
                    "success", true,
                    "results", results,
                    "count", results.size());
        } catch (Exception e) {
            log.error("搜索知识失败: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取知识列表
     */
    @GetMapping("/knowledge")
    public Map<String, Object> listKnowledge(
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<KnowledgeItem> results = knowledgeVectorService.listKnowledge(districtId, type, limit);
            return Map.of(
                    "success", true,
                    "results", results,
                    "count", results.size());
        } catch (Exception e) {
            log.error("获取知识列表失败: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 删除知识
     */
    @DeleteMapping("/knowledge/{id}")
    public Map<String, Object> deleteKnowledge(@PathVariable String id) {
        try {
            boolean success = knowledgeVectorService.deleteKnowledge(id);
            return Map.of(
                    "success", success,
                    "message", success ? "知识删除成功" : "知识删除失败");
        } catch (Exception e) {
            log.error("删除知识失败: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 从代码目录提取知识并入库
     */
    @PostMapping("/knowledge/extract")
    public Map<String, Object> extractKnowledge(@RequestBody KnowledgeExtractRequest request) {
        try {
            var result = knowledgeExtractionService.extractFromCode(
                    request.getCodePath(),
                    request.getDistrictId());

            return Map.of(
                    "success", result.success(),
                    "message", result.message(),
                    "extractedCount", result.extractedCount());
        } catch (Exception e) {
            log.error("知识提取失败: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @Data
    public static class KnowledgeAddRequest {
        private String id;
        private String districtId;
        private String title;
        private String content;
        private String type;
        private String tags;
    }

    @Data
    public static class KnowledgeSearchRequest {
        private String keyword;
        private String districtId;
        private Integer limit;
    }

    @Data
    public static class KnowledgeExtractRequest {
        private String codePath;
        private String districtId;
    }

    /**
     * 添加仓库监控
     */
    @PostMapping("/knowledge/monitor")
    public Map<String, Object> addMonitor(@RequestBody MonitorAddRequest request) {
        try {
            changeMonitorService.addMonitorConfig(
                    request.getRepoId(),
                    request.getRepoPath(),
                    request.getDistrictId());
            return Map.of("success", true, "message", "监控已添加");
        } catch (Exception e) {
            log.error("添加监控失败: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取监控状态
     */
    @GetMapping("/knowledge/monitor")
    public Map<String, Object> getMonitorStatus() {
        return changeMonitorService.getMonitorStatus();
    }

    /**
     * 手动触发同步
     */
    @PostMapping("/knowledge/sync/{repoId}")
    public Map<String, Object> manualSync(@PathVariable String repoId) {
        return changeMonitorService.manualSync(repoId);
    }

    @Data
    public static class MonitorAddRequest {
        private String repoId;
        private String repoPath;
        private String districtId;
    }
}