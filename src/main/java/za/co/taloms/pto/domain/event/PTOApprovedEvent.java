package za.co.taloms.pto.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

@Getter
public class PTOApprovedEvent extends ApplicationEvent {

    private final Long          ptoId;
    private final String        ptoNumber;
    private final String        ptoHolderName;
    private final String        approvedBy;
    private final LocalDateTime approvedAt;

    public PTOApprovedEvent(Object        source,
                            Long          ptoId,
                            String        ptoNumber,
                            String        ptoHolderName,
                            String        approvedBy,
                            LocalDateTime approvedAt) {
        super(source);
        this.ptoId         = ptoId;
        this.ptoNumber     = ptoNumber;
        this.ptoHolderName = ptoHolderName;
        this.approvedBy    = approvedBy;
        this.approvedAt    = approvedAt;
    }
}