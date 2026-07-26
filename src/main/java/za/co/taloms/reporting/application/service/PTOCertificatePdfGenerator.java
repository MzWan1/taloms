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
import za.co.taloms.pto.application.dto.PTOResponse;
import za.co.taloms.pto.application.service.PTOService;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PTOCertificatePdfGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    private final PTOService ptoService;

    public byte[] generateCertificate(Long ptoId) {
        PTOResponse pto = ptoService.findById(ptoId);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float margin = 50;
                float width = PDRectangle.A4.getWidth() - 2 * margin;
                float y = 750;

                // Header
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("PERMISSION TO OCCUPY (PTO) CERTIFICATE");
                cs.endText();
                y -= 40;

                // Decorative line
                cs.setStrokingColor(27, 58, 107);
                cs.setLineWidth(2);
                cs.moveTo(margin, y);
                cs.lineTo(margin + width, y);
                cs.stroke();
                y -= 30;

                // Certificate details
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("PTO Number:        " + (pto.getPtoNumber() != null ? pto.getPtoNumber() : ""));
                cs.endText();
                y -= 25;

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Holder Name:       " + (pto.getPtoHolderName() != null ? pto.getPtoHolderName() : ""));
                cs.endText();
                y -= 25;

                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("ID Number:         " + (pto.getIdNumber() != null ? pto.getIdNumber() : ""));
                cs.endText();
                y -= 25;

                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Traditional Authority: " + (pto.getAuthorityName() != null ? pto.getAuthorityName() : ""));
                cs.endText();
                y -= 25;

                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Village:           " + (pto.getVillageName() != null ? pto.getVillageName() : ""));
                cs.endText();
                y -= 35;

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Status:            " + (pto.getStatusDisplay() != null ? pto.getStatusDisplay() : ""));
                cs.endText();
                y -= 25;

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Issue Date:        " + (pto.getIssueDate() != null ? pto.getIssueDate().format(DATE_FORMATTER) : ""));
                cs.endText();
                y -= 25;

                if (pto.getExpiryDate() != null) {
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Expiry Date:       " + pto.getExpiryDate().format(DATE_FORMATTER));
                    cs.endText();
                } else {
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Expiry Date:       Indefinite");
                    cs.endText();
                }
                y -= 40;

                if (pto.getAllocatedBy() != null) {
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Allocated By:      " + pto.getAllocatedBy());
                    cs.endText();
                    y -= 25;
                }

                if (pto.getSurveyReference() != null) {
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Survey Reference:  " + pto.getSurveyReference());
                    cs.endText();
                    y -= 25;
                }

                y -= 40;

                // Footer
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                cs.beginText();
                cs.newLineAtOffset(margin, 30);
                cs.showText("Generated by TALOMS on " + java.time.LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PTO certificate PDF for PTO {}", ptoId, e);
            throw new RuntimeException("Failed to generate PTO certificate: " + e.getMessage(), e);
        }
    }
}


