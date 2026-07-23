package za.co.taloms.reporting.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import za.co.taloms.reporting.application.dto.ReportData;
import za.co.taloms.reporting.application.dto.ReportRequest;
import za.co.taloms.reporting.application.dto.ReportResponse;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceExcelExporter implements ReportService {

    private final ReportDataService reportDataService;

    @Override
    public ReportResponse generatePtoOccupancyRegisterReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generatePtoOccupancyRegisterReport(request), "PTO_Occupancy_Register");
    }

    @Override
    public ReportResponse generateLandParcelUtilisationReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateLandParcelUtilisationReport(request), "Land_Parcel_Utilisation");
    }

    @Override
    public ReportResponse generateStandAllocationReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateStandAllocationReport(request), "Stand_Allocation");
    }

    @Override
    public ReportResponse generateVillagePopulationReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateVillagePopulationReport(request), "Village_Population");
    }

    @Override
    public ReportResponse generateHouseholdRegisterReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateHouseholdRegisterReport(request), "Household_Register");
    }

    @Override
    public ReportResponse generateResidentDemographicsReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateResidentDemographicsReport(request), "Resident_Demographics");
    }

    @Override
    public ReportResponse generateBusinessOccupancyReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateBusinessOccupancyReport(request), "Business_Occupancy");
    }

    @Override
    public ReportResponse generateEconomicActivityReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateEconomicActivityReport(request), "Economic_Activity");
    }

    @Override
    public ReportResponse generateUserActivityAuditReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateUserActivityAuditReport(request), "User_Activity_Audit");
    }

    @Override
    public ReportResponse generateDocumentManagementReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateDocumentManagementReport(request), "Document_Management");
    }

    @Override
    public ReportResponse generatePerformanceDashboardReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generatePerformanceDashboardReport(request), "Performance_Dashboard");
    }

    @Override
    public ReportResponse generateLandBoundaryReport(ReportRequest request) {
        return buildExcelReport(reportDataService.generateLandBoundaryReport(request), "Land_Boundary");
    }

    private ReportResponse buildExcelReport(ReportData data, String baseName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            writeWorkbook(workbook, data);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return buildResponse(baos.toByteArray(), baseName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (Exception e) {
            log.error("Failed to generate Excel report", e);
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    private void writeWorkbook(Workbook workbook, ReportData data) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle boldStyle = createBoldStyle(workbook);

        Sheet summarySheet = workbook.createSheet("Summary");
        createSummarySheet(summarySheet, data, headerStyle, boldStyle);

        int sheetIndex = 1;
        for (za.co.taloms.reporting.application.dto.ReportSection section : data.sections()) {
            Sheet detailSheet = workbook.createSheet("Sheet" + sheetIndex);
            createDetailSheet(detailSheet, section, headerStyle, dataStyle);
            sheetIndex++;
        }

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getPhysicalNumberOfRows() > 0) {
                Row firstRow = sheet.getRow(0);
                if (firstRow != null) {
                    int lastCell = firstRow.getLastCellNum();
                    for (int j = 0; j < lastCell; j++) {
                        sheet.autoSizeColumn(j);
                    }
                    sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, lastCell - 1));
                    sheet.createFreezePane(0, 1);
                }
            }
        }
    }

    private void createSummarySheet(Sheet sheet, ReportData data, CellStyle headerStyle, CellStyle boldStyle) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(data.reportTitle() != null ? data.reportTitle() : "Report Summary");
        titleCell.setCellStyle(boldStyle);

        Row filterRow = sheet.createRow(1);
        Cell filterCell = filterRow.createCell(0);
        StringBuilder filterInfo = new StringBuilder();
        if (data.authorityName() != null) filterInfo.append("Authority: ").append(data.authorityName()).append(" | ");
        if (data.villageName() != null) filterInfo.append("Village: ").append(data.villageName()).append(" | ");
        if (data.dateFrom() != null) filterInfo.append("From: ").append(data.dateFrom()).append(" | ");
        if (data.dateTo() != null) filterInfo.append("To: ").append(data.dateTo());
        filterCell.setCellValue(filterInfo.toString());

        int rowNum = 3;
        for (za.co.taloms.reporting.application.dto.ReportSection section : data.sections()) {
            if (section.metrics() != null && !section.metrics().isEmpty()) {
                Row sectionRow = sheet.createRow(rowNum++);
                sectionRow.createCell(0).setCellValue(section.sectionTitle());
                sectionRow.getCell(0).setCellStyle(boldStyle);
                rowNum++;

                for (Map<String, String> metric : section.metrics()) {
                    for (Map.Entry<String, String> entry : metric.entrySet()) {
                        Row row = sheet.createRow(rowNum++);
                        Cell keyCell = row.createCell(0);
                        keyCell.setCellValue(entry.getKey());
                        keyCell.setCellStyle(headerStyle);
                        Cell valCell = row.createCell(1);
                        valCell.setCellValue(entry.getValue());
                        valCell.setCellStyle(headerStyle);
                    }
                }
                rowNum++;
            }
        }
    }

    private void createDetailSheet(Sheet sheet, za.co.taloms.reporting.application.dto.ReportSection section,
                                   CellStyle headerStyle, CellStyle dataStyle) {
        if (section.tableHeaders() == null || section.tableHeaders().isEmpty()) return;

        List<String> headers = section.tableHeaders();
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (List<String> rowData : section.tableRows()) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < Math.min(headers.size(), rowData.size()); i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(rowData.get(i) != null ? rowData.get(i) : "");
                cell.setCellStyle(dataStyle);
            }
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        return style;
    }

    private ReportResponse buildResponse(byte[] content, String baseName, String contentType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return ReportResponse.builder()
                .content(content)
                .filename(baseName + "_" + timestamp + ".xlsx")
                .contentType(contentType)
                .fileSize(content.length)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
