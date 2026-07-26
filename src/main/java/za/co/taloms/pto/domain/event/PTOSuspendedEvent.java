package za.co.taloms.pto.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

@Getter
public class PTOSuspendedEvent extends ApplicationEvent {

    private final Long ptoId;
    private final String ptoNumber;
    private final String ptoHolderName;
    private final String suspendedBy;
    private final String reason;
    private final LocalDateTime suspendedAt;

    public PTOSuspendedEvent(Object source, Long ptoId, String ptoNumber, String ptoHolderName,
                             String suspendedBy, String reason, LocalDateTime suspendedAt) {
        super(source);
        this.ptoId = ptoId;
        this.ptoNumber = ptoNumber;
        this.ptoHolderName = ptoHolderName;
        this.suspendedBy = suspendedBy;
        this.reason = reason;
        this.suspendedAt = suspendedAt;
    }
}


