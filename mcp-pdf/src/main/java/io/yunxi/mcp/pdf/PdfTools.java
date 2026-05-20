package io.yunxi.mcp.pdf;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * PDF 处理工具
 */
@Slf4j
@Component
public class PdfTools {

    private final PdfConfig config;

    @Autowired
    public PdfTools(PdfConfig config) {
        this.config = config;
    }

    /**
     * PDF 操作工具
     */
    public static class PdfTool implements ToolHandler {

        private final PdfTools parent;

        @Autowired
        public PdfTool(PdfTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("pdf_operation")
                    .description(
                            "Process PDF files: extract text, get info, merge, split, rotate pages, encrypt, decrypt. " +
                                    "处理 PDF 文件：提取文本、获取信息、合并、拆分、旋转页面、加密、解密。 " +
                                    "Use this when you need to: read PDF content, combine multiple PDFs, extract specific pages, or protect sensitive documents. " +
                                    "适用于：文档阅读、报告合并、页面提取、文档保护等场景。 " +
                                    "Common use cases: document reading, report merging, page extraction, document protection. " +
                                    "典型用例：文档阅读、报告合并、页面提取、文档保护。")
                    .inputSchema(schema(
                            "action", "string", "Operation type: info, extract_text, merge, split, rotate, encrypt, decrypt | 操作类型",
                            "input_path", "string", "Input PDF file path | 输入 PDF 文件路径",
                            "output_path", "string", "Output file path (optional) | 输出文件路径",
                            "pages", "string", "Page range like '1-5' or '1,3,5' (optional) | 页面范围",
                            "degrees", "integer", "Rotation degrees: 90, 180, 270 (for rotate) | 旋转角度",
                            "password", "string", "Password for encrypt/decrypt | 加密/解密密码",
                            "input_files", "array", "Input files for merge (array of paths) | 合并的输入文件列表"))
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String action = (String) args.getOrDefault("action", "info");
            String inputPath = (String) args.get("input_path");

            if (inputPath == null || inputPath.isBlank()) {
                return ToolResult.error("Error: input_path is required");
            }

            try {
                return switch (action) {
                    case "info" -> parent.getPdfInfo(inputPath);
                    case "extract_text" -> parent.extractText(inputPath, (String) args.get("pages"));
                    case "merge" -> parent.mergePdfs(args);
                    case "split" -> parent.splitPdf(inputPath, (String) args.get("pages"), (String) args.get("output_path"));
                    case "rotate" -> parent.rotatePdf(inputPath, (Integer) args.get("degrees"), (String) args.get("pages"), (String) args.get("output_path"));
                    case "encrypt" -> parent.encryptPdf(inputPath, (String) args.get("password"), (String) args.get("output_path"));
                    case "decrypt" -> parent.decryptPdf(inputPath, (String) args.get("password"), (String) args.get("output_path"));
                    default -> ToolResult.error("Error: Unknown action: " + action);
                };
            } catch (Exception e) {
                log.error("PDF operation error: {}", e.getMessage(), e);
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 获取 PDF 信息
     */
    public ToolResult getPdfInfo(String inputPath) {
        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            Map<String, Object> info = new HashMap<>();
            info.put("file", inputPath);
            info.put("pages", document.getNumberOfPages());
            info.put("encrypted", document.isEncrypted());

            if (document.getDocumentInformation() != null) {
                var meta = document.getDocumentInformation();
                Map<String, String> metadata = new HashMap<>();
                if (meta.getTitle() != null) metadata.put("title", meta.getTitle());
                if (meta.getAuthor() != null) metadata.put("author", meta.getAuthor());
                if (meta.getSubject() != null) metadata.put("subject", meta.getSubject());
                if (meta.getCreator() != null) metadata.put("creator", meta.getCreator());
                if (meta.getCreationDate() != null) metadata.put("creationDate", meta.getCreationDate().toString());
                info.put("metadata", metadata);
            }

            return ToolResult.text(formatPdfInfo(info));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot read PDF - " + e.getMessage());
        }
    }

    private String formatPdfInfo(Map<String, Object> info) {
        StringBuilder sb = new StringBuilder();
        sb.append("📄 PDF Information\n");
        sb.append("───────────────\n");
        sb.append("File: ").append(info.get("file")).append("\n");
        sb.append("Pages: ").append(info.get("pages")).append("\n");
        sb.append("Encrypted: ").append(info.get("encrypted")).append("\n");

        @SuppressWarnings("unchecked")
        Map<String, String> metadata = (Map<String, String>) info.get("metadata");
        if (metadata != null && !metadata.isEmpty()) {
            sb.append("Metadata:\n");
            metadata.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        }
        return sb.toString();
    }

    /**
     * 提取文本
     */
    public ToolResult extractText(String inputPath, String pages) {
        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            PDFTextStripper stripper = new PDFTextStripper();

            if (pages != null && !pages.isBlank()) {
                int[] range = parsePageRange(pages, document.getNumberOfPages());
                stripper.setStartPage(range[0]);
                stripper.setEndPage(range[1]);
            }

            String text = stripper.getText(document);
            return ToolResult.text("📝 Extracted Text:\n" + text);
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot extract text - " + e.getMessage());
        }
    }

    /**
     * 合并 PDF
     */
    @SuppressWarnings("unchecked")
    public ToolResult mergePdfs(Map<String, Object> args) {
        List<String> inputFiles = (List<String>) args.get("input_files");
        String outputPath = (String) args.getOrDefault("output_path", "merged.pdf");

        if (inputFiles == null || inputFiles.isEmpty()) {
            return ToolResult.error("Error: input_files is required for merge");
        }

        try (PDDocument merged = new PDDocument()) {
            for (String filePath : inputFiles) {
                try (PDDocument doc = PDDocument.load(new File(filePath))) {
                    for (PDPage page : doc.getPages()) {
                        merged.addPage(page);
                    }
                }
            }
            merged.save(outputPath);
            return ToolResult.text(String.format("✅ Merged %d PDF files into: %s", inputFiles.size(), outputPath));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot merge PDFs - " + e.getMessage());
        }
    }

    /**
     * 拆分 PDF
     */
    public ToolResult splitPdf(String inputPath, String pages, String outputPath) {
        String outPath = outputPath != null ? outputPath : "split.pdf";

        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            int[] range = parsePageRange(pages, document.getNumberOfPages());
            PDDocument splitDoc = new PDDocument();

            for (int i = range[0] - 1; i < range[1]; i++) {
                splitDoc.addPage(document.getPage(i));
            }

            splitDoc.save(outPath);
            return ToolResult.text(String.format("✅ Extracted %d pages to: %s", range[1] - range[0] + 1, outPath));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot split PDF - " + e.getMessage());
        }
    }

    /**
     * 旋转 PDF
     */
    public ToolResult rotatePdf(String inputPath, Integer degrees, String pages, String outputPath) {
        int rot = degrees != null ? degrees : 90;
        String outPath = outputPath != null ? outputPath : "rotated.pdf";

        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            int totalPages = document.getNumberOfPages();
            int[] range = parsePageRange(pages, totalPages);

            for (int i = range[0] - 1; i < range[1]; i++) {
                PDPage page = document.getPage(i);
                int currentRotation = page.getRotation();
                page.setRotation((currentRotation + rot) % 360);
            }

            document.save(outPath);
            return ToolResult.text(String.format("✅ Rotated %d pages by %d degrees, saved to: %s", range[1] - range[0] + 1, rot, outPath));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot rotate PDF - " + e.getMessage());
        }
    }

    /**
     * 加密 PDF
     */
    public ToolResult encryptPdf(String inputPath, String password, String outputPath) {
        if (password == null || password.isBlank()) {
            return ToolResult.error("Error: password is required for encryption");
        }

        String outPath = outputPath != null ? outputPath : "encrypted.pdf";

        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password,
                    new AccessPermission());
            document.protect(policy);
            document.save(outPath);
            return ToolResult.text(String.format("✅ PDF encrypted and saved to: %s", outPath));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot encrypt PDF - " + e.getMessage());
        }
    }

    /**
     * 解密 PDF
     */
    public ToolResult decryptPdf(String inputPath, String password, String outputPath) {
        if (password == null || password.isBlank()) {
            return ToolResult.error("Error: password is required for decryption");
        }

        String outPath = outputPath != null ? outputPath : "decrypted.pdf";

        try (PDDocument document = PDDocument.load(new File(inputPath), password)) {
            document.save(outPath);
            return ToolResult.text(String.format("✅ PDF decrypted and saved to: %s", outPath));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot decrypt PDF - " + e.getMessage());
        }
    }

    /**
     * 解析页面范围
     */
    private int[] parsePageRange(String pages, int totalPages) {
        if (pages == null || pages.isBlank()) {
            return new int[]{1, totalPages};
        }

        if (pages.contains("-")) {
            String[] parts = pages.split("-");
            int start = Math.max(1, Integer.parseInt(parts[0].trim()));
            int end = Math.min(totalPages, Integer.parseInt(parts[1].trim()));
            return new int[]{start, end};
        } else if (pages.contains(",")) {
            String[] parts = pages.split(",");
            int start = Math.max(1, Integer.parseInt(parts[0].trim()));
            int end = Math.min(totalPages, Integer.parseInt(parts[parts.length - 1].trim()));
            return new int[]{start, end};
        } else {
            int page = Integer.parseInt(pages.trim());
            return new int[]{page, page};
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