package io.yunxi.mcp.database.controller;

import io.yunxi.mcp.database.entity.DistrictInfo;
import io.yunxi.mcp.database.entity.DistrictDb;
import io.yunxi.mcp.database.entity.DistrictCode;
import io.yunxi.mcp.database.entity.KnowledgeBase;
import io.yunxi.mcp.database.service.DistrictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 区县配置中心 API
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class DistrictController {

    private final DistrictService districtService;

    public DistrictController(DistrictService districtService) {
        this.districtService = districtService;
    }

    // ===== 区县管理 =====

    @GetMapping("/districts")
    public Map<String, Object> getAllDistricts() {
        List<DistrictInfo> list = districtService.getAllDistricts();
        return Map.of("districts", list, "total", list.size());
    }

    @GetMapping("/districts/{id}")
    public Map<String, Object> getDistrict(@PathVariable String id) {
        DistrictInfo district = districtService.getDistrict(id);
        if (district == null) {
            return Map.of("status", "error", "message", "区县不存在");
        }
        return Map.of("district", district);
    }

    @PostMapping("/districts")
    public Map<String, Object> addDistrict(@RequestBody DistrictInfo district) {
        DistrictInfo saved = districtService.addDistrict(district);
        if (saved != null) {
            return Map.of("status", "success", "district", saved);
        }
        return Map.of("status", "error", "message", "添加失败");
    }

    @DeleteMapping("/districts/{id}")
    public Map<String, Object> deleteDistrict(@PathVariable String id) {
        boolean success = districtService.deleteDistrict(id);
        return Map.of("status", success ? "success" : "error");
    }

    // ===== 数据库配置 =====

    @GetMapping("/districts/{id}/databases")
    public Map<String, Object> getDatabases(@PathVariable String id) {
        List<DistrictDb> list = districtService.getDatabases(id);
        return Map.of("databases", list, "total", list.size());
    }

    @PostMapping("/districts/{id}/databases")
    public Map<String, Object> addDatabase(@PathVariable String id, @RequestBody DistrictDb db) {
        db.setDistrictId(id);
        DistrictDb saved = districtService.addDatabase(db);
        if (saved != null) {
            return Map.of("status", "success", "database", saved);
        }
        return Map.of("status", "error", "message", "添加失败");
    }

    @DeleteMapping("/districts/{districtId}/databases/{id}")
    public Map<String, Object> deleteDatabase(@PathVariable String districtId, @PathVariable String id) {
        boolean success = districtService.deleteDatabase(id);
        return Map.of("status", success ? "success" : "error");
    }

    // ===== 代码版本 =====

    @GetMapping("/districts/{id}/code")
    public Map<String, Object> getCodeVersion(@PathVariable String id) {
        DistrictCode code = districtService.getCodeVersion(id);
        if (code == null) {
            return Map.of("code", Map.of());
        }
        return Map.of("code", code);
    }

    @PostMapping("/districts/{id}/code")
    public Map<String, Object> addCodeVersion(@PathVariable String id, @RequestBody DistrictCode code) {
        code.setDistrictId(id);
        DistrictCode saved = districtService.addCodeVersion(code);
        if (saved != null) {
            return Map.of("status", "success", "code", saved);
        }
        return Map.of("status", "error", "message", "添加失败");
    }

    // ===== 知识库 =====

    @GetMapping("/knowledge")
    public Map<String, Object> searchKnowledge(
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q) {
        List<KnowledgeBase> list = districtService.searchKnowledge(districtId, type, q);
        return Map.of("knowledge", list, "total", list.size());
    }

    @PostMapping("/knowledge")
    public Map<String, Object> addKnowledge(@RequestBody KnowledgeBase kb) {
        KnowledgeBase saved = districtService.addKnowledge(kb);
        if (saved != null) {
            return Map.of("status", "success", "knowledge", saved);
        }
        return Map.of("status", "error", "message", "添加失败");
    }
}
