package za.co.taloms.pto.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PTOCertificatePdfGenerator {

    private final PTORepositoryPort ptoRepository;

    public byte[] generateCertificate(Long ptoId) {
        PTO pto = ptoRepository.findById(ptoId)
                .orElseThrow(() -> new ResourceNotFoundException("PTO", ptoId));

        // For now, return a simple text-based certificate
        // In a real implementation, you would use a PDF library like iText or Apache PDFBuilder
        String certificate = generateSimpleCertificate(pto);
        return certificate.getBytes();
    }

    private String generateSimpleCertificate(PTO pto) {
        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n");
        sb.append("       TRADITIONAL AUTHORITY LAND\n");
        sb.append("       OCCUPANCY MANAGEMENT SYSTEM\n");
        sb.append("=====================================\n\n");
        sb.append("         PTO CERTIFICATE\n\n");
        sb.append("PTO Number: ").append(pto.getPtoNumber()).append("\n");
        sb.append("Holder Name: ").append(pto.getPtoHolderName()).append("\n");
        sb.append("ID Number: ").append(pto.getIdNumber()).append("\n");
        sb.append("Purpose: ").append(pto.getPurpose().getDisplayName()).append("\n");
        sb.append("Status: ").append(pto.getStatus().getDisplayName()).append("\n");
        sb.append("Issue Date: ").append(pto.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
        if (pto.getExpiryDate() != null) {
            sb.append("Expiry Date: ").append(pto.getExpiryDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
        }
        if (pto.getVillage() != null) {
            sb.append("Village: ").append(pto.getVillage().getVillageName()).append("\n");
        }
        if (pto.getTraditionalAuthority() != null) {
            sb.append("Authority: ").append(pto.getTraditionalAuthority().getAuthorityName()).append("\n");
        }
        sb.append("\n=====================================\n");
        sb.append("This certificate is issued by TALOMS\n");
        sb.append("=====================================\n");
        return sb.toString();
    }
}