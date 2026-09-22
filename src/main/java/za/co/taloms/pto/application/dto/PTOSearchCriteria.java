package za.co.taloms.pto.application.dto;

import lombok.*;
import za.co.taloms.pto.domain.entity.PTOPurpose;
import za.co.taloms.pto.domain.entity.PTOStatus;
import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PTOSearchCriteria {
    private String     holderName;
    private String     idNumber;
    private String     ptoNumber;
    private PTOStatus  status;
    private PTOPurpose purpose;
    private Set<Long> villageIds;
    private Long       authorityId;
    private Integer    page;
    private Integer    size;
}

