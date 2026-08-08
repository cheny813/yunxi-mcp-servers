package io.yunxi.mcp.formfill.service;

import io.yunxi.mcp.formfill.model.FormFillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 页面结构注册表：缓存各会话的真实页面字段，作为填表指令的下发依据。
 *
 * <p>前端在会话中通过 {@code get_structure} 回写真实页面 DOM 的可交互字段清单，
 * 后端把这份"真实结构"按 {@code scene + sessionKey} 缓存起来。LLM 通过
 * {@code formfill_get_structure} 获取这些真实页面字段后产出映射好的 formData，
 * 服务端只管透传下发，不做任何字段映射或值翻译。
 *
 * <p>设计要点：
 * <ul>
 *   <li>内存存储 + 会话维度隔离，天然无持久化负担；多标签页用 requestId/sessionKey 区分。</li>
 *   <li>结构可随时刷新（同一 sessionKey 再次回写即覆盖），适配 SPA 路由切换。</li>
 *   <li>线程安全：ConcurrentHashMap + 每段用 synchronized 保护列表替换。</li>
 * </ul>
 */
@Slf4j
@Component
public class PageStructureRegistry {

    /** scene -> (sessionKey -> 字段清单) */
    private final Map<String, Map<String, List<Map<String, Object>>>> store = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("页面结构注册表已初始化（内存态，按 scene+sessionKey 隔离）");
    }

    /**
     * 注册（或刷新）某会话的页面结构。
     *
     * @param scene      场景标识（对应 scenarios/*.json 的 id）
     * @param sessionKey 会话标识（如 requestId / 页面实例标识），为空时归到默认 "__default__"
     * @param fields     前端回写的字段清单，元素形如 {index,name,label,type}
     */
    public void register(String scene, String sessionKey, List<Map<String, Object>> fields) {
        if (scene == null || scene.isBlank()) scene = "__default__";
        if (sessionKey == null || sessionKey.isBlank()) sessionKey = "__default__";
        if (fields == null) fields = new ArrayList<>();

        Map<String, List<Map<String, Object>>> sceneMap =
                store.computeIfAbsent(scene, k -> new ConcurrentHashMap<>());
        synchronized (sceneMap) {
            sceneMap.put(sessionKey, new ArrayList<>(fields));
        }
        log.info("已注册页面结构: scene={}, session={}, 字段数={}", scene, sessionKey, fields.size());
    }

    /** 从前端回写的 FormFillMessage 中提取结构并注册（type=get_structure 分支） */
    public void registerFromMessage(FormFillMessage msg) {
        if (msg == null) return;
        List<Map<String, Object>> fields = extractFields(msg);
        String sessionKey = extractSessionKey(msg);
        register(msg.getScene(), sessionKey, fields);
    }

    /**
     * 取得某场景最新（最近注册的 session）的真实页面结构；不存在返回空列表。
     * 取"最近写入"的 session：遍历该 scene 下所有 session，取字段数最多者（更完整）。
     */
    public List<Map<String, Object>> getLatest(String scene) {
        if (scene == null) return new ArrayList<>();
        Map<String, List<Map<String, Object>>> sceneMap = store.get(scene);
        if (sceneMap == null || sceneMap.isEmpty()) return new ArrayList<>();

        List<Map<String, Object>> best = null;
        synchronized (sceneMap) {
            for (List<Map<String, Object>> list : sceneMap.values()) {
                if (best == null || list.size() > best.size()) best = list;
            }
        }
        return best != null ? new ArrayList<>(best) : new ArrayList<>();
    }

    /** 是否存在真实页面结构（用于决定下发时是否走真实结构校准） */
    public boolean hasStructure(String scene) {
        List<Map<String, Object>> s = getLatest(scene);
        return s != null && !s.isEmpty();
    }

    /** 清空（测试或会话销毁时调用） */
    public void clear(String scene) {
        if (scene != null) store.remove(scene);
    }

    public void clearAll() {
        store.clear();
    }

    // ── 内部工具 ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFields(FormFillMessage msg) {
        if (msg.getFormData() != null && msg.getFormData().containsKey("fields")) {
            Object f = msg.getFormData().get("fields");
            if (f instanceof List) return (List<Map<String, Object>>) f;
        }
        if (msg.getResult() != null && msg.getResult().getMessage() != null) {
            // 兼容前端把 fields 放在 result.message 的 JSON 字符串里的旧约定
            try {
                Object parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(msg.getResult().getMessage(), Map.class);
                if (parsed instanceof Map && ((Map<?, ?>) parsed).containsKey("fields")) {
                    Object f = ((Map<?, ?>) parsed).get("fields");
                    if (f instanceof List) return (List<Map<String, Object>>) f;
                }
            } catch (Exception ignored) {
                // 非 JSON，忽略
            }
        }
        return new ArrayList<>();
    }

    private String extractSessionKey(FormFillMessage msg) {
        if (msg.getParams() != null) {
            Object rid = msg.getParams().get("requestId");
            if (rid != null && !String.valueOf(rid).isBlank()) return String.valueOf(rid);
        }
        return "__default__";
    }
}
