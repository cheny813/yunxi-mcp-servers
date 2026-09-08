package io.yunxi.mcp.nutrition.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.nutrition.sync.SchoolDishSyncRunner;
import io.yunxi.mcp.nutrition.sync.SyncEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据同步工具 —— 统一入口，按业务场景分发到不同的同步执行器。
 *
 * <h3>同步架构</h3>
 * <p>mcp-nutrition 提供两条独立的同步路径：</p>
 * <ol>
 *   <li><b>管道同步</b>（{@code type=管道名}）—— 由 sync-pipelines.yml 声明式配置，
 *       适合新增数据源、快速定义同步规则，无需改代码。</li>
 *   <li><b>菜品同步</b>（{@code type=dishes}）—— 走 {@link SchoolDishSyncRunner}，
 *       处理带量食谱特殊表结构，适合已有完整菜品数据表的场景。</li>
 *   <li><b>一键同步</b>（{@code type=all}）—— 执行全部启用管道。</li>
 * </ol>
 */
@Component
public class SyncTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(SyncTool.class);

    private final SyncEngine syncEngine;
    private final SchoolDishSyncRunner dishSyncRunner;

    public SyncTool(SyncEngine syncEngine, SchoolDishSyncRunner dishSyncRunner) {
        this.syncEngine = syncEngine;
        this.dishSyncRunner = dishSyncRunner;
    }

    @Override
    public String getName() {
        return "sync_data";
    }

    @Override
    public String getDescription() {
        return "同步 MySQL 数据到 Milvus 向量库。参数 type 可选：管道名 / dishes / all。type=dishes 时可传 schoolId 按需同步单个学校菜品集合";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "type", Map.of("type", "string",
                                "description", "同步类型：管道名（如 dish-library-sync）/ dishes / all（默认）"),
                        "schoolId", Map.of("type", "integer",
                                "description", "学校ID，仅 type=dishes 时生效。传入则只同步该学校菜品；不传则全量同步所有学校")),
                "required", List.of());
    }

    @Override
    public String execute(Map<String, Object> args) {
        String type = (String) args.getOrDefault("type", "all");
        log.info("[SyncTool] 执行同步: type={}", type);

        return switch (type) {
            case "dishes" -> {
                Object schoolIdObj = args.get("schoolId");
                if (schoolIdObj != null) {
                    long schoolId = ((Number) schoolIdObj).longValue();
                    log.info("[SyncTool] 按需同步学校 {} 菜品", schoolId);
                    dishSyncRunner.syncSchoolDishes(schoolId);
                    yield "学校 " + schoolId + " 菜品数据同步已触发，请查看服务日志获取进度";
                }
                dishSyncRunner.syncAllSchoolDishes();
                yield "全量菜品数据同步已触发（共N所学校），请查看服务日志获取进度";
            }
            case "all" -> {
                syncEngine.syncAll();
                yield "一键同步已触发，所有管道正在执行中，请查看服务日志获取进度";
            }
            default -> {
                syncEngine.syncPipeline(type);
                yield "管道 [" + type + "] 同步已触发，请查看服务日志获取进度";
            }
        };
    }
}
