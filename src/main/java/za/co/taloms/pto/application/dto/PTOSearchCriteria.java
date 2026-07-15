package za.co.taloms.pto.application.dto;

import lombok.*;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PTOSearchCriteria {
    private String     holderName;
    private String     idNumber;
    private String     ptoNumber;
    private PTOStatus  status;
    private PTOPurpose purpose;
    private Long       villageId;
    private Long       authorityId;
}