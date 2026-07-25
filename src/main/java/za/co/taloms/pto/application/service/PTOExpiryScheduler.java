package za.co.taloms.pto.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.pto.domain.entity.PTO;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.pto.domain.event.PTOExpiredEvent;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.reporting.application.service.PTOCertificatePdfGenerator;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PTOExpiryScheduler {

    private final PTORepositoryPort ptoRepository;
    private final PTOCertificatePdfGenerator ptoCertificatePdfGenerator;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireOverduePTOs() {
        // PTOs with a status of ACTIVE are the only ones that can expire
        // PENDING, SUSPENDED, REVOKED, and EXPIRED are not affected by this job
        java.util.List<PTO> activePTOs = ptoRepository.findByStatus(PTOStatus.ACTIVE);
        int expiredCount = 0;

        for (PTO pto : activePTOs) {
            // Only expire PTOs that have an expiry date in the past
            if (pto.getExpiryDate() != null && pto.getExpiryDate().isBefore(LocalDate.now())) {
                pto.setStatus(PTOStatus.EXPIRED);
                ptoRepository.save(pto);

                eventPublisher.publishEvent(new PTOExpiredEvent(
                        this, pto.getId(), pto.getPtoNumber(),
                        pto.getPtoHolderName(), java.time.LocalDateTime.now()));

                expiredCount++;
                log.info("PTO {} expired on {}", pto.getPtoNumber(), pto.getExpiryDate());
            }
        }

        if (expiredCount > 0) {
            log.info("PTO expiry job completed. {} PTO(s) marked as EXPIRED", expiredCount);
        }
    }
}
