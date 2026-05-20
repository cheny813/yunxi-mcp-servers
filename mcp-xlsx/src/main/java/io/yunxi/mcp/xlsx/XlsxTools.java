package io.yunxi.mcp.xlsx;

import io.yunxi.mcp.common.handler.ToolHandler;
import io.yunxi.mcp.common.model.ToolDefinition;
import io.yunxi.mcp.common.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * Excel 处理工具
 */
@Slf4j
@Component
public class XlsxTools {

    private final XlsxConfig config;

    @Autowired
    public XlsxTools(XlsxConfig config) {
        this.config = config;
    }

    /**
     * Excel 操作工具
     */
    public static class XlsxTool implements ToolHandler {

        private final XlsxTools parent;

        @Autowired
        public XlsxTool(XlsxTools parent) {
            this.parent = parent;
        }

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                    .name("xlsx_operation")
                    .description(
                            "Process Excel files: read, write, create, info, csv_to_xlsx, xlsx_to_csv. " +
                                    "处理 Excel 文件：读取、写入、创建、获取信息、CSV 转换。 " +
                                    "Use this when you need to read spreadsheet data, create new Excel files, " +
                                    "convert between CSV and Excel formats, or perform data analysis. " +
                                    "适用于读取表格数据、创建 Excel 文件、CSV 与 Excel 格式转换、数据分析等场景。 " +
                                    "Common use cases: data analysis, report generation, spreadsheet manipulation, data import/export. " +
                                    "典型用例：数据分析、报表生成、电子表格操作、数据导入导出。")
                    .inputSchema(schema(
                            "action", "string", "Operation: read, write, create, info, csv_to_xlsx, xlsx_to_csv",
                            "file_path", "string", "Excel file path | Excel 文件路径",
                            "sheet_name", "string", "Sheet name (optional) | 工作表名称",
                            "data", "object", "Data to write (array of objects or 2D array) | 要写入的数据",
                            "range", "string", "Cell range like 'A1:D10' | 单元格范围",
                            "header", "boolean", "Has header row (default: true) | 是否有表头",
                            "output_path", "string", "Output file path | 输出文件路径"))
                    .build();
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            String action = (String) args.getOrDefault("action", "info");
            String filePath = (String) args.get("file_path");

            if (filePath == null || filePath.isBlank()) {
                return ToolResult.error("Error: file_path is required");
            }

            try {
                return switch (action) {
                    case "info" -> parent.getExcelInfo(filePath);
                    case "read" -> parent.readExcel(args);
                    case "write" -> parent.writeExcel(args);
                    case "create" -> parent.createExcel(args);
                    case "csv_to_xlsx" -> parent.csvToXlsx(args);
                    case "xlsx_to_csv" -> parent.xlsxToCsv(args);
                    default -> ToolResult.error("Error: Unknown action: " + action);
                };
            } catch (Exception e) {
                log.error("Excel operation error: {}", e.getMessage(), e);
                return ToolResult.error("Error: " + e.getMessage());
            }
        }
    }

    /**
     * 获取 Excel 信息
     */
    public ToolResult getExcelInfo(String filePath) {
        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            Map<String, Object> info = new HashMap<>();
            info.put("file", filePath);
            info.put("sheet_count", workbook.getNumberOfSheets());
            info.put("sheets", Arrays.asList(workbook.getSheetAt(0).getSheetName()));

            return ToolResult.text(formatExcelInfo(info));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot read Excel file - " + e.getMessage());
        }
    }

    private String formatExcelInfo(Map<String, Object> info) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Excel Information\n");
        sb.append("───────────────\n");
        sb.append("File: ").append(info.get("file")).append("\n");
        sb.append("Sheets: ").append(info.get("sheet_count")).append("\n");
        @SuppressWarnings("unchecked")
        List<String> sheets = (List<String>) info.get("sheets");
        if (sheets != null && !sheets.isEmpty()) {
            sb.append("Sheet Names: ").append(String.join(", ", sheets)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 读取 Excel
     */
    @SuppressWarnings("unchecked")
    public ToolResult readExcel(Map<String, Object> args) {
        String filePath = (String) args.get("file_path");
        String sheetName = (String) args.get("sheet_name");
        boolean header = (boolean) args.getOrDefault("header", true);
        String range = (String) args.get("range");

        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            Sheet sheet = sheetName != null ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);

            List<List<String>> rows = new ArrayList<>();

            if (range != null && !range.isBlank()) {
                rows = readRange(sheet, range);
            } else {
                rows = readAll(sheet, header);
            }

            return ToolResult.text(String.format("📖 Excel Data (%d rows):\n%s", rows.size(), formatData(rows)));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot read Excel - " + e.getMessage());
        }
    }

    private List<List<String>> readAll(Sheet sheet, boolean header) {
        List<List<String>> result = new ArrayList<>();
        Iterator<Row> iterator = sheet.iterator();

        boolean firstRow = true;
        List<String> headers = null;

        while (iterator.hasNext()) {
            Row row = iterator.next();
            List<String> rowData = new ArrayList<>();

            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                rowData.add(getCellValue(cell));
            }

            if (firstRow && header) {
                headers = rowData;
                firstRow = false;
                continue;
            }

            if (headers != null && rowData.size() < headers.size()) {
                while (rowData.size() < headers.size()) {
                    rowData.add("");
                }
            }

            if (header && headers != null) {
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    rowMap.put(headers.get(i), i < rowData.size() ? rowData.get(i) : "");
                }
                result.add(new ArrayList<>(rowMap.values()));
            } else {
                result.add(rowData);
            }
            firstRow = false;
        }

        return result;
    }

    private List<List<String>> readRange(Sheet sheet, String range) {
        List<List<String>> result = new ArrayList<>();
        String[] bounds = range.split(":");
        int startRow, endRow, startCol, endCol;

        if (bounds.length == 2) {
            org.apache.poi.ss.util.CellReference start = new org.apache.poi.ss.util.CellReference(bounds[0]);
            org.apache.poi.ss.util.CellReference end = new org.apache.poi.ss.util.CellReference(bounds[1]);
            startRow = start.getRow();
            endRow = end.getRow();
            startCol = start.getCol();
            endCol = end.getCol();
        } else {
            return result;
        }

        for (int r = startRow; r <= endRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            List<String> rowData = new ArrayList<>();
            for (int c = startCol; c <= endCol; c++) {
                Cell cell = row.getCell(c);
                rowData.add(cell != null ? getCellValue(cell) : "");
            }
            result.add(rowData);
        }

        return result;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private String formatData(List<List<String>> rows) {
        if (rows.isEmpty()) return "No data";
        StringBuilder sb = new StringBuilder();
        int maxCols = rows.stream().mapToInt(List::size).max().orElse(0);

        for (List<String> row : rows) {
            sb.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
        return sb.toString();
    }

    /**
     * 写入 Excel
     */
    @SuppressWarnings("unchecked")
    public ToolResult writeExcel(Map<String, Object> args) {
        String filePath = (String) args.get("file_path");
        String sheetName = (String) args.getOrDefault("sheet_name", "Sheet1");
        Object data = args.get("data");

        if (data == null) {
            return ToolResult.error("Error: data is required for write action");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            if (data instanceof List) {
                List<?> dataList = (List<?>) data;
                int rowNum = 0;

                for (Object item : dataList) {
                    Row row = sheet.createRow(rowNum++);
                    if (item instanceof List) {
                        List<?> rowData = (List<?>) item;
                        int colNum = 0;
                        for (Object cellData : rowData) {
                            createCell(row, colNum++, cellData);
                        }
                    } else if (item instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        int colNum = 0;
                        for (Object value : map.values()) {
                            createCell(row, colNum++, value);
                        }
                    } else {
                        createCell(row, 0, item);
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
            return ToolResult.text(String.format("✅ Data written to: %s", filePath));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot write Excel - " + e.getMessage());
        }
    }

    private void createCell(Row row, int col, Object value) {
        Cell cell = row.createCell(col);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 创建新 Excel
     */
    @SuppressWarnings("unchecked")
    public ToolResult createExcel(Map<String, Object> args) {
        String outputPath = (String) args.getOrDefault("output_path", (String) args.get("file_path"));
        String sheetName = (String) args.getOrDefault("sheet_name", "Sheet1");
        Object data = args.get("data");

        if (outputPath == null || outputPath.isBlank()) {
            return ToolResult.error("Error: output_path or file_path is required");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            if (data instanceof List) {
                List<?> dataList = (List<?>) data;
                int rowNum = 0;

                for (Object item : dataList) {
                    Row row = sheet.createRow(rowNum++);
                    if (item instanceof List) {
                        List<?> rowData = (List<?>) item;
                        int colNum = 0;
                        for (Object cellData : rowData) {
                            createCell(row, colNum++, cellData);
                        }
                    } else if (item instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        int colNum = 0;
                        for (Object value : map.values()) {
                            createCell(row, colNum++, value);
                        }
                    } else {
                        createCell(row, 0, item);
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            return ToolResult.text(String.format("✅ Excel file created: %s", outputPath));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot create Excel - " + e.getMessage());
        }
    }

    /**
     * CSV 转 Excel
     */
    public ToolResult csvToXlsx(Map<String, Object> args) {
        String csvPath = (String) args.get("file_path");
        String outputPath = (String) args.getOrDefault("output_path", csvPath.replace(".csv", ".xlsx"));

        if (csvPath == null || csvPath.isBlank()) {
            return ToolResult.error("Error: file_path is required");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath));
             Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Sheet1");
            String line;
            int rowNum = 0;

            while ((line = reader.readLine()) != null) {
                String[] cells = line.split(",");
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < cells.length; i++) {
                    createCell(row, i, cells[i].trim());
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            return ToolResult.text(String.format("✅ CSV converted to Excel: %s", outputPath));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot convert CSV to Excel - " + e.getMessage());
        }
    }

    /**
     * Excel 转 CSV
     */
    public ToolResult xlsxToCsv(Map<String, Object> args) {
        String xlsxPath = (String) args.get("file_path");
        String outputPath = (String) args.getOrDefault("output_path", xlsxPath.replace(".xlsx", ".csv"));
        String sheetName = (String) args.get("sheet_name");

        if (xlsxPath == null || xlsxPath.isBlank()) {
            return ToolResult.error("Error: file_path is required");
        }

        try (Workbook workbook = WorkbookFactory.create(new File(xlsxPath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {

            Sheet sheet = sheetName != null ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);

            for (Row row : sheet) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (Cell cell : row) {
                    if (!first) sb.append(",");
                    sb.append(getCellValue(cell).replace(",", ";"));
                    first = false;
                }
                writer.write(sb.toString());
                writer.newLine();
            }

            return ToolResult.text(String.format("✅ Excel converted to CSV: %s", outputPath));
        } catch (IOException e) {
            return ToolResult.error("Error: Cannot convert Excel to CSV - " + e.getMessage());
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