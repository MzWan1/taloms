package za.co.taloms.pto.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import za.co.taloms.pto.domain.entity.PTO;
import java.time.LocalDateTime;

@Getter
public class PTOReinstatedEvent extends ApplicationEvent {

    private final Long ptoId;
    private final String ptoNumber;
    private final String ptoHolderName;
    private final String reinstatedBy;
    private final String reason;
    private final LocalDateTime reinstatedAt;

    public PTOReinstatedEvent(PTO pto) {
        super(pto);
        this.ptoId = pto.getId();
        this.ptoNumber = pto.getPtoNumber();
        this.ptoHolderName = pto.getPtoHolderName();
        this.reinstatedBy = pto.getReinstatedBy();
        this.reason = pto.getReinstateReason();
        this.reinstatedAt = pto.getReinstatedAt();
    }
}