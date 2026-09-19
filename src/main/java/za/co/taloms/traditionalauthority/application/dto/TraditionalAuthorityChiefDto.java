package za.co.taloms.traditionalauthority.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import za.co.taloms.common.MaskedLongSerializer;

/**
 * A chief (ROLE_CHIEF user) linked to a Traditional Authority through the
 * many-to-many chief ↔ authority relationship.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TraditionalAuthorityChiefDto {
    @JsonSerialize(using = MaskedLongSerializer.class)
    private Long   id;
    private String fullName;
    private String username;
    private String email;
}
