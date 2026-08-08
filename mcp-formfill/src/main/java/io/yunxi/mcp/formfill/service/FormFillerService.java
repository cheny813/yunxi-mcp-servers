package io.yunxi.mcp.formfill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yunxi.mcp.formfill.config.SceneConfig;
import io.yunxi.mcp.formfill.config.SceneConfigLoader;
import io.yunxi.mcp.formfill.model.FormFillMessage.FillResult;
import io.yunxi.mcp.formfill.controller.FormFillWebSocketController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单填写服务
 *
 * 纯透传：接收上游 LLM 已映射好的表单数据，直接通过 WebSocket 下发到前端。
 * 服务端不做字段映射、值翻译、也不做任何迭代/批处理——这些全部由 LLM 自行完成。
 */
@Slf4j
@Service
public class FormFillerService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SceneConfigLoader sceneConfigLoader;

    @Autowired
    private PageStructureRegistry structureRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("表单填写服务已初始化，已加载场景: {}", sceneConfigLoader.listSceneIds());
    }

    /**
     * 通用填写入口：将 LLM 已映射好的表单数据下发到前端。
     *
     * @param scene    场景标识
     * @param formData 表单数据，键为表单元素 ID（由上游 LLM 根据 formfill_get_structure 产出）
     * @param params   附加参数透传：targetPage、requestId、waitFeedback 等
     */
    public FillResult fillForm(String scene, Map<String, Object> formData, Map<String, Object> params) {
        SceneConfig config = sceneConfigLoader.getSceneConfig(scene);
        if (config == null) {
            return FillResult.builder()
                    .success(false)
                    .error("未知场景: " + scene + "，可用场景: " + sceneConfigLoader.listSceneIds())
                    .build();
        }

        if (params == null) params = new HashMap<>();
        Object targetPage = params.get("targetPage");

        FillResult r = sendFill(config, formData, params, targetPage);
        return r != null ? r : FillResult.builder().success(false).error("填写下发失败").build();
    }

    /** 获取某场景的表单结构（从 PageStructureRegistry 返回前端回写的真实页面字段） */
    public Map<String, Object> getFormStructure(String scene) {
        SceneConfig config = sceneConfigLoader.getSceneConfig(scene);
        if (config == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "未知场景: " + scene);
            return err;
        }
        Map<String, Object> structure = new HashMap<>();
        structure.put("scene", config.getId());
        structure.put("name", config.getName());
        structure.put("description", config.getDescription());

        List<Map<String, Object>> liveFields = structureRegistry.getLatest(scene);
        structure.put("hasLiveStructure", !liveFields.isEmpty());
        if (!liveFields.isEmpty()) {
            structure.put("fields", liveFields);
            log.info("getFormStructure 返回真实页面结构: scene={}, 字段数={}",
                    scene, liveFields.size());
        }
        return structure;
    }

    /**
     * 取得某场景最近一次前端回写的真实页面结构。
     * 供调试面板 / 上层调用方核验"后端是否真的拿到了页面 DOM 字段"。
     */
    public List<Map<String, Object>> getLiveStructure(String scene) {
        return structureRegistry.getLatest(scene);
    }

    /** 列出所有已注册场景 */
    public List<Map<String, Object>> listScenes() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SceneConfig c : sceneConfigLoader.getSceneConfigs().values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("description", c.getDescription());
            list.add(m);
        }
        return list;
    }

    /**
     * 将 LLM 已映射好的表单数据通过 WebSocket 下发到前端执行填写。
     */
    private FillResult sendFill(SceneConfig config, Map<String, Object> data,
                                Map<String, Object> params, Object targetPage) {

        // 用真实页面结构做下发前校验（仅告警，不阻断）
        List<Map<String, Object>> liveFields = structureRegistry.getLatest(config.getId());
        if (!liveFields.isEmpty()) {
            java.util.Set<String> liveNames = new java.util.HashSet<>();
            for (Map<String, Object> f : liveFields) {
                if (f.get("name") != null) liveNames.add(String.valueOf(f.get("name")));
                if (f.get("id") != null) liveNames.add(String.valueOf(f.get("id")));
            }
            for (String key : data.keySet()) {
                if (!liveNames.contains(key)) {
                    log.warn("下发字段不在真实页面结构中: scene={}, field={}", config.getId(), key);
                }
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "fill");
        payload.put("scene", config.getId());
        payload.put("formData", data);
        payload.put("params", params);
        if (targetPage != null) payload.put("targetPage", targetPage);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("序列化表单数据失败", e);
            return FillResult.builder().success(false).error("序列化失败").build();
        }

        messagingTemplate.convertAndSend(FormFillWebSocketController.TOPIC, json);
        log.info("已下发表单填写指令: scene={}, 字段数={}", config.getId(), data.size());
        return FillResult.builder()
                .success(true)
                .filledCount(data.size())
                .message("已下发填写指令（" + data.size() + " 个字段）")
                .build();
    }
}
