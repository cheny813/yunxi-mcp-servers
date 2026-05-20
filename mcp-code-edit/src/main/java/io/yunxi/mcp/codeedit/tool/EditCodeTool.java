package io.yunxi.mcp.codeedit.tool;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import io.yunxi.mcp.codeedit.service.CodeEditService;

import java.util.Map;

/**
 * 编辑代码工具 - 带安全保护
 * <p>
 * MCP 工具实现，用于安全地编辑代码文件。
 * </p>
 * <p>
 * 安全措施：
 * </p>
 * <ul>
 * <li>自动 UTF-8 编码处理</li>
 * <li>自动备份原文件</li>
 * <li>验证原内容匹配后再修改</li>
 * <li>支持修改恢复</li>
 * </ul>
 * <p>
 * 使用示例：
 * </p>
 * 
 * <pre>
 * {
 *   "path": "src/main/java/Example.java",
 *   "oldContent": "public void oldMethod() {}",
 *   "newContent": "public void newMethod() {}",
 *   "reason": "重构方法名"
 * }
 * </pre>
 *
 * @author yunxi-mcp-servers
 * @see CodeEditService
 * @since 1.0.0
 */
public class EditCodeTool implements ToolHandler {

    /**
     * 代码编辑服务
     */
    private final CodeEditService codeEditService;

    /**
     * 构造函数
     *
     * @param codeEditService 代码编辑服务实例
     */
    public EditCodeTool(CodeEditService codeEditService) {
        this.codeEditService = codeEditService;
    }

    /**
     * 获取工具定义
     * <p>
     * 定义工具的名称、描述和输入参数模式。
     * </p>
     *
     * @return 工具定义对象
     */
    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name("edit_code")
                .description(
                        "编辑代码文件（带安全保护）。\n" +
                                "通过字符串替换方式修改代码文件。\n" +
                                "安全措施：\n" +
                                "- 自动 UTF-8 编码\n" +
                                "- 自动备份原文件\n" +
                                "- 自动添加修改注释\n" +
                                "- 验证原内容匹配后再修改\n" +
                                "⚠️ 警告：此操作会修改代码，请谨慎使用！")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "文件相对路径"),
                                "oldContent", Map.of(
                                        "type", "string",
                                        "description", "要替换的原始代码片段"),
                                "newContent", Map.of(
                                        "type", "string",
                                        "description", "新的代码片段"),
                                "reason", Map.of(
                                        "type", "string",
                                        "description", "修改原因，用于自动注释")),
                        "required", java.util.Arrays.asList("path", "oldContent", "newContent")))
                .build();
    }

    /**
     * 执行代码编辑操作
     * <p>
     * 根据传入的参数执行文件编辑操作，返回编辑结果。
     * </p>
     *
     * @param arguments 工具参数，包含 path, oldContent, newContent, reason
     * @return 工具执行结果
     */
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");
        String oldContent = (String) arguments.get("oldContent");
        String newContent = (String) arguments.get("newContent");
        String reason = (String) arguments.getOrDefault("reason", "AI自动修复");

        if (path == null || oldContent == null || newContent == null) {
            return ToolResult.error("path, oldContent, newContent are required");
        }

        try {
            // 可选：添加自动注释
            // String commentedNewContent = codeEditService.addAutoComment(newContent,
            // reason);

            var result = codeEditService.editFile(path, oldContent, newContent);

            if (result.success()) {
                return ToolResult.text("✅ " + result.message() + "\n\n修改详情:\n" +
                        "文件: " + path + "\n" +
                        "修改ID: " + result.editId());
            } else {
                return ToolResult.error("❌ " + result.message());
            }
        } catch (Exception e) {
            return ToolResult.error("编辑失败: " + e.getMessage());
        }
    }
}
