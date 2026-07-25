package za.co.taloms.pto.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

@Getter
public class PTOExpiredEvent extends ApplicationEvent {

    private final Long ptoId;
    private final String ptoNumber;
    private final String ptoHolderName;
    private final LocalDateTime expiredAt;

    public PTOExpiredEvent(Object source, Long ptoId, String ptoNumber, String ptoHolderName, LocalDateTime expiredAt) {
        super(source);
        this.ptoId = ptoId;
        this.ptoNumber = ptoNumber;
        this.ptoHolderName = ptoHolderName;
        this.expiredAt = expiredAt;
    }
}
