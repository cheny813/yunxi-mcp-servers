package io.yunxi.mcp.formfill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 场景配置加载器。
 *
 * 从 classpath 下的 {@code scenarios/*.json} 加载所有场景配置，
 * 部署时按需放入场景 JSON 即可，无需改动代码。
 */
@Slf4j
@Component
public class SceneConfigLoader {

    private static final String SCENARIO_LOCATION = "classpath*:scenarios/*.json";

    private final Map<String, SceneConfig> sceneConfigs = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void load() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(SCENARIO_LOCATION);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    SceneConfig config = objectMapper.readValue(is, SceneConfig.class);
                    if (config.getId() == null || config.getId().isBlank()) {
                        log.warn("跳过场景配置（缺少 id）: {}", resource.getFilename());
                        continue;
                    }
                    sceneConfigs.put(config.getId(), config);
                    log.info("已加载场景配置: {} ({})", config.getId(), config.getName());
                } catch (IOException e) {
                    log.warn("加载场景配置失败: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.error("扫描场景配置目录失败", e);
        }
        if (sceneConfigs.isEmpty()) {
            log.warn("未加载到任何场景配置，formfill_fill 将无法识别任何场景");
        }
    }

    public Map<String, SceneConfig> getSceneConfigs() {
        return sceneConfigs;
    }

    public SceneConfig getSceneConfig(String id) {
        return sceneConfigs.get(id);
    }

    public boolean containsScene(String id) {
        return sceneConfigs.containsKey(id);
    }

    public List<String> listSceneIds() {
        return new ArrayList<>(sceneConfigs.keySet());
    }
}
