package za.co.taloms.businessoccupancy.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

@Getter
public class BusinessOccupancyCreatedEvent extends ApplicationEvent {

    private final Long businessId;
    private final String businessName;
    private final String ownerName;
    private final Long parcelId;
    private final Long ptoId;
    private final String createdBy;
    private final LocalDateTime createdAt;

    public BusinessOccupancyCreatedEvent(Object source,
                                         Long businessId,
                                         String businessName,
                                         String ownerName,
                                         Long parcelId,
                                         Long ptoId,
                                         String createdBy,
                                         LocalDateTime createdAt) {
        super(source);
        this.businessId = businessId;
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.parcelId = parcelId;
        this.ptoId = ptoId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
