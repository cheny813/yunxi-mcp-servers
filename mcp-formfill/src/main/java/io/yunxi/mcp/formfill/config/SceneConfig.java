package io.yunxi.mcp.formfill.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场景配置：表单场景的纯身份标识。
 *
 * 服务端不做字段映射、值翻译、也不做任何迭代/批处理——这些全部由上游 LLM
 * 根据 {@code formfill_get_structure} 返回的真实页面结构（liveFields）自行完成。
 * 场景配置仅用于命名空间隔离，不同场景之间的表单互不干扰。
 *
 * 该配置从 classpath 下的 scenarios/*.json 加载，
 * 部署时按需放入场景 JSON 即可，无需改动任何 Java 代码。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneConfig {

    /** 场景唯一标识 */
    private String id;

    /** 场景的展示名称 */
    private String name;

    /** 场景描述 */
    private String description;
}
