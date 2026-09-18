package za.co.taloms.useraccess.application.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.*;
import za.co.taloms.common.MaskedIdNumberSerializer;
import za.co.taloms.common.MaskedLongSerializer;

/** Lightweight view of a ROLE_USER account used for adding/removing access. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserAccessDto {
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long   id;
    private String fullName;
    private String email;
    @JsonSerialize(using = MaskedIdNumberSerializer.class)
    private String idNumber;
}
