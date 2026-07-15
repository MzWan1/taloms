package za.co.taloms.pto.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

@Getter
public class PTORevokedEvent extends ApplicationEvent {

    private final Long          ptoId;
    private final String        ptoNumber;
    private final String        ptoHolderName;
    private final String        revokedBy;
    private final String        reason;
    private final LocalDateTime revokedAt;

    public PTORevokedEvent(Object        source,
                           Long          ptoId,
                           String        ptoNumber,
                           String        ptoHolderName,
                           String        revokedBy,
                           String        reason,
                           LocalDateTime revokedAt) {
        super(source);
        this.ptoId         = ptoId;
        this.ptoNumber     = ptoNumber;
        this.ptoHolderName = ptoHolderName;
        this.revokedBy     = revokedBy;
        this.reason        = reason;
        this.revokedAt     = revokedAt;
    }
}