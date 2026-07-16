package za.co.taloms.reporting.application.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.pto.application.dto.PTOResponse;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.reporting.application.dto.ReportRequest;
import za.co.taloms.reporting.application.dto.ReportResponse;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportServiceExcelExporter implements ReportService {

    private final PTOService ptoService;
    private final ParcelService parcelService;

    public ReportServiceExcelExporter(PTOService ptoService, ParcelService parcelService) {
        this.ptoService = ptoService;
        this.parcelService = parcelService;
    }

    @Override
    public ReportResponse generatePTOReport(ReportRequest request) {
        List<PTOResponse> ptos;
        if (request.getAuthorityId() != null) {
            ptos = ptoService.findByAuthority(request.getAuthorityId());
        } else if (request.getVillageId() != null) {
            ptos = ptoService.findByVillage(request.getVillageId());
        } else {
            ptos = ptoService.findAll();
        }

        byte[] excelBytes = generatePTOExcel(ptos);
        return buildResponse(excelBytes, "PTO_Report", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Override
    public ReportResponse generateParcelReport(ReportRequest request) {
        List<ParcelResponse> parcels;
        if (request.getAuthorityId() != null) {
            parcels = parcelService.findAll();
        } else if (request.getVillageId() != null) {
            parcels = parcelService.findByVillage(request.getVillageId());
        } else {
            parcels = parcelService.findAll();
        }

        byte[] excelBytes = generateParcelExcel(parcels);
        return buildResponse(excelBytes, "Parcel_Report", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Override
    public ReportResponse generatePopulationReport(ReportRequest request) {
        byte[] excelBytes = generatePlaceholderExcel("Population Report");
        return buildResponse(excelBytes, "Population_Report", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Override
    public ReportResponse generateLandUtilisationReport(ReportRequest request) {
        byte[] excelBytes = generatePlaceholderExcel("Land Utilisation Report");
        return buildResponse(excelBytes, "Land_Utilisation_Report", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private byte[] generatePTOExcel(List<PTOResponse> ptos) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PTO Report");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Create headers
            String[] headers = {"PTO Number", "Holder Name", "ID Number", "Purpose", "Status", "Village", "Issue Date", "Expiry Date"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Create data rows
            int rowNum = 1;
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            for (PTOResponse pto : ptos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(pto.getPtoNumber());
                row.createCell(1).setCellValue(pto.getPtoHolderName());
                row.createCell(2).setCellValue(pto.getIdNumber());
                row.createCell(3).setCellValue(pto.getPurposeDisplay());
                row.createCell(4).setCellValue(pto.getStatusDisplay());
                row.createCell(5).setCellValue(pto.getVillageName() != null ? pto.getVillageName() : "");
                row.createCell(6).setCellValue(pto.getIssueDate() != null ? pto.getIssueDate().toString() : "");
                row.createCell(7).setCellValue(pto.getExpiryDate() != null ? pto.getExpiryDate().toString() : "");
            }

            // Add summary row
            Row summaryRow = sheet.createRow(rowNum + 1);
            Cell summaryCell = summaryRow.createCell(0);
            summaryCell.setCellValue("Total PTOs: " + ptos.size());
            CellStyle summaryStyle = workbook.createCellStyle();
            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryStyle.setFont(summaryFont);
            summaryCell.setCellStyle(summaryStyle);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    private byte[] generateParcelExcel(List<ParcelResponse> parcels) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parcel Report");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Create headers
            String[] headers = {"Parcel Number", "Stand Number", "Type", "Status", "Village", "Area (m²)", "Area (ha)"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (ParcelResponse parcel : parcels) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(parcel.getParcelNumber());
                row.createCell(1).setCellValue(parcel.getStandNumber());
                row.createCell(2).setCellValue(parcel.getParcelTypeDisplay());
                row.createCell(3).setCellValue(parcel.getStatusDisplay());
                row.createCell(4).setCellValue(parcel.getVillageName() != null ? parcel.getVillageName() : "");
                row.createCell(5).setCellValue(parcel.getAreaM2() != null ? parcel.getAreaM2() : 0);
                row.createCell(6).setCellValue(parcel.getAreaHectares() != null ? parcel.getAreaHectares() : 0);
            }

            // Add summary row
            Row summaryRow = sheet.createRow(rowNum + 1);
            Cell summaryCell = summaryRow.createCell(0);
            summaryCell.setCellValue("Total Parcels: " + parcels.size());
            CellStyle summaryStyle = workbook.createCellStyle();
            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryStyle.setFont(summaryFont);
            summaryCell.setCellStyle(summaryStyle);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    private byte[] generatePlaceholderExcel(String title) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(title);

            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("TALOMS - " + title);

            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 16);
            style.setFont(font);
            cell.setCellStyle(style);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("This report is under development. Please check back later.");

            sheet.autoSizeColumn(0);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate placeholder Excel", e);
        }
    }

    private ReportResponse buildResponse(byte[] content, String baseName, String contentType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return ReportResponse.builder()
                .content(content)
                .filename(baseName + "_" + timestamp + ".xlsx")  // Always .xlsx for Excel
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .fileSize(content.length)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}