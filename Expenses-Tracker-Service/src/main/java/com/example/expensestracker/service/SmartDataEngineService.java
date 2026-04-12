package com.example.expensestracker.service;

import com.example.expensestracker.model.TrackerType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class SmartDataEngineService {

    @Autowired
    private ObjectMapper objectMapper;

    public ObjectNode parseTabularData(MultipartFile file, String trackerName) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<String[]> dataRows = getRawRows(file);

        if (dataRows.isEmpty()) {
            throw new RuntimeException("The file is empty or could not be parsed as tabular data.");
        }

        return inferSchemaAndExtract(dataRows, trackerName != null ? trackerName : stripExtension(filename));
    }

    public List<String[]> getRawRows(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".csv")) {
            try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                return reader.readAll();
            }
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return parseExcel(file.getInputStream());
        }
        return Collections.emptyList();
    }

    private List<String[]> parseExcel(InputStream is) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0); // Process only the first sheet
            for (Row row : sheet) {
                int lastCellNum = row.getLastCellNum();
                String[] rowData = new String[lastCellNum];
                for (int i = 0; i < lastCellNum; i++) {
                    Cell cell = row.getCell(i);
                    rowData[i] = (cell == null) ? "" : formatCellValue(cell);
                }
                rows.add(rowData);
            }
        }
        return rows;
    }

    private String formatCellValue(Cell cell) {
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }

    private ObjectNode inferSchemaAndExtract(List<String[]> dataRows, String name) {
        String[] headers = dataRows.get(0);
        List<String[]> rows = dataRows.subList(1, dataRows.size());

        ObjectNode result = objectMapper.createObjectNode();
        result.put("name", name);
        result.put("type", inferTrackerType(headers).toString());
        result.put("icon", "📊");

        ArrayNode fieldDefs = result.putArray("fieldDefinitions");
        Map<Integer, String> colTypes = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            if (header.isEmpty()) header = "Column_" + (i + 1);
            
            String detectedType = inferFieldType(rows, i);
            colTypes.put(i, detectedType);

            ObjectNode field = fieldDefs.addObject();
            field.put("name", header);
            field.put("type", detectedType);
        }

        ArrayNode entries = result.putArray("entries");
        for (String[] row : rows) {
            ObjectNode entry = entries.addObject();
            for (int i = 0; i < headers.length && i < row.length; i++) {
                String header = headers[i].trim();
                if (header.isEmpty()) header = "Column_" + (i + 1);
                
                String val = row[i].trim();
                String type = colTypes.get(i);

                if (val.isEmpty() || val.equalsIgnoreCase("null") || val.equalsIgnoreCase("undefined")) {
                    entry.putNull(header);
                    continue;
                }

                try {
                    if ("NUMBER".equals(type) || "CURRENCY".equals(type)) {
                        String cleanVal = val.replace("$", "").replace(",", "").trim();
                        entry.put(header, Double.parseDouble(cleanVal));
                    } else if ("BOOLEAN".equals(type)) {
                        entry.put(header, Boolean.parseBoolean(val));
                    } else {
                        entry.put(header, val);
                    }
                } catch (Exception e) {
                    entry.put(header, val);
                }
            }
        }

        return result;
    }

    private String inferFieldType(List<String[]> rows, int colIndex) {
        int count = 0;
        int numericCount = 0;
        int currencyCount = 0;
        int dateCount = 0;
        int booleanCount = 0;

        int limit = Math.min(rows.size(), 10);
        for (int i = 0; i < limit; i++) {
            String val = rows.get(i).length > colIndex ? rows.get(i)[colIndex].trim() : "";
            if (val.isEmpty()) continue;
            count++;

            if (val.matches("-?\\d+(\\.\\d+)?")) numericCount++;
            if (val.contains("$") && val.replaceAll("[^0-9.]", "").length() > 0) currencyCount++;
            if (val.matches("\\d{1,4}[-/.]\\d{1,2}[-/.]\\d{1,4}.*")) dateCount++;
            if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false") || val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("no")) booleanCount++;
        }

        if (count == 0) return "TEXT";
        if (currencyCount > count / 2) return "CURRENCY";
        if (numericCount > count / 2) return "NUMBER";
        if (dateCount > count / 2) return "DATE";
        if (booleanCount > count / 2) return "BOOLEAN";
        
        return "TEXT";
    }

    private TrackerType inferTrackerType(String[] headers) {
        String combined = String.join(" ", headers).toLowerCase();
        if (combined.contains("price") || combined.contains("cost") || combined.contains("amount") || combined.contains("spend")) return TrackerType.FINANCE;
        if (combined.contains("weight") || combined.contains("steps") || combined.contains("health") || combined.contains("bpm")) return TrackerType.HEALTH;
        if (combined.contains("symbol") || combined.contains("ticker") || combined.contains("shares")) return TrackerType.STOCK;
        return TrackerType.CUSTOM;
    }

    private String stripExtension(String filename) {
        int pos = filename.lastIndexOf(".");
        if (pos == -1) return filename;
        return filename.substring(0, pos);
    }
}
