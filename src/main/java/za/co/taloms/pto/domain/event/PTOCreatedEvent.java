package za.co.taloms.pto.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PTOCreatedEvent extends ApplicationEvent {

    private final Long   ptoId;
    private final String ptoNumber;
    private final String ptoHolderName;
    private final Long   villageId;
    private final Long   traditionalAuthorityId;
    private final String createdBy;

    public PTOCreatedEvent(Object source,
                           Long   ptoId,
                           String ptoNumber,
                           String ptoHolderName,
                           Long   villageId,
                           Long   traditionalAuthorityId,
                           String createdBy) {
        super(source);
        this.ptoId                 = ptoId;
        this.ptoNumber             = ptoNumber;
        this.ptoHolderName         = ptoHolderName;
        this.villageId             = villageId;
        this.traditionalAuthorityId = traditionalAuthorityId;
        this.createdBy             = createdBy;
    }
}