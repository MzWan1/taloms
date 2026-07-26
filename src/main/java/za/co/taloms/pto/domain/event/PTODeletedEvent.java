package za.co.taloms.pto.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

@Getter
public class PTODeletedEvent extends ApplicationEvent {

    private final Long ptoId;
    private final String ptoNumber;
    private final String ptoHolderName;
    private final String deletedBy;
    private final LocalDateTime deletedAt;
    private final String reason;

    public PTODeletedEvent(Object source, Long ptoId, String ptoNumber, String ptoHolderName,
                           String deletedBy, LocalDateTime deletedAt, String reason) {
        super(source);
        this.ptoId = ptoId;
        this.ptoNumber = ptoNumber;
        this.ptoHolderName = ptoHolderName;
        this.deletedBy = deletedBy;
        this.deletedAt = deletedAt;
        this.reason = reason;
    }
}


