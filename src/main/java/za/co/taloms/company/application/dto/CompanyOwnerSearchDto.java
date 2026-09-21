package za.co.taloms.company.application.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CompanyOwnerSearchDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String maskedIdNumber;
    private String roleName;
    private boolean alreadyAssigned;
}
