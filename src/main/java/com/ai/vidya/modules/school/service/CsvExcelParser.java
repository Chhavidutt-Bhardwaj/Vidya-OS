package com.ai.vidya.modules.school.service;

import com.ai.vidya.exception.BadRequestException;
import com.ai.vidya.modules.school.dto.request.BulkSchoolRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses CSV and Excel (.xlsx) bulk upload files into BulkSchoolRow objects.
 *<p>
 * Expected header row (case-insensitive, order-insensitive for Excel;
 * for CSV the order must match exactly):
 *<p>
 *   school_name | type | board | medium | udise_code | affiliation_no |
 *   principal_name | official_email | phone_primary |
 *   address_line1 | address_line2 | city | district | state | pincode |
 *   academic_year | chain_id | branch_code | branch_name | is_headquarter |
 *   admin_email | admin_full_name | admin_phone |
 *   established_year | trust_name | management_type | co_ed | residential
 *
 */
@Component
@Slf4j
public class CsvExcelParser {

    private static final int MAX_ROWS = 500;

    // Canonical column names — used for header-based Excel parsing
    public static final String COL_SCHOOL_NAME      = "school_name";
    public static final String COL_TYPE             = "type";
    public static final String COL_BOARD            = "board";
    public static final String COL_MEDIUM           = "medium";
    public static final String COL_UDISE_CODE       = "udise_code";
    public static final String COL_AFFILIATION_NO   = "affiliation_no";
    public static final String COL_PRINCIPAL_NAME   = "principal_name";
    public static final String COL_OFFICIAL_EMAIL   = "official_email";
    public static final String COL_PHONE_PRIMARY    = "phone_primary";
    public static final String COL_ADDRESS_LINE1    = "address_line1";
    public static final String COL_ADDRESS_LINE2    = "address_line2";
    public static final String COL_CITY             = "city";
    public static final String COL_DISTRICT         = "district";
    public static final String COL_STATE            = "state";
    public static final String COL_PINCODE          = "pincode";
    public static final String COL_ACADEMIC_YEAR    = "academic_year";
    public static final String COL_CHAIN_ID         = "chain_id";
    public static final String COL_BRANCH_CODE      = "branch_code";
    public static final String COL_BRANCH_NAME      = "branch_name";
    public static final String COL_IS_HEADQUARTER   = "is_headquarter";
    public static final String COL_ADMIN_EMAIL      = "admin_email";
    public static final String COL_ADMIN_FULL_NAME  = "admin_full_name";
    public static final String COL_ADMIN_PHONE      = "admin_phone";
    public static final String COL_ESTABLISHED_YEAR = "established_year";
    public static final String COL_TRUST_NAME       = "trust_name";
    public static final String COL_MANAGEMENT_TYPE  = "management_type";
    public static final String COL_CO_ED            = "co_ed";
    public static final String COL_RESIDENTIAL      = "residential";

    // ── Public entry point ────────────────────────────────────────────────

    public List<BulkSchoolRow> parse(MultipartFile file) {
        String filename = file.getOriginalFilename() != null
            ? file.getOriginalFilename().toLowerCase() : "";

        if (filename.endsWith(".csv")) {
            return parseCsv(file);
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return parseExcel(file);
        } else {
            throw new BadRequestException(
                "Unsupported file type. Please upload a .csv or .xlsx file.");
        }
    }

    // ── CSV ───────────────────────────────────────────────────────────────

    private List<BulkSchoolRow> parseCsv(MultipartFile file) {
        List<BulkSchoolRow> rows = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                 .builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setIgnoreHeaderCase(true)
                 .setTrim(true)
                 .setIgnoreEmptyLines(true)
                 .build()
                 .parse(reader)) {

            validateHeaders(parser.getHeaderNames(), "CSV");

            int rowNum = 2; // 1-indexed; row 1 = header
            for (CSVRecord record : parser) {
                if (rowNum > MAX_ROWS + 1) {
                    throw new BadRequestException(
                        "File exceeds maximum of " + MAX_ROWS + " rows. " +
                        "Split into smaller batches.");
                }
                BulkSchoolRow row = mapCsvRecord(record, rowNum);
                rows.add(row);
                rowNum++;
            }

        } catch (IOException e) {
            throw new BadRequestException("Failed to parse CSV file: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("CSV file contains no data rows.");
        }
        return rows;
    }

    private BulkSchoolRow mapCsvRecord(CSVRecord record, int rowNum) {
        BulkSchoolRow row = new BulkSchoolRow();
        row.setRowNumber(rowNum);

        row.setSchoolName(get(record, COL_SCHOOL_NAME));
        row.setType(get(record, COL_TYPE));
        row.setBoard(get(record, COL_BOARD));
        row.setMedium(get(record, COL_MEDIUM));
        row.setUdiseCode(get(record, COL_UDISE_CODE));
        row.setAffiliationNo(get(record, COL_AFFILIATION_NO));
        row.setPrincipalName(get(record, COL_PRINCIPAL_NAME));
        row.setOfficialEmail(get(record, COL_OFFICIAL_EMAIL));
        row.setPhonePrimary(get(record, COL_PHONE_PRIMARY));
        row.setAddressLine1(get(record, COL_ADDRESS_LINE1));
        row.setAddressLine2(get(record, COL_ADDRESS_LINE2));
        row.setCity(get(record, COL_CITY));
        row.setDistrict(get(record, COL_DISTRICT));
        row.setState(get(record, COL_STATE));
        row.setPincode(get(record, COL_PINCODE));
        row.setAcademicYear(get(record, COL_ACADEMIC_YEAR));
        row.setChainId(get(record, COL_CHAIN_ID));
        row.setBranchCode(get(record, COL_BRANCH_CODE));
        row.setBranchName(get(record, COL_BRANCH_NAME));
        row.setHeadquarter(parseBool(get(record, COL_IS_HEADQUARTER)));
        row.setAdminEmail(get(record, COL_ADMIN_EMAIL));
        row.setAdminFullName(get(record, COL_ADMIN_FULL_NAME));
        row.setAdminPhone(get(record, COL_ADMIN_PHONE));
        row.setEstablishedYear(get(record, COL_ESTABLISHED_YEAR));
        row.setTrustName(get(record, COL_TRUST_NAME));
        row.setManagementType(get(record, COL_MANAGEMENT_TYPE));
        row.setCoEd(parseBool(get(record, COL_CO_ED), true));
        row.setResidential(parseBool(get(record, COL_RESIDENTIAL), false));

        return row;
    }

    // ── Excel ─────────────────────────────────────────────────────────────

    private List<BulkSchoolRow> parseExcel(MultipartFile file) {
        List<BulkSchoolRow> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                throw new BadRequestException("Excel file is empty or has no data rows.");
            }

            // Build header map from row 0
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BadRequestException("Excel file is missing the header row.");
            }

            java.util.Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            validateHeaders(new ArrayList<>(headerMap.keySet()), "Excel");

            int lastRow = Math.min(sheet.getLastRowNum(), MAX_ROWS);
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                BulkSchoolRow parsed = mapExcelRow(row, headerMap, i + 1);
                rows.add(parsed);
            }

        } catch (IOException e) {
            throw new BadRequestException("Failed to parse Excel file: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("Excel file contains no data rows.");
        }
        return rows;
    }

    private java.util.Map<String, Integer> buildHeaderMap(Row headerRow) {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String header = cellToString(cell).toLowerCase().trim()
                    .replace(" ", "_");
                map.put(header, i);
            }
        }
        return map;
    }

    private BulkSchoolRow mapExcelRow(Row row,
                                      java.util.Map<String, Integer> hdr,
                                      int rowNum) {
        BulkSchoolRow parsed = new BulkSchoolRow();
        parsed.setRowNumber(rowNum);

        parsed.setSchoolName(excelCell(row, hdr, COL_SCHOOL_NAME));
        parsed.setType(excelCell(row, hdr, COL_TYPE));
        parsed.setBoard(excelCell(row, hdr, COL_BOARD));
        parsed.setMedium(excelCell(row, hdr, COL_MEDIUM));
        parsed.setUdiseCode(excelCell(row, hdr, COL_UDISE_CODE));
        parsed.setAffiliationNo(excelCell(row, hdr, COL_AFFILIATION_NO));
        parsed.setPrincipalName(excelCell(row, hdr, COL_PRINCIPAL_NAME));
        parsed.setOfficialEmail(excelCell(row, hdr, COL_OFFICIAL_EMAIL));
        parsed.setPhonePrimary(excelCell(row, hdr, COL_PHONE_PRIMARY));
        parsed.setAddressLine1(excelCell(row, hdr, COL_ADDRESS_LINE1));
        parsed.setAddressLine2(excelCell(row, hdr, COL_ADDRESS_LINE2));
        parsed.setCity(excelCell(row, hdr, COL_CITY));
        parsed.setDistrict(excelCell(row, hdr, COL_DISTRICT));
        parsed.setState(excelCell(row, hdr, COL_STATE));
        parsed.setPincode(excelCell(row, hdr, COL_PINCODE));
        parsed.setAcademicYear(excelCell(row, hdr, COL_ACADEMIC_YEAR));
        parsed.setChainId(excelCell(row, hdr, COL_CHAIN_ID));
        parsed.setBranchCode(excelCell(row, hdr, COL_BRANCH_CODE));
        parsed.setBranchName(excelCell(row, hdr, COL_BRANCH_NAME));
        parsed.setHeadquarter(parseBool(excelCell(row, hdr, COL_IS_HEADQUARTER)));
        parsed.setAdminEmail(excelCell(row, hdr, COL_ADMIN_EMAIL));
        parsed.setAdminFullName(excelCell(row, hdr, COL_ADMIN_FULL_NAME));
        parsed.setAdminPhone(excelCell(row, hdr, COL_ADMIN_PHONE));
        parsed.setEstablishedYear(excelCell(row, hdr, COL_ESTABLISHED_YEAR));
        parsed.setTrustName(excelCell(row, hdr, COL_TRUST_NAME));
        parsed.setManagementType(excelCell(row, hdr, COL_MANAGEMENT_TYPE));
        parsed.setCoEd(parseBool(excelCell(row, hdr, COL_CO_ED), true));
        parsed.setResidential(parseBool(excelCell(row, hdr, COL_RESIDENTIAL), false));

        return parsed;
    }

    // ── Utility helpers ───────────────────────────────────────────────────

    private void validateHeaders(List<String> provided, String source) {
        List<String> required = List.of(
            COL_SCHOOL_NAME, COL_TYPE, COL_PHONE_PRIMARY,
            COL_ADDRESS_LINE1, COL_CITY, COL_STATE, COL_PINCODE
        );
        List<String> normalized = provided.stream()
            .map(h -> h.toLowerCase().trim())
            .toList();
        List<String> missing = required.stream()
            .filter(r -> !normalized.contains(r))
            .toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException(
                source + " file is missing required columns: " + missing);
        }
    }

    private String get(CSVRecord record, String col) {
        try {
            String val = record.get(col);
            return val != null && !val.isBlank() ? val.trim() : null;
        } catch (IllegalArgumentException e) {
            return null; // column not present
        }
    }

    private String excelCell(Row row, java.util.Map<String, Integer> hdr, String col) {
        Integer idx = hdr.get(col);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        String val = cellToString(cell).trim();
        return val.isBlank() ? null : val;
    }

    private String cellToString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                // Avoid "1234.0" for integer-like numbers
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { yield cell.getStringCellValue(); }
            }
            default -> "";
        };
    }

    private boolean parseBool(String value) {
        return parseBool(value, false);
    }

    private boolean parseBool(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        return value.equalsIgnoreCase("true")
            || value.equalsIgnoreCase("yes")
            || value.equalsIgnoreCase("1")
            || value.equalsIgnoreCase("y");
    }

    private boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !cellToString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
