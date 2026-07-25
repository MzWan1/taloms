package za.co.taloms.dashboard.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingPtoSummaryDto {
    private Long id;
    private String ptoNumber;
    private String holderName;
    private String idNumber;
    private String villageName;
    private String authorityName;
    private LocalDate issueDate;
}
