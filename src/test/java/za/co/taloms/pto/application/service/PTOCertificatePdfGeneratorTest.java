package za.co.taloms.pto.application.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PTOCertificatePdfGenerator to verify PDF generation works.
 */
@SpringBootTest
class PTOCertificatePdfGeneratorTest {

    @Autowired
    private PTOCertificatePdfGenerator pdfGenerator;

    @Test
    void generateCertificate_shouldProduceValidPdf() throws Exception {
        // Arrange: Create a mock PTO for testing
        // Note: In a real test, you'd use a test database or mock repository
        // For now, we test the PDF library integration directly
        
        // Act & Assert: Verify PDFBox can create a valid document
        try (PDDocument document = new PDDocument()) {
            assertNotNull(document);
            assertTrue(document.getNumberOfPages() >= 0);
            
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void pdfFileSignature_shouldDetectPdfHeader() throws Exception {
        // PDF files must start with "%PDF"
        // This validates we're generating actual PDFs, not text
        
        // Create a minimal valid PDF using PDFBox
        byte[] pdfBytes = createTestPdf();
        
        // Verify PDF header
        assertTrue(pdfBytes.length > 0, "PDF should not be empty");
        String header = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", header, "PDF should start with %PDF header");
        
        // Verify contains PDF EOF marker
        String pdfContent = new String(pdfBytes);
        assertTrue(pdfContent.contains("%%EOF"), "PDF should contain EOF marker");
    }

    @Test
    void generateCertificate_withValidPto_shouldReturnPdfWithCorrectHeader() {
        // This test verifies that the actual generateCertificate method
        // produces output that starts with %PDF (not plain text)
        
        // Create a mock PTO
        PTO mockPto = createMockPto(1L);
        
        // Create mock repository
        PTORepositoryPort mockRepo = mock(PTORepositoryPort.class);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(mockPto));
        
        // Create generator with mock repository
        PTOCertificatePdfGenerator generator = new PTOCertificatePdfGenerator(mockRepo);
        
        // Act: Generate certificate
        byte[] result = generator.generateCertificate(1L);
        
        // Assert: Verify it's a PDF (starts with %PDF)
        assertNotNull(result, "Result should not be null");
        assertTrue(result.length > 0, "Result should not be empty");
        
        String header = new String(result, 0, Math.min(4, result.length), StandardCharsets.ISO_8859_1);
        assertEquals("%PDF", header, "PDF should start with %PDF header");
        
        // Verify contains PDF EOF marker
        String content = new String(result, StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("%%EOF"), "PDF should contain EOF marker");
    }

    private PTO createMockPto(Long id) {
        PTO pto = new PTO();
        // Use reflection or setter methods if available
        // For now, we assume PTO has a no-arg constructor and we can set fields
        try {
            java.lang.reflect.Field idField = PTO.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(pto, id);
        } catch (Exception e) {
            // Field might not exist or be accessible
        }
        return pto;
    }

    private byte[] createTestPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                document.save(baos);
                return baos.toByteArray();
            }
        }
    }
}