package io.yunxi.mcp.chart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源控制器
 * <p>
 * 提供图表图片的访问接口。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/charts")
public class StaticController {

    @Value("${chart.output-dir:./charts}")
    private String outputDir;

    /**
     * 获取图表图片
     *
     * @param fileName 文件名
     * @return 图片资源
     */
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getChart(@PathVariable String fileName) {
        log.debug("访问图表文件: {}", fileName);

        try {
            Path filePath = Paths.get(outputDir, fileName).normalize();

            // 安全检查：确保文件在输出目录内
            if (!filePath.startsWith(Paths.get(outputDir).normalize())) {
                log.warn("非法访问路径: {}", filePath);
                return ResponseEntity.badRequest().build();
            }

            if (!Files.exists(filePath)) {
                log.warn("图表文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);

            // 根据文件扩展名设置 Content-Type
            String contentType = determineContentType(fileName);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(resource.contentLength());
            headers.setCacheControl("public, max-age=3600");

            log.debug("返回图表文件: {}, size={} bytes", fileName, resource.contentLength());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            log.error("读取图表文件失败: {}, error={}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 确定文件 Content-Type
     *
     * @param fileName 文件名
     * @return Content-Type
     */
    private String determineContentType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerCaseFileName.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerCaseFileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            return "application/octet-stream";
        }
    }
}
