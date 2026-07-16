package za.co.taloms.reporting.application.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
public class ReportServicePdfExporter implements ReportService {

    private final PTOService ptoService;
    private final ParcelService parcelService;

    public ReportServicePdfExporter(PTOService ptoService, ParcelService parcelService) {
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

        byte[] pdfBytes = generatePTOPDF(ptos);
        return buildResponse(pdfBytes, "PTO_Report", "application/pdf");
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

        byte[] pdfBytes = generateParcelPDF(parcels);
        return buildResponse(pdfBytes, "Parcel_Report", "application/pdf");
    }

    @Override
    public ReportResponse generatePopulationReport(ReportRequest request) {
        byte[] pdfBytes = generatePlaceholderPDF("Population Report");
        return buildResponse(pdfBytes, "Population_Report", "application/pdf");
    }

    @Override
    public ReportResponse generateLandUtilisationReport(ReportRequest request) {
        byte[] pdfBytes = generatePlaceholderPDF("Land Utilisation Report");
        return buildResponse(pdfBytes, "Land_Utilisation_Report", "application/pdf");
    }

    private byte[] generatePTOPDF(List<PTOResponse> ptos) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Title
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.beginText();
                cs.newLineAtOffset(50, 750);
                cs.showText("TALOMS - PTO Report");
                cs.endText();

                // Subtitle
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.beginText();
                cs.newLineAtOffset(50, 720);
                cs.showText("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
                cs.endText();

                // Table headers
                float y = 680;
                float margin = 50;
                float[] columnWidths = {80, 100, 100, 100, 120};

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                drawTableHeader(cs, margin, y, columnWidths, new String[]{"PTO Number", "Holder", "Purpose", "Status", "Village"});

                // Table rows
                y -= 20;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                for (PTOResponse pto : ptos) {
                    String[] rowData = {
                            pto.getPtoNumber(),
                            pto.getPtoHolderName(),
                            pto.getPurposeDisplay(),
                            pto.getStatusDisplay(),
                            pto.getVillageName() != null ? pto.getVillageName() : "—"
                    };
                    drawTableRow(cs, margin, y, columnWidths, rowData);
                    y -= 20;
                }

                // Footer
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                cs.beginText();
                cs.newLineAtOffset(margin, 30);
                cs.showText("Total PTOs: " + ptos.size());
                cs.endText();

                cs.beginText();
                cs.newLineAtOffset(400, 30);
                cs.showText("Generated by TALOMS v1.0");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private byte[] generateParcelPDF(List<ParcelResponse> parcels) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Title
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.beginText();
                cs.newLineAtOffset(50, 750);
                cs.showText("TALOMS - Parcel Report");
                cs.endText();

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.beginText();
                cs.newLineAtOffset(50, 720);
                cs.showText("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
                cs.endText();

                // Table headers
                float y = 680;
                float margin = 50;
                float[] columnWidths = {80, 80, 100, 80, 80};

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                drawTableHeader(cs, margin, y, columnWidths, new String[]{"Parcel #", "Stand #", "Type", "Status", "Village"});

                y -= 20;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                for (ParcelResponse parcel : parcels) {
                    String[] rowData = {
                            parcel.getParcelNumber(),
                            parcel.getStandNumber(),
                            parcel.getParcelTypeDisplay(),
                            parcel.getStatusDisplay(),
                            parcel.getVillageName() != null ? parcel.getVillageName() : "—"
                    };
                    drawTableRow(cs, margin, y, columnWidths, rowData);
                    y -= 20;
                }

                // Footer
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                cs.beginText();
                cs.newLineAtOffset(margin, 30);
                cs.showText("Total Parcels: " + parcels.size());
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private byte[] generatePlaceholderPDF(String title) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                cs.beginText();
                cs.newLineAtOffset(50, 750);
                cs.showText("TALOMS - " + title);
                cs.endText();

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.beginText();
                cs.newLineAtOffset(50, 700);
                cs.showText("This report is under development. Please check back later.");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate placeholder PDF", e);
        }
    }

    private void drawTableHeader(PDPageContentStream cs, float x, float y, float[] columnWidths, String[] headers) throws Exception {
        float cx = x;
        for (int i = 0; i < headers.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(cx + 5, y - 5);
            cs.showText(headers[i]);
            cs.endText();
            cx += columnWidths[i];
        }
        // Draw header line
        cs.moveTo(x, y - 15);
        cs.lineTo(x + sumArray(columnWidths), y - 15);
        cs.stroke();
    }

    private void drawTableRow(PDPageContentStream cs, float x, float y, float[] columnWidths, String[] rowData) throws Exception {
        float cx = x;
        for (int i = 0; i < rowData.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(cx + 5, y - 5);
            cs.showText(rowData[i]);
            cs.endText();
            cx += columnWidths[i];
        }
    }

    private float sumArray(float[] arr) {
        float sum = 0;
        for (float f : arr) sum += f;
        return sum;
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