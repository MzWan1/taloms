package za.co.taloms.dashboard.application.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.taloms.common.MaskedIdNumberSerializer;
import za.co.taloms.common.MaskedLongSerializer;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingPtoSummaryDto {
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long id;
    private String ptoNumber;
    private String holderName;
    @JsonSerialize(using = MaskedIdNumberSerializer.class)
    private String idNumber;
    private String villageName;
    private String authorityName;
    private LocalDate issueDate;
}


