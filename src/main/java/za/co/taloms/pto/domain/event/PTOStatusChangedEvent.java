package za.co.taloms.pto.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.time.LocalDateTime;

@Getter
public class PTOStatusChangedEvent extends ApplicationEvent {

    private final Long          ptoId;
    private final String        ptoNumber;
    private final PTOStatus     previousStatus;
    private final PTOStatus     newStatus;
    private final String        changedBy;
    private final LocalDateTime changedAt;

    public PTOStatusChangedEvent(Object      source,
                                 Long        ptoId,
                                 String      ptoNumber,
                                 PTOStatus   previousStatus,
                                 PTOStatus   newStatus,
                                 String      changedBy,
                                 LocalDateTime changedAt) {
        super(source);
        this.ptoId          = ptoId;
        this.ptoNumber      = ptoNumber;
        this.previousStatus = previousStatus;
        this.newStatus      = newStatus;
        this.changedBy      = changedBy;
        this.changedAt      = changedAt;
    }
}