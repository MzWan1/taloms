package za.co.taloms.pto.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PTOCertificatePdfGenerator {

    private final PTORepositoryPort ptoRepository;

    @Transactional(readOnly = true)
    public byte[] generateCertificate(Long ptoId) {
        PTO pto = ptoRepository.findById(ptoId)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", ptoId));
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawCertificate(cs, pto);
            }
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF certificate for PTO {}", ptoId, e);
            throw new RuntimeException("Failed to generate PDF certificate", e);
        }
    }


    @Transactional(readOnly = true)
    public byte[] generateProofOfResidencePdf(Long ptoId, String requesterName, String requesterIdNumber) {
        PTO pto = ptoRepository.findById(ptoId)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", ptoId));
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawProofOfResidence(cs, pto, requesterName, requesterIdNumber);
            }
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF proof of residence for PTO {}", ptoId, e);
            throw new RuntimeException("Failed to generate PDF proof of residence", e);
        }
    }

    private void drawCertificate(PDPageContentStream cs, PTO pto) throws Exception {
        float y = 750, m = 50;
        textLine(cs, m, y, PDType1Font.HELVETICA_BOLD, 18, "TRADITIONAL AUTHORITY LAND");
        y -= 25;
        textLine(cs, m, y, PDType1Font.HELVETICA_BOLD, 18, "OCCUPANCY MANAGEMENT SYSTEM");
        y -= 40;
        textLine(cs, m, y, PDType1Font.HELVETICA_BOLD, 14, "PTO CERTIFICATE");
        y -= 50;
        drawLine(cs, m, y, 500);
        y -= 25;
        drawLabel(cs, m, y, "PTO Number:", pto.getPtoNumber());
        y -= 20;
        drawLabel(cs, m, y, "Holder Name:", pto.getPtoHolderName());
        y -= 20;
        // Official land-occupancy certificate: the full holder ID is intentionally
        // printed (it is part of the legally meaningful document content). Do NOT
        // replace this with the display-masked value without confirming the
        // document's legal requirements.
        drawLabel(cs, m, y, "ID Number:", pto.getIdNumber());
        y -= 20;
        drawLabel(cs, m, y, "Purpose:", pto.getPurpose() == null ? "-" : pto.getPurpose().getDisplayName()); y -= 20;
        drawLabel(cs, m, y, "Status:", pto.getStatus() == null ? "-" : pto.getStatus().getDisplayName()); y -= 20;
        drawLabel(cs, m, y, "Issue Date:", pto.getIssueDate() == null ? "-" : pto.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE)); y -= 20;
        if (pto.getExpiryDate() != null) {
            drawLabel(cs, m, y, "Expiry Date:", pto.getExpiryDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            y -= 20;
        }
        if (pto.getVillage() != null) {
            drawLabel(cs, m, y, "Village:", pto.getVillage().getVillageName());
            y -= 20;
        }
        if (pto.getTraditionalAuthority() != null) {
            drawLabel(cs, m, y, "Authority:", pto.getTraditionalAuthority().getAuthorityName());
            y -= 20;
        }
        if (pto.getParcel() != null) {
            drawLabel(cs, m, y, "Stand:", pto.getParcel().getStandNumber());
            y -= 20;
        }
        y -= 20;
        drawLine(cs, m, y, 500);
        y -= 30;
        textLine(cs, m, y, PDType1Font.HELVETICA, 10, "This certificate is issued by TALOMS");
        y -= 15;
        textLine(cs, m, y, PDType1Font.HELVETICA_BOLD, 10, "Generated electronically - no wet signature required");
    }

    private void drawProofOfResidence(PDPageContentStream cs, PTO pto, String requesterName, String requesterIdNumber) throws Exception {
        float y = 750, m = 50;
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter pf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        textLine(cs, m, y, PDType1Font.HELVETICA_BOLD, 16, "PROOF OF RESIDENCE");
        y -= 30;
        textLine(cs, m, y, PDType1Font.HELVETICA, 11, "Traditional Authority Land & Occupancy Management System");
        y -= 40;
        drawLine(cs, m, y, 500);
        y -= 20;
        drawLabel(cs, m, y, "Issued to:", requesterName); y -= 18;
        drawLabel(cs, m, y, "Requester ID:", requesterIdNumber); y -= 18;
        drawLabel(cs, m, y, "Issued by:", "TALOMS (on behalf)"); y -= 18;
        drawLabel(cs, m, y, "Print date:", java.time.LocalDateTime.now().format(pf)); y -= 30;
        textLine(cs, m, y, PDType1Font.HELVETICA_BOLD, 12, "OCCUPANCY / RESIDENCY DETAILS");
        y -= 25;
        cs.setFont(PDType1Font.HELVETICA, 11);
        drawLine(cs, m, y, 500);
        y -= 20;
        drawLabel(cs, m, y, "PTO Number:", pto.getPtoNumber()); y -= 18;
        drawLabel(cs, m, y, "Holder Name:", pto.getPtoHolderName()); y -= 18;
        // Official proof-of-residence document: the full holder ID is intentionally
        // printed (it is part of the legally meaningful document content). Do NOT
        // replace this with the display-masked value without confirming the
        // document's legal requirements.
        drawLabel(cs, m, y, "Holder ID:", pto.getIdNumber()); y -= 18;
        drawLabel(cs, m, y, "Purpose:", pto.getPurpose() == null ? "-" : pto.getPurpose().getDisplayName()); y -= 18;
        drawLabel(cs, m, y, "Status:", pto.getStatus() == null ? "-" : pto.getStatus().getDisplayName()); y -= 18;
        drawLabel(cs, m, y, "Issue Date:", pto.getIssueDate().format(df)); y -= 18;
        if (pto.getExpiryDate() != null) {
            drawLabel(cs, m, y, "Expiry Date:", pto.getExpiryDate().format(df)); y -= 18;
        }
        if (pto.getVillage() != null) {
            drawLabel(cs, m, y, "Village:", pto.getVillage().getVillageName()); y -= 18;
        }
        if (pto.getTraditionalAuthority() != null) {
            drawLabel(cs, m, y, "Authority:", pto.getTraditionalAuthority().getAuthorityName()); y -= 18;
        }
        if (pto.getParcel() != null) {
            drawLabel(cs, m, y, "Stand:", pto.getParcel().getStandNumber()); y -= 18;
        }
        y -= 20;
        drawLine(cs, m, y, 500);
        y -= 25;
        textLine(cs, m, y, PDType1Font.HELVETICA_BOLD, 10, "OFFICIAL STAMP");
        y -= 15;
        textLine(cs, m, y, PDType1Font.HELVETICA, 9, "[Stamp placeholder]");
        y -= 25;
        textLine(cs, m, y, PDType1Font.HELVETICA, 9, "This document is a system-generated proof of residence.");
        y -= 12;
        textLine(cs, m, y, PDType1Font.HELVETICA, 9, "It confirms association with the above PTO at the time of printing.");
        y -= 25;
        textLine(cs, m, y, PDType1Font.HELVETICA_BOLD, 9, "Generated electronically - no wet signature required.");
    }

    private void drawLine(PDPageContentStream cs, float x, float y, float w) throws Exception {
        cs.moveTo(x, y);
        cs.lineTo(x + w, y);
        cs.stroke();
    }

    private void textLine(PDPageContentStream cs, float x, float y, PDType1Font font, float size, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void drawLabel(PDPageContentStream cs, float x, float y, String label, String value) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(label + " ");
        cs.setFont(PDType1Font.HELVETICA, 11);
        cs.showText(value == null ? "-" : value);
        cs.endText();
    }
}
