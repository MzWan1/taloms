package za.co.taloms.household.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

@Getter
public class HouseholdCreatedEvent extends ApplicationEvent {

    private final Long householdId;
    private final String householdHeadName;
    private final Long parcelId;
    private final Long ptoId;
    private final String createdBy;
    private final LocalDateTime createdAt;

    public HouseholdCreatedEvent(Object source,
                                 Long householdId,
                                 String householdHeadName,
                                 Long parcelId,
                                 Long ptoId,
                                 String createdBy,
                                 LocalDateTime createdAt) {
        super(source);
        this.householdId = householdId;
        this.householdHeadName = householdHeadName;
        this.parcelId = parcelId;
        this.ptoId = ptoId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
