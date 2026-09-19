package za.co.taloms.pto.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class PTONumberGeneratorImpl implements PTONumberGenerator {

    private final PTORepositoryPort ptoRepository;
    private final AtomicInteger     counter = new AtomicInteger(0);

    /**
     * Synchronised: two concurrent callers could otherwise read the same
     * countAll() baseline and mint the SAME number, which then collides on
     * pto_records.pto_number's UNIQUE constraint and fails one insert with a
     * confusing database error instead of a clean business retry.
     */
    @Override
    public synchronized String generate() {
        String year   = String.valueOf(Year.now().getValue());
        long   total  = ptoRepository.countAll();
        int    next   = (int) total + counter.incrementAndGet();
        return String.format("PTO-%s-%05d", year, next);
    }
}


