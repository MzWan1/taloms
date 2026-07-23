package za.co.taloms.reporting.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
public class ReportServicePdfExporter implements ReportService {

    private final ReportDataService reportDataService;

    @Override
    public ReportResponse generatePtoOccupancyRegisterReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generatePtoOccupancyRegisterReport(request), "PTO Occupancy Register");
    }

    @Override
    public ReportResponse generateLandParcelUtilisationReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateLandParcelUtilisationReport(request), "Land Parcel Utilisation");
    }

    @Override
    public ReportResponse generateStandAllocationReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateStandAllocationReport(request), "Stand Allocation");
    }

    @Override
    public ReportResponse generateVillagePopulationReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateVillagePopulationReport(request), "Village Population");
    }

    @Override
    public ReportResponse generateHouseholdRegisterReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateHouseholdRegisterReport(request), "Household Register");
    }

    @Override
    public ReportResponse generateResidentDemographicsReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateResidentDemographicsReport(request), "Resident Demographics");
    }

    @Override
    public ReportResponse generateBusinessOccupancyReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateBusinessOccupancyReport(request), "Business Occupancy");
    }

    @Override
    public ReportResponse generateEconomicActivityReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateEconomicActivityReport(request), "Economic Activity");
    }

    @Override
    public ReportResponse generateUserActivityAuditReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateUserActivityAuditReport(request), "User Activity Audit");
    }

    @Override
    public ReportResponse generateDocumentManagementReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateDocumentManagementReport(request), "Document Management");
    }

    @Override
    public ReportResponse generatePerformanceDashboardReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generatePerformanceDashboardReport(request), "Performance Dashboard");
    }

    @Override
    public ReportResponse generateLandBoundaryReport(ReportRequest request) {
        return buildPdfReport(reportDataService.generateLandBoundaryReport(request), "Land Boundary");
    }

    private ReportResponse buildPdfReport(ReportData data, String reportTitle) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            drawPdfPage(document, page, data, 0);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return buildResponse(baos.toByteArray(), reportTitle.replaceAll("\\s+", "_"), "application/pdf");
        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private void drawPdfPage(PDDocument document, PDPage page, ReportData data, int pageNum) throws Exception {
        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            float margin = 50;
            float width = PDRectangle.A4.getWidth() - 2 * margin;
            float y = 780;

            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText("TALOMS - " + (data.reportTitle() != null ? data.reportTitle() : "Report"));
            cs.endText();
            y -= 25;

            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            String filterInfo = buildFilterInfo(data);
            cs.showText("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                    + "    Filters: " + filterInfo);
            cs.endText();
            y -= 30;

            for (za.co.taloms.reporting.application.dto.ReportSection section : data.sections()) {
                if (y < 100) {
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    y = 780;
                    drawFooter(cs, margin, data, pageNum + 1);
                }

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(section.sectionTitle() != null ? section.sectionTitle() : "");
                cs.endText();
                y -= 20;

                if (section.metrics() != null && !section.metrics().isEmpty()) {
                    y = drawMetricsTable(cs, margin, y, width, section.metrics());
                    y -= 15;
                }

                if (section.tableHeaders() != null && !section.tableHeaders().isEmpty()) {
                    y = drawDataTable(cs, margin, y, width, section.tableHeaders(), section.tableRows());
                }

                y -= 20;
            }

            drawFooter(cs, margin, data, pageNum + 1);
        }
    }

    private void drawFooter(PDPageContentStream cs, float margin, ReportData data, int pageNum) throws Exception {
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        cs.beginText();
        cs.newLineAtOffset(margin, 30);
        cs.showText("Page " + pageNum + "    Generated by TALOMS");
        cs.endText();
    }

    private String buildFilterInfo(ReportData data) {
        StringBuilder sb = new StringBuilder();
        if (data.authorityName() != null) sb.append("Authority: ").append(data.authorityName()).append("  ");
        if (data.villageName() != null) sb.append("Village: ").append(data.villageName()).append("  ");
        if (data.dateFrom() != null) sb.append("From: ").append(data.dateFrom()).append("  ");
        if (data.dateTo() != null) sb.append("To: ").append(data.dateTo());
        return sb.toString().trim();
    }

    private float drawMetricsTable(PDPageContentStream cs, float x, float y, float width, List<Map<String, String>> metrics)
            throws Exception {
        float rowHeight = 16;
        float currentY = y;

        float keyWidth = width * 0.6f;
        float valueWidth = width - keyWidth;

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);
        cs.setNonStrokingColor(200, 200, 200);
        cs.addRect(x, currentY - rowHeight + 5, width, rowHeight);
        cs.fill();
        cs.setNonStrokingColor(0, 0, 0);
        cs.beginText();
        cs.newLineAtOffset(x + 5, currentY - rowHeight + 5);
        cs.showText("Metric");
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(x + keyWidth + 5, currentY - rowHeight + 5);
        cs.showText("Value");
        cs.endText();
        currentY -= rowHeight;

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
        for (Map<String, String> metric : metrics) {
            for (Map.Entry<String, String> entry : metric.entrySet()) {
                cs.beginText();
                cs.newLineAtOffset(x + 5, currentY - rowHeight + 5);
                cs.showText(entry.getKey() != null ? entry.getKey() : "");
                cs.endText();
                cs.beginText();
                cs.newLineAtOffset(x + keyWidth + 5, currentY - rowHeight + 5);
                cs.showText(entry.getValue() != null ? entry.getValue() : "");
                cs.endText();
                currentY -= rowHeight;
            }
        }
        return currentY;
    }

    private float drawDataTable(PDPageContentStream cs, float x, float y, float width, List<String> headers,
                                List<List<String>> rows) throws Exception {
        float currentY = y;
        float rowHeight = 16;

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 8);
        cs.setNonStrokingColor(200, 200, 200);
        cs.addRect(x, currentY - rowHeight + 5, width, rowHeight);
        cs.fill();
        cs.setNonStrokingColor(0, 0, 0);

        int colCount = headers.size();
        float colWidth = width / colCount;

        float cx = x;
        for (String header : headers) {
            cs.beginText();
            cs.newLineAtOffset(cx + 3, currentY - rowHeight + 5);
            cs.showText(header != null ? header : "");
            cs.endText();
            cx += colWidth;
        }
        currentY -= rowHeight;

        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        for (List<String> row : rows) {
            cx = x;
            for (int i = 0; i < colCount; i++) {
                String val = i < row.size() ? row.get(i) : "";
                cs.beginText();
                cs.newLineAtOffset(cx + 3, currentY - rowHeight + 5);
                cs.showText(val != null ? val : "");
                cs.endText();
                cx += colWidth;
            }
            currentY -= rowHeight;
        }
        return currentY;
    }

    private ReportResponse buildResponse(byte[] content, String baseName, String contentType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return ReportResponse.builder()
                .content(content)
                .filename(baseName + "_" + timestamp + ".pdf")
                .contentType(contentType)
                .fileSize(content.length)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
