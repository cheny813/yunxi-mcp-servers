package io.yunxi.mcp.pptx;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.util.ToolResult;
import lombok.extern.slf4j.Slf4j;

/**
 * PowerPoint 处理工具
 */
@Slf4j
@Component
public class PptxTools {

    private final PptxConfig config;

    @Autowired
    public PptxTools(PptxConfig config) {
        this.config = config;
    }

    /**
     * PowerPoint 操作工具
     */
    public static class PptxTool implements ToolHandler {

        private final PptxTools parent;

        @Autowired
        public PptxTool(PptxTools parent) {
            this.parent = parent;
        }

        @Override


        public String getName() {


            return "pptx_operation";


        }



        @Override


        public String getDescription() {


            return "Process PowerPoint files: read, info, create, add_slide. " +
                                    "处理 PowerPoint 文件：读取、获取信息、创建、添加幻灯片。 " +
                                    "Use this when you need to read presentation content, create new slides, " +
                                    "or build presentations programmatically. " +
                                    "适用于读取演示文稿内容、创建新幻灯片、程序化构建演示文稿等场景。 " +
                                    "Common use cases: presentation reading, slide creation, report conversion, meeting preparation. " +
                                    "典型用例：演示文稿阅读、幻灯片创建、报告转换、会议准备。";


        }



        @Override


        @SuppressWarnings("unchecked")


        public Map<String, Object> getInputSchema() {


            return (Map<String, Object>) (Map) schema(
                            "action", "string", "Operation: read, info, create, add_slide",
                            "file_path", "string", "PowerPoint file path | PPTX 文件路径",
                            "slide_index", "integer", "Slide index (0-based) | 幻灯片索引",
                            "title", "string", "Slide title | 幻灯片标题",
                            "content", "string", "Slide content | 幻灯片内容",
                            "output_path", "string", "Output file path | 输出文件路径");


        }


        @Override
        public String execute(Map<String, Object> args) {
            String action = (String) args.getOrDefault("action", "info");
            String filePath = (String) args.get("file_path");

            if (filePath == null || filePath.isBlank()) {
                throw new RuntimeException("Error: file_path is required");
            }

            try {
                return switch (action) {
                    case "info" -> parent.getPptxInfo(filePath);
                    case "read" -> parent.readPptx(args);
                    case "create" -> parent.createPptx(args);
                    case "add_slide" -> parent.addSlide(args);
                    default -> ToolResult.error("Error: Unknown action: " + action);
                };
            } catch (Exception e) {
                log.error("PowerPoint operation error: {}", e.getMessage(), e);
                throw new RuntimeException("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 获取 PowerPoint 信息
     */
    public String getPptxInfo(String filePath) {
        try (XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(filePath))) {
            Map<String, Object> info = new HashMap<>();
            info.put("file", filePath);
            info.put("slide_count", ppt.getSlides().size());

            return (formatPptxInfo(info));
        } catch (IOException e) {
            throw new RuntimeException("Error: Cannot read PowerPoint file - " + e.getMessage());
        }
    }

    private String formatPptxInfo(Map<String, Object> info) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 PowerPoint Information\n");
        sb.append("───────────────\n");
        sb.append("File: ").append(info.get("file")).append("\n");
        sb.append("Slides: ").append(info.get("slide_count")).append("\n");
        return sb.toString();
    }

    /**
     * 读取 PowerPoint 幻灯片
     */
    public String readPptx(Map<String, Object> args) {
        String filePath = (String) args.get("file_path");
        Integer slideIndex = args.get("slide_index") != null ? ((Number) args.get("slide_index")).intValue() : null;

        try (XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(filePath))) {
            List<XSLFSlide> slides = ppt.getSlides();

            if (slideIndex != null) {
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    throw new RuntimeException("Error: Slide index out of range");
                }
                return (readSlideContent(slides.get(slideIndex), slideIndex));
            }

            // 读取所有幻灯片
            StringBuilder sb = new StringBuilder();
            sb.append("📊 PowerPoint Slides (Total: ").append(slides.size()).append("):\n\n");

            for (int i = 0; i < slides.size(); i++) {
                sb.append(readSlideContent(slides.get(i), i));
                sb.append("\n");
            }

            return (sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Error: Cannot read PowerPoint - " + e.getMessage());
        }
    }

    private String readSlideContent(XSLFSlide slide, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Slide ").append(index + 1).append(" ---\n");

        // 读取文本框内容
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextBox) {
                XSLFTextBox textBox = (XSLFTextBox) shape;
                String text = textBox.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 创建新 PowerPoint
     */
    public String createPptx(Map<String, Object> args) {
        String outputPath = (String) args.getOrDefault("output_path", (String) args.get("file_path"));

        if (outputPath == null || outputPath.isBlank()) {
            throw new RuntimeException("Error: output_path or file_path is required");
        }

        try (XMLSlideShow ppt = new XMLSlideShow()) {
            // 创建一个空白幻灯片
            XSLFSlide slide = ppt.createSlide();

            ppt.write(new FileOutputStream(outputPath));
            return (String.format("✅ PowerPoint file created: %s", outputPath));
        } catch (IOException e) {
            throw new RuntimeException("Error: Cannot create PowerPoint - " + e.getMessage());
        }
    }

    /**
     * 添加幻灯片
     */
    public String addSlide(Map<String, Object> args) {
        String filePath = (String) args.get("file_path");
        String title = (String) args.get("title");
        String content = (String) args.get("content");
        String outputPath = (String) args.getOrDefault("output_path", filePath);

        if (filePath == null || filePath.isBlank()) {
            throw new RuntimeException("Error: file_path is required");
        }

        try (XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(filePath))) {
            // 创建新幻灯片
            XSLFSlide slide = ppt.createSlide();

            // 添加标题文本框
            if (title != null && !title.isBlank()) {
                XSLFTextBox titleBox = slide.createTextBox();
                titleBox.setText(title);
            }

            // 添加内容文本框
            if (content != null && !content.isBlank()) {
                XSLFTextBox contentBox = slide.createTextBox();
                contentBox.setText(content);
            }

            ppt.write(new FileOutputStream(outputPath));
            return (String.format("✅ Slide added to: %s", outputPath));
        } catch (IOException e) {
            throw new RuntimeException("Error: Cannot add slide - " + e.getMessage());
        }
    }

    /**
     * 构建输入参数 Schema
     */
    private static Map<String, Object> schema(String... props) {
        Map<String, Object> s = new HashMap<>();
        s.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < props.length; i += 3) {
            Map<String, Object> p = new HashMap<>();
            p.put("type", props[i + 1]);
            p.put("description", props[i + 2]);
            properties.put(props[i], p);
        }
        s.put("properties", properties);
        return s;
    }
}